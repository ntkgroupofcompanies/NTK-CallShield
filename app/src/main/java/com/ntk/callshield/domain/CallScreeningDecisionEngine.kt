package com.ntk.callshield.domain

/**
 * Pure, deterministic Phase-1 call screening decision engine (<500ms SLA).
 *
 * Precedence order:
 * 1. Master Protection OFF -> Allow immediately
 * 2. Local Safe List (Whitelist) & On-Device Contacts -> Allow immediately (never erroneously blocked)
 * 3. Personal Block List -> Apply configured block action
 * 4. Carrier-Verified Bank/Business -> Allow with Verified badge
 * 5. Emergency Repeat Caller Bypass -> Allow if redialed within configured 3-15m window
 * 6. Hidden / Private Number rule (respects Gentle mode)
 * 7. Custom User Rules (Country Code, Area Code, Prefix Pattern, Private, Unverified VoIP)
 * 8. Unverified VoIP global toggle
 * 9. Carrier Spoof Warning (STIR/SHAKEN FAILED)
 * 10. Offline Spam DB + Bloom Filter thresholds per ProtectionPreset
 */
class CallScreeningDecisionEngine {

    data class RuleSpec(
        val id: Long,
        val label: String,
        val ruleType: RuleType,
        val pattern: String,
        val action: BlockAction,
        val enabled: Boolean
    )

    data class LocalIntelRecord(
        val normalizedNumber: String,
        val callerName: String,
        val riskScore: Int,
        val category: ThreatCategory,
        val lineType: LineType,
        val carrier: String,
        val isVerifiedBusiness: Boolean,
        val isBlacklisted: Boolean
    )

    data class ScreeningConfig(
        val masterProtectionEnabled: Boolean = true,
        val protectionPreset: ProtectionPreset = ProtectionPreset.BALANCED,
        val zeroTelemetryMode: Boolean = true,
        val blockHiddenNumbers: Boolean = true,
        val blockUnverifiedVoip: Boolean = false,
        val repeatedCallerBypassEnabled: Boolean = true,
        val repeatedCallerWindowMinutes: Int = 5,
        val defaultBlockAction: BlockAction = BlockAction.BLOCK_SILENTLY
    )

    data class ScreeningInput(
        val rawHandle: String?,
        val verificationStatus: CarrierVerificationStatus,
        val isWhitelisted: Boolean,
        val isLocalContact: Boolean,
        val localContactName: String? = null,
        val intelRecord: LocalIntelRecord?,
        val bloomFilterHit: Boolean = false,
        val customRules: List<RuleSpec> = emptyList(),
        val recentBlockedOrSilencedTimestampsMs: List<Long> = emptyList(),
        val nowMillis: Long = System.currentTimeMillis(),
        val config: ScreeningConfig = ScreeningConfig()
    )

