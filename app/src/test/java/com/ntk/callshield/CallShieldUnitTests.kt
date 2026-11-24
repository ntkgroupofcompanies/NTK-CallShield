package com.ntk.callshield

import com.ntk.callshield.domain.BlockAction
import com.ntk.callshield.domain.CallDisposition
import com.ntk.callshield.domain.CallScreeningDecisionEngine
import com.ntk.callshield.domain.CarrierVerificationStatus
import com.ntk.callshield.domain.LineType
import com.ntk.callshield.domain.OfflineDeltaUpdater
import com.ntk.callshield.domain.ProtectionPreset
import com.ntk.callshield.domain.RuleType
import com.ntk.callshield.domain.ScamUrlAnalyzer
import com.ntk.callshield.domain.ScreeningCallResponseFlags
import com.ntk.callshield.domain.SmsCategory
import com.ntk.callshield.domain.SpamBloomFilter
import com.ntk.callshield.domain.ThreatCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallShieldUnitTests {

    private val engine = CallScreeningDecisionEngine()

    @Test
    fun `local contact and safe list override spam intel even in Strict mode`() {
        val spamRecord = CallScreeningDecisionEngine.LocalIntelRecord(
            normalizedNumber = "+18885550194",
            callerName = "Auto-Warranty Scam Bot",
            riskScore = 98,
            category = ThreatCategory.ROBOCALL,
            lineType = LineType.VOIP,
            carrier = "Onvoy VoIP",
            isVerifiedBusiness = false,
            isBlacklisted = true
        )

        val whitelisted = engine.evaluate(
            CallScreeningDecisionEngine.ScreeningInput(
                rawHandle = "+1 (888) 555-0194",
                verificationStatus = CarrierVerificationStatus.SPOOF_WARNING,
                isWhitelisted = true,
                isLocalContact = false,
                intelRecord = spamRecord,
                bloomFilterHit = true,
                config = CallScreeningDecisionEngine.ScreeningConfig(protectionPreset = ProtectionPreset.STRICT)
            )
        )
        assertEquals(CallDisposition.ALLOWED, whitelisted.disposition)
        assertFalse(whitelisted.responseFlags.disallowCall)
        assertEquals(0, whitelisted.riskScore)
        assertTrue(whitelisted.latencyMs < 500L)

        val savedContact = engine.evaluate(
            CallScreeningDecisionEngine.ScreeningInput(
                rawHandle = "+14155550201",
                verificationStatus = CarrierVerificationStatus.SPOOF_WARNING,
                isWhitelisted = false,
                isLocalContact = true,
                localContactName = "Elena Vance",
                intelRecord = null,
                config = CallScreeningDecisionEngine.ScreeningConfig(protectionPreset = ProtectionPreset.STRICT)
            )
        )
        assertEquals(CallDisposition.ALLOWED, savedContact.disposition)
        assertEquals("Elena Vance", savedContact.displayName)
        assertFalse(savedContact.responseFlags.disallowCall)
    }

    @Test
    fun `protection presets handle spoofed caller and Gentle never auto-blocks`() {
        val gentle = engine.evaluate(
            CallScreeningDecisionEngine.ScreeningInput(
                rawHandle = "+16465550154",
                verificationStatus = CarrierVerificationStatus.SPOOF_WARNING,
                isWhitelisted = false,
                isLocalContact = false,
                intelRecord = null,
                config = CallScreeningDecisionEngine.ScreeningConfig(protectionPreset = ProtectionPreset.GENTLE)
            )
        )
        assertEquals(CallDisposition.ALLOWED, gentle.disposition)
        assertFalse(gentle.responseFlags.disallowCall)
        assertFalse(gentle.responseFlags.silenceCall)

        val balanced = engine.evaluate(
            CallScreeningDecisionEngine.ScreeningInput(
                rawHandle = "+16465550154",
                verificationStatus = CarrierVerificationStatus.SPOOF_WARNING,
                isWhitelisted = false,
                isLocalContact = false,
                intelRecord = null,
                config = CallScreeningDecisionEngine.ScreeningConfig(protectionPreset = ProtectionPreset.BALANCED)
            )
        )
        assertEquals(CallDisposition.SILENCED, balanced.disposition)
        assertFalse(balanced.responseFlags.disallowCall)
        assertTrue(balanced.responseFlags.silenceCall)

        val strict = engine.evaluate(
            CallScreeningDecisionEngine.ScreeningInput(
                rawHandle = "+16465550154",
                verificationStatus = CarrierVerificationStatus.SPOOF_WARNING,
                isWhitelisted = false,
                isLocalContact = false,
                intelRecord = null,
                config = CallScreeningDecisionEngine.ScreeningConfig(
                    protectionPreset = ProtectionPreset.STRICT,
                    defaultBlockAction = BlockAction.BLOCK_SILENTLY
                )
            )
        )
        assertEquals(CallDisposition.BLOCKED, strict.disposition)
        assertTrue(strict.responseFlags.disallowCall)
        assertTrue(strict.responseFlags.rejectCall)
        assertFalse(strict.responseFlags.silenceCall)
    }

    @Test
    fun `master protection toggle OFF allows call immediately`() {
        val decision = engine.evaluate(
            CallScreeningDecisionEngine.ScreeningInput(
                rawHandle = "+18885550194",
                verificationStatus = CarrierVerificationStatus.SPOOF_WARNING,
                isWhitelisted = false,
                isLocalContact = false,
                intelRecord = null,
                bloomFilterHit = true,
                config = CallScreeningDecisionEngine.ScreeningConfig(masterProtectionEnabled = false)
            )
        )
        assertEquals(CallDisposition.ALLOWED, decision.disposition)
        assertFalse(decision.responseFlags.disallowCall)
    }

    @Test
    fun `country code and wildcard prefix rules match +224, +232, +375, and +234-700 wildcard`() {
        val rules = listOf(
            CallScreeningDecisionEngine.RuleSpec(1, "Guinea", RuleType.COUNTRY_CODE, "+224", BlockAction.BLOCK_SILENTLY, true),
            CallScreeningDecisionEngine.RuleSpec(2, "Sierra Leone", RuleType.COUNTRY_CODE, "+232", BlockAction.BLOCK_SILENTLY, true),
            CallScreeningDecisionEngine.RuleSpec(3, "Belarus", RuleType.COUNTRY_CODE, "+375", BlockAction.BLOCK_SILENTLY, true),
            CallScreeningDecisionEngine.RuleSpec(4, "Fake NG Toll", RuleType.PREFIX_PATTERN, "+234-700-*", BlockAction.BLOCK_SHOW_MISSED, true)
        )

        val guinea = engine.evaluate(
            CallScreeningDecisionEngine.ScreeningInput("+224620550194", CarrierVerificationStatus.UNVERIFIED, false, false, null, null, false, rules)
        )
        assertEquals(CallDisposition.BLOCKED, guinea.disposition)

        val sierra = engine.evaluate(
            CallScreeningDecisionEngine.ScreeningInput("+23276550118", CarrierVerificationStatus.UNVERIFIED, false, false, null, null, false, rules)
        )
        assertEquals(CallDisposition.BLOCKED, sierra.disposition)

        val belarus = engine.evaluate(
            CallScreeningDecisionEngine.ScreeningInput("+375295501420", CarrierVerificationStatus.UNVERIFIED, false, false, null, null, false, rules)
        )
        assertEquals(CallDisposition.BLOCKED, belarus.disposition)

        val prefixMissed = engine.evaluate(
            CallScreeningDecisionEngine.ScreeningInput("+234 700 555 0199", CarrierVerificationStatus.UNVERIFIED, false, false, null, null, false, rules)
        )
        assertEquals(CallDisposition.MISSED, prefixMissed.disposition)
        assertTrue(prefixMissed.responseFlags.disallowCall)
        assertTrue(prefixMissed.responseFlags.rejectCall)
        assertFalse(prefixMissed.responseFlags.skipCallLog)
    }

    @Test
    fun `repeat caller bypass allows 2nd call within window and blocks after window expires`() {
        val now = 1_700_000_000_000L
        val config = CallScreeningDecisionEngine.ScreeningConfig(
            protectionPreset = ProtectionPreset.STRICT,
            repeatedCallerBypassEnabled = true,
            repeatedCallerWindowMinutes = 5
        )

        val secondCallInWindow = engine.evaluate(
            CallScreeningDecisionEngine.ScreeningInput(
                rawHandle = "+13105550168",
                verificationStatus = CarrierVerificationStatus.UNVERIFIED,
                isWhitelisted = false,
                isLocalContact = false,
                intelRecord = null,
                recentBlockedOrSilencedTimestampsMs = listOf(now - 2 * 60_000L),
                nowMillis = now,
                config = config
            )
        )
        assertEquals(CallDisposition.BYPASSED_REPEAT, secondCallInWindow.disposition)
        assertFalse(secondCallInWindow.responseFlags.disallowCall)

        val expired = engine.evaluate(
            CallScreeningDecisionEngine.ScreeningInput(
                rawHandle = "+13105550168",
                verificationStatus = CarrierVerificationStatus.SPOOF_WARNING,
                isWhitelisted = false,
                isLocalContact = false,
                intelRecord = null,
                recentBlockedOrSilencedTimestampsMs = listOf(now - 20 * 60_000L),
                nowMillis = now,
                config = config
            )
        )
        assertEquals(CallDisposition.BLOCKED, expired.disposition)
    }

    @Test
    fun `CallResponse flags enforce strict Android OS invariants across all block actions`() {
        for (action in BlockAction.entries) {
            val flags = CallScreeningDecisionEngine.flagsForAction(action)
            if (flags.disallowCall) {
                assertFalse(flags.silenceCall)
            } else {
                assertFalse(flags.rejectCall)
                assertFalse(flags.skipCallLog)
                assertFalse(flags.skipNotification)
            }
        }

        var invalidRejectThrew = false
        try {
            ScreeningCallResponseFlags(disallowCall = false, rejectCall = true, silenceCall = false, skipCallLog = false, skipNotification = false)
        } catch (_: IllegalArgumentException) {
            invalidRejectThrew = true
        }
        assertTrue(invalidRejectThrew)

        var invalidSilenceThrew = false
        try {
            ScreeningCallResponseFlags(disallowCall = true, rejectCall = true, silenceCall = true, skipCallLog = false, skipNotification = false)
        } catch (_: IllegalArgumentException) {
            invalidSilenceThrew = true
        }
        assertTrue(invalidSilenceThrew)
    }

    @Test
    fun `bloom filter accurately tracks spam numbers`() {
        val bloom = SpamBloomFilter()
        bloom.rebuild(listOf("+18885550194", "+224620550194", "+375295501420"))
        assertTrue(bloom.mightContain("+18885550194"))
        assertTrue(bloom.mightContain("+224620550194"))
        assertFalse(bloom.mightContain("+14155550201"))
    }

    @Test
    fun `ScamUrlAnalyzer scores official domains safe and detects spoofed, shortened, and IP phishing links`() {
        val safe = ScamUrlAnalyzer.analyze("Check your statement at https://www.chase.com/personal/banking")
        assertTrue("Expected safe score < 15, got ${safe.riskScore}", safe.riskScore < 15)

        val spoofed = ScamUrlAnalyzer.analyze(
            "FINAL NOTICE: Your account suspended due to unpaid toll. Verify immediately within 24 hours at http://usps-toll-pay.xyz/verify"
        )
        assertTrue("Expected high score >= 70, got ${spoofed.riskScore}", spoofed.riskScore >= 70)
        assertTrue(spoofed.extractedUrls.contains("http://usps-toll-pay.xyz/verify"))

        val shortened = ScamUrlAnalyzer.analyze("PayPa1 alert: verify immediately via https://bit.ly/paypa1-recover")
        assertTrue("Expected score >= 60, got ${shortened.riskScore}", shortened.riskScore >= 60)

        val ipLink = ScamUrlAnalyzer.analyze("Login at http://192.168.45.12/bank-login to verify immediately")
        assertTrue("Expected score >= 60, got ${ipLink.riskScore}", ipLink.riskScore >= 60)
    }

    @Test
    fun `Smart SMS categorizes Codes, Scams, Promos, and Personal texts`() {
        val (codeCat, codeRes) = ScamUrlAnalyzer.categorizeSms("Chase", "Your one-time verification code is 482916.")
        assertEquals(SmsCategory.CODES_ALERTS, codeCat)
        assertEquals("482916", codeRes.extractedOtp)

        val (scamCat, _) = ScamUrlAnalyzer.categorizeSms("+13055550181", "Account suspended! Verify immediately at https://bit.ly/chase-fix")
        assertEquals(SmsCategory.BLOCKED_SCAMS, scamCat)

        val (promoCat, _) = ScamUrlAnalyzer.categorizeSms("Cafe", "Flash sale 25% off! Reply STOP to unsubscribe.")
        assertEquals(SmsCategory.PROMOS, promoCat)

        val (personalCat, _) = ScamUrlAnalyzer.categorizeSms("Elena", "See you for dinner at 6pm!")
        assertEquals(SmsCategory.PERSONAL, personalCat)
    }

    @Test
    fun `OfflineDeltaUpdater computes exact addition and deletion snapshot math`() {
        val initial = mapOf(
            "+2348010000099" to OfflineDeltaUpdater.SnapshotRecord(
                "+2348010000099", "Lagos Fraud Ring", 94, ThreatCategory.INTERNATIONAL_FRAUD, LineType.MOBILE, "Gateway", "West Africa"
            ),
            "+2347005550199" to OfflineDeltaUpdater.SnapshotRecord(
                "+2347005550199", "Fake BVN Desk", 96, ThreatCategory.BANK_IMPERSONATION, LineType.VOIP, "VoIP", "West Africa"
            )
        )

        val deltaPack = OfflineDeltaUpdater.buildRegionalDeltaPack("WA", "2026.11.4")
        val applied = OfflineDeltaUpdater.applyDelta(initial, deltaPack)

        // Initial had 2; WA pack adds 3 and removes 1 (+2348010000099) => 2 + 3 - 1 = 4
        assertEquals(2, applied.result.previousCount)
        assertEquals(3, applied.result.additionsCount)
        assertEquals(1, applied.result.deletionsCount)
        assertEquals(4, applied.result.newCount)
        assertEquals("2026.11.5", applied.result.newVersion)
        assertFalse(applied.records.containsKey("+2348010000099"))
        assertNotNull(applied.records["+224620991044"])
        assertNotNull(applied.records["+23276441908"])
    }
}