    fun evaluate(input: ScreeningInput): ScreeningDecision {
        val startNanos = System.nanoTime()
        val isHidden = PhoneNumberNormalizer.isHiddenOrPrivate(input.rawHandle)
        val normalized = if (isHidden) "PRIVATE" else PhoneNumberNormalizer.normalize(input.rawHandle)

        val record = input.intelRecord
        val displayName = input.localContactName
            ?: record?.callerName
            ?: if (isHidden) "Private / Hidden Caller" else PhoneNumberNormalizer.formatForDisplay(normalized)
        val lineType = record?.lineType ?: LineType.UNKNOWN
        val carrier = record?.carrier ?: if (isHidden) "Hidden Caller ID" else "Unknown Carrier"
        val category = when {
            input.isLocalContact || input.isWhitelisted -> ThreatCategory.PERSONAL
            record != null -> record.category
            else -> ThreatCategory.UNKNOWN
        }
        val baseRiskScore = when {
            input.isLocalContact || input.isWhitelisted -> 0
            record != null -> record.riskScore
            input.verificationStatus == CarrierVerificationStatus.SPOOF_WARNING -> 78
            input.bloomFilterHit -> 86
            isHidden -> 65
            else -> 20
        }

        fun buildDecision(
            disposition: CallDisposition,
            flags: ScreeningCallResponseFlags,
            matchedRule: String,
            plainReason: String,
            riskScore: Int = baseRiskScore,
            showOverlay: Boolean = !flags.disallowCall,
            asyncEnrich: Boolean = !flags.disallowCall && !isHidden
        ): ScreeningDecision {
            val elapsedMs = ((System.nanoTime() - startNanos) / 1_000_000L).coerceAtLeast(1L)
            return ScreeningDecision(
                normalizedNumber = normalized,
                disposition = disposition,
                responseFlags = flags,
                matchedRule = matchedRule,
                plainReason = plainReason,
                riskScore = riskScore.coerceIn(0, 100),
                displayName = displayName,
                category = category,
                lineType = lineType,
                carrier = carrier,
                verificationStatus = input.verificationStatus,
                latencyMs = elapsedMs,
                showFloatingOverlay = showOverlay,
                triggerAsyncEnrichment = asyncEnrich,
                blockedExternalNetworkQuery = input.config.zeroTelemetryMode && asyncEnrich
            )
        }

        // 0. Master Protection toggle paused
        if (!input.config.masterProtectionEnabled) {
            return buildDecision(
                disposition = CallDisposition.ALLOWED,
                flags = allowFlags(silence = false),
                matchedRule = "Master Shield Paused",
                plainReason = "Protection is paused, so this call rang through normally.",
                showOverlay = false,
                asyncEnrich = false
            )
        }

        // 1. Strict On-Device Safe List & Local Contacts precedence (never erroneously blocked)
        if (input.isWhitelisted) {
            return buildDecision(
                disposition = CallDisposition.ALLOWED,
                flags = allowFlags(silence = false),
                matchedRule = "On-Device Safe List",
                plainReason = "In your Safe List — always rings through.",
                riskScore = 0,
                showOverlay = true,
                asyncEnrich = false
            )
        }
        if (input.isLocalContact) {
            return buildDecision(
                disposition = CallDisposition.ALLOWED,
                flags = allowFlags(silence = false),
                matchedRule = "Saved Contact Match",
                plainReason = "Saved in your contacts — protected from blocking.",
                riskScore = 0,
                showOverlay = true,
                asyncEnrich = false
            )
        }

        // 2. Emergency Repeat Caller Bypass check
        val windowMs = input.config.repeatedCallerWindowMinutes.coerceIn(3, 15) * 60_000L
        val hasRecentAttempt = input.config.repeatedCallerBypassEnabled &&
            !isHidden &&
            input.recentBlockedOrSilencedTimestampsMs.any { ts ->
                val delta = input.nowMillis - ts
                delta in 0..windowMs
            }

        // 3. Explicit Personal Block List
        if (record?.isBlacklisted == true) {
            val flags = flagsForAction(input.config.defaultBlockAction)
            val disp = dispositionForFlags(flags, input.config.defaultBlockAction)
            return buildDecision(
                disposition = disp,
                flags = flags,
                matchedRule = "Personal Block List",
                plainReason = "You previously blocked this number.",
                riskScore = maxOf(baseRiskScore, 95),
                showOverlay = !flags.disallowCall,
                asyncEnrich = false
            )
        }

        // 4. Verified Bank / Business with Carrier Verification
        if (record?.isVerifiedBusiness == true && input.verificationStatus == CarrierVerificationStatus.VERIFIED) {
            return buildDecision(
                disposition = CallDisposition.ALLOWED,
                flags = allowFlags(silence = false),
                matchedRule = "Carrier Verified Business",
                plainReason = "Carrier Verified ✓ trusted organization.",
                riskScore = minOf(baseRiskScore, 5),
                showOverlay = true,
                asyncEnrich = true
            )
        }

        // 5. Repeat Caller Bypass takes effect before automatic blocks
        if (hasRecentAttempt && baseRiskScore < 95) {
            return buildDecision(
                disposition = CallDisposition.BYPASSED_REPEAT,
                flags = allowFlags(silence = false),
                matchedRule = "Emergency Repeat Caller Bypass (${input.config.repeatedCallerWindowMinutes}m)",
                plainReason = "Caller tried twice within ${input.config.repeatedCallerWindowMinutes} minutes, so NTK CallShield let it ring.",
                riskScore = baseRiskScore,
                showOverlay = true,
                asyncEnrich = true
            )
        }

        val isGentle = input.config.protectionPreset == ProtectionPreset.GENTLE

        // 6. Hidden / Private / Unknown Numbers
        if (isHidden && (input.config.blockHiddenNumbers || input.config.protectionPreset == ProtectionPreset.STRICT)) {
            if (isGentle) {
                return buildDecision(
                    disposition = CallDisposition.ALLOWED,
                    flags = allowFlags(silence = false),
                    matchedRule = "Gentle Mode · Hidden Caller Warning",
                    plainReason = "Caller hid their number. Gentle mode warned instead of blocking.",
                    showOverlay = true,
                    asyncEnrich = false
                )
            }
            val flags = flagsForAction(input.config.defaultBlockAction)
            return buildDecision(
                disposition = dispositionForFlags(flags, input.config.defaultBlockAction),
                flags = flags,
                matchedRule = "Hidden / Private Number Rule",
                plainReason = "Blocked because caller hid their phone number.",
                showOverlay = !flags.disallowCall,
                asyncEnrich = false
            )
        }

        // 7. Custom User Rules (Country Code, Area Code, Prefix Pattern, Private, Unverified VoIP)
        for (rule in input.customRules) {
            if (!rule.enabled) continue
            val matched = when (rule.ruleType) {
                RuleType.COUNTRY_CODE -> {
                    val cc = PhoneNumberNormalizer.extractCountryCode(normalized)
                    val target = if (rule.pattern.startsWith("+")) rule.pattern.trim() else "+${rule.pattern.trim()}"
                    cc.isNotEmpty() && cc.equals(target, ignoreCase = true)
                }
                RuleType.AREA_CODE -> {
                    val ac = PhoneNumberNormalizer.extractUsAreaCode(normalized)
                    ac.isNotEmpty() && ac == rule.pattern.filter { it.isDigit() }
                }
                RuleType.PREFIX_PATTERN -> PhoneNumberNormalizer.matchesPattern(normalized, rule.pattern)
                RuleType.HIDDEN_PRIVATE -> isHidden
                RuleType.UNVERIFIED_VOIP -> lineType == LineType.VOIP && input.verificationStatus != CarrierVerificationStatus.VERIFIED
            }
            if (matched) {
                if (isGentle) {
                    return buildDecision(
                        disposition = CallDisposition.ALLOWED,
                        flags = allowFlags(silence = false),
                        matchedRule = "Gentle Mode · Matched '${rule.label}'",
                        plainReason = "Matched your '${rule.label}' rule. Gentle mode shows a warning only.",
                        showOverlay = true,
                        asyncEnrich = true
                    )
                }
                val flags = flagsForAction(rule.action)
                return buildDecision(
                    disposition = dispositionForFlags(flags, rule.action),
                    flags = flags,
                    matchedRule = "Custom Rule: ${rule.label} (${rule.pattern})",
                    plainReason = "Matched your custom rule '${rule.label}'.",
                    showOverlay = !flags.disallowCall,
                    asyncEnrich = !flags.disallowCall
                )
            }
        }

        // 8. Global Unverified Internet (VoIP) toggle
        if (input.config.blockUnverifiedVoip &&
            lineType == LineType.VOIP &&
            input.verificationStatus != CarrierVerificationStatus.VERIFIED &&
            !isGentle
        ) {
            val flags = flagsForAction(BlockAction.SILENCE_RINGTONE)
            return buildDecision(
                disposition = CallDisposition.SILENCED,
                flags = flags,
                matchedRule = "Unverified Internet Call Shield",
                plainReason = "Silenced unverified Internet (VoIP) call.",
                showOverlay = true,
                asyncEnrich = true
            )
        }

        // 9. Carrier Spoof Warning (STIR/SHAKEN FAILED)
        if (input.verificationStatus == CarrierVerificationStatus.SPOOF_WARNING) {
            return when (input.config.protectionPreset) {
                ProtectionPreset.STRICT -> {
                    val flags = flagsForAction(input.config.defaultBlockAction)
                    buildDecision(
                        disposition = dispositionForFlags(flags, input.config.defaultBlockAction),
                        flags = flags,
                        matchedRule = "Strict Mode · Spoof Warning Blocked",
                        plainReason = "Caller ID failed carrier verification (possible spoofed number).",
                        riskScore = maxOf(baseRiskScore, 88),
                        showOverlay = !flags.disallowCall,
                        asyncEnrich = !flags.disallowCall
                    )
                }
                ProtectionPreset.BALANCED -> {
                    buildDecision(
                        disposition = CallDisposition.SILENCED,
                        flags = allowFlags(silence = true),
                        matchedRule = "Balanced Mode · Spoof Warning Silenced",
                        plainReason = "Spoof Warning ⚠️ — silenced ringtone and flagged on screen.",
                        riskScore = maxOf(baseRiskScore, 78),
                        showOverlay = true,
                        asyncEnrich = true
                    )
                }
                ProtectionPreset.GENTLE -> {
                    buildDecision(
                        disposition = CallDisposition.ALLOWED,
                        flags = allowFlags(silence = false),
                        matchedRule = "Gentle Mode · Spoof Warning",
                        plainReason = "Spoof Warning ⚠️ — caller ID could not be verified by carrier.",
                        riskScore = maxOf(baseRiskScore, 75),
                        showOverlay = true,
                        asyncEnrich = true
                    )
                }
            }
        }

        // 10. Offline Spam Database + Bloom Filter thresholds
        val blockThreshold = when (input.config.protectionPreset) {
            ProtectionPreset.GENTLE -> Int.MAX_VALUE
            ProtectionPreset.BALANCED -> 80
            ProtectionPreset.STRICT -> 50
        }

        if (baseRiskScore >= blockThreshold || (input.bloomFilterHit && !isGentle && baseRiskScore >= 75)) {
            val flags = flagsForAction(input.config.defaultBlockAction)
            val sourceLabel = if (input.bloomFilterHit) "Offline Spam Shield + Bloom Index" else "Offline Threat Index"
            return buildDecision(
                disposition = dispositionForFlags(flags, input.config.defaultBlockAction),
                flags = flags,
                matchedRule = "$sourceLabel ($baseRiskScore/100)",
                plainReason = "Matched known scam & robocall protection list.",
                showOverlay = !flags.disallowCall,
                asyncEnrich = !flags.disallowCall
            )
        }

        val reason = when {
            baseRiskScore >= 45 -> "Unverified caller with moderate spam reports — showing Caution badge."
            record?.isVerifiedBusiness == true -> "Recognized organization."
            else -> "No spam signals found."
        }
        return buildDecision(
            disposition = CallDisposition.ALLOWED,
            flags = allowFlags(silence = false),
            matchedRule = if (baseRiskScore >= 45) "Phase-2 Caller ID Warning ($baseRiskScore/100)" else "Clean Signal · Allowed",
            plainReason = reason,
            showOverlay = true,
            asyncEnrich = true
        )
    }

    companion object {
        fun allowFlags(silence: Boolean): ScreeningCallResponseFlags =
            ScreeningCallResponseFlags(
                disallowCall = false,
                rejectCall = false,
                silenceCall = silence,
                skipCallLog = false,
                skipNotification = false
            )

        fun flagsForAction(action: BlockAction): ScreeningCallResponseFlags = when (action) {
            BlockAction.BLOCK_SILENTLY -> ScreeningCallResponseFlags(
                disallowCall = true,
                rejectCall = true,
                silenceCall = false,
                skipCallLog = true,
                skipNotification = true
            )
            BlockAction.BLOCK_SHOW_MISSED -> ScreeningCallResponseFlags(
                disallowCall = true,
                rejectCall = true,
                silenceCall = false,
                skipCallLog = false,
                skipNotification = false
            )
            BlockAction.SILENCE_RINGTONE -> ScreeningCallResponseFlags(
                disallowCall = false,
                rejectCall = false,
                silenceCall = true,
                skipCallLog = false,
                skipNotification = false
            )
            BlockAction.SEND_TO_VOICEMAIL -> ScreeningCallResponseFlags(
                disallowCall = true,
                rejectCall = false,
                silenceCall = false,
                skipCallLog = false,
                skipNotification = true
            )
        }

        private fun dispositionForFlags(flags: ScreeningCallResponseFlags, action: BlockAction): CallDisposition =
            when {
                flags.disallowCall && action == BlockAction.BLOCK_SHOW_MISSED -> CallDisposition.MISSED
                flags.disallowCall -> CallDisposition.BLOCKED
                else -> CallDisposition.SILENCED
            }
    }
}
