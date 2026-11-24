package com.ntk.callshield.data.repository

import android.content.Context
import com.ntk.callshield.data.local.BlockingRuleEntity
import com.ntk.callshield.data.local.CallLogEntity
import com.ntk.callshield.data.local.CallShieldDatabase
import com.ntk.callshield.data.local.ContactEntity
import com.ntk.callshield.data.local.MissionProgressEntity
import com.ntk.callshield.data.local.OfflineRegionEntity
import com.ntk.callshield.data.local.PhoneIntelEntity
import com.ntk.callshield.data.local.PrivacyAuditEntity
import com.ntk.callshield.data.local.SeedData
import com.ntk.callshield.data.local.SmsMessageEntity
import com.ntk.callshield.data.prefs.CallShieldPreferences
import com.ntk.callshield.data.prefs.UserSettings
import com.ntk.callshield.data.remote.CarrierIntelligenceProvider
import com.ntk.callshield.domain.BlockAction
import com.ntk.callshield.domain.CallScreeningDecisionEngine
import com.ntk.callshield.domain.CarrierVerificationStatus
import com.ntk.callshield.domain.DeltaUpdateResult
import com.ntk.callshield.domain.LineType
import com.ntk.callshield.domain.OfflineDeltaUpdater
import com.ntk.callshield.domain.PhoneIntelligenceReport
import com.ntk.callshield.domain.PhoneNumberNormalizer
import com.ntk.callshield.domain.RuleType
import com.ntk.callshield.domain.ScamUrlAnalyzer
import com.ntk.callshield.domain.ScreeningDecision
import com.ntk.callshield.domain.SpamBloomFilter
import com.ntk.callshield.domain.ThreatCategory
import com.ntk.callshield.domain.UrlRiskAnalysis
import com.ntk.callshield.telecom.DeviceDataSyncHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface CallerIntelligenceRepository {
    val allPhoneIntel: Flow<List<PhoneIntelEntity>>
    val callLogs: Flow<List<CallLogEntity>>
    val blockingRules: Flow<List<BlockingRuleEntity>>
    val contacts: Flow<List<ContactEntity>>
    val offlineRegions: Flow<List<OfflineRegionEntity>>
    val privacyAudits: Flow<List<PrivacyAuditEntity>>
    val missions: Flow<List<MissionProgressEntity>>

    suspend fun ensureSeeded()
    suspend fun screenIncomingCall(
        rawHandle: String?,
        verificationStatus: CarrierVerificationStatus,
        simSlot: Int = 1,
        systemContactName: String? = null,
        persistLog: Boolean = true
    ): ScreeningDecision

    suspend fun reverseLookup(rawInput: String): PhoneIntelligenceReport
    suspend fun searchDirectory(query: String): List<PhoneIntelEntity>
    suspend fun findIntelSnapshot(rawNumber: String): PhoneIntelEntity?
    suspend fun restoreIntelSnapshot(rawNumber: String, previous: PhoneIntelEntity?)
    suspend fun reportNumber(
        rawNumber: String,
        callerName: String,
        category: ThreatCategory,
        blockLocally: Boolean
    ): PhoneIntelEntity?
    suspend fun setWhitelistStatus(rawNumber: String, displayName: String, whitelisted: Boolean)
    suspend fun setBlacklistStatus(rawNumber: String, displayName: String, blacklisted: Boolean)
    suspend fun addBlockingRule(label: String, ruleType: RuleType, pattern: String, action: BlockAction)
    suspend fun restoreBlockingRule(rule: BlockingRuleEntity)
    suspend fun toggleBlockingRule(rule: BlockingRuleEntity)
    suspend fun deleteBlockingRule(rule: BlockingRuleEntity)
    suspend fun deleteCallLog(id: Long)
    suspend fun restoreCallLog(log: CallLogEntity)
    suspend fun saveContact(displayName: String, rawNumber: String, roleOrLabel: String, whitelisted: Boolean)
    suspend fun toggleContactWhitelist(contact: ContactEntity)
    suspend fun findDuplicateContactGroups(): Map<String, List<ContactEntity>>
    suspend fun mergeDuplicateGroup(normalizedNumber: String): Int
    suspend fun mergeAllDuplicates(): Int
    suspend fun exportContactsToJson(): String
    suspend fun importContactsFromJson(json: String): Int
    suspend fun applyRegionalDeltaUpdate(regionId: String): DeltaUpdateResult?
    suspend fun recordPrivacyAudit(eventType: String, detail: String, leftDevice: Boolean)
    suspend fun clearPrivacyAudits()
    suspend fun incrementMissionProgress(missionId: String, step: Int = 1)
    suspend fun syncDeviceContacts(context: Context): Int
    suspend fun syncDeviceCallLogs(context: Context): Int
}

interface ThreatAnalysisRepository {
    val smsMessages: Flow<List<SmsMessageEntity>>
    suspend fun analyzeUrlOrText(input: String, recordMission: Boolean = true): UrlRiskAnalysis
    suspend fun ingestSmsMessage(sender: String, body: String): SmsMessageEntity
    suspend fun deleteSms(id: Long)
    suspend fun restoreSms(sms: SmsMessageEntity)
    suspend fun syncDeviceSmsInbox(context: Context): Int
}

class DefaultCallerIntelligenceRepository(
    private val db: CallShieldDatabase,
    private val preferences: CallShieldPreferences,
    private val remoteProvider: CarrierIntelligenceProvider,
    private val decisionEngine: CallScreeningDecisionEngine = CallScreeningDecisionEngine(),
    private val bloomFilter: SpamBloomFilter = SpamBloomFilter()
) : CallerIntelligenceRepository {

    private val seedMutex = Mutex()
    private val bloomMutex = Mutex()
    @Volatile
    private var isSeeded = false
    @Volatile
    private var bloomReady = false

    override val allPhoneIntel: Flow<List<PhoneIntelEntity>> = db.phoneIntelDao().observeAll()
    override val callLogs: Flow<List<CallLogEntity>> = db.callLogDao().observeAll()
    override val blockingRules: Flow<List<BlockingRuleEntity>> = db.blockingRuleDao().observeAll()
    override val contacts: Flow<List<ContactEntity>> = db.contactDao().observeAll()
    override val offlineRegions: Flow<List<OfflineRegionEntity>> = db.offlineRegionDao().observeAll()
    override val privacyAudits: Flow<List<PrivacyAuditEntity>> = db.privacyAuditDao().observeRecent()
    override val missions: Flow<List<MissionProgressEntity>> = db.missionDao().observeAll()

    override suspend fun ensureSeeded() {
        if (isSeeded) return
        seedMutex.withLock {
            if (isSeeded) return
            withContext(Dispatchers.IO) {
                if (!db.phoneIntelDao().hasAnyRecords()) {
                    val now = System.currentTimeMillis()
                    db.phoneIntelDao().upsertAll(SeedData.phoneIntelList(now))
                    if (db.blockingRuleDao().count() == 0) {
                        db.blockingRuleDao().insertAll(SeedData.initialBlockingRules())
                    }
                    if (db.offlineRegionDao().count() == 0) {
                        db.offlineRegionDao().upsertAll(SeedData.initialOfflineRegions(now))
                    }
                    if (db.privacyAuditDao().count() == 0) {
                        db.privacyAuditDao().insertAll(SeedData.initialPrivacyAudits(now))
                    }
                    if (db.missionDao().count() == 0) {
                        db.missionDao().upsertAll(SeedData.initialMissions())
                    }
                }
                isSeeded = true
            }
        }
    }

    private suspend fun ensureBloomReady() {
        if (bloomReady) return
        bloomMutex.withLock {
            if (!bloomReady) {
                rebuildBloomFilter()
            }
        }
    }

    private suspend fun rebuildBloomFilter() {
        val spamNumbers = db.phoneIntelDao().getHighRiskNumbers()
        bloomFilter.rebuild(spamNumbers)
        bloomReady = true
    }

    override suspend fun screenIncomingCall(
        rawHandle: String?,
        verificationStatus: CarrierVerificationStatus,
        simSlot: Int,
        systemContactName: String?,
        persistLog: Boolean
    ): ScreeningDecision = withContext(Dispatchers.IO) {
        ensureSeeded()
        val settings: UserSettings = preferences.getSnapshot()
        val isHidden = PhoneNumberNormalizer.isHiddenOrPrivate(rawHandle)
        val normalized = if (isHidden) "PRIVATE" else PhoneNumberNormalizer.normalize(rawHandle)

        val localContact = if (!isHidden) {
            db.contactDao().findByNumber(normalized)
                ?: db.contactDao().getAllSnapshot().firstOrNull { DeviceDataSyncHelper.numbersMatch(it.normalizedNumber, normalized) }
        } else null
        val intel = if (!isHidden) db.phoneIntelDao().findByNumber(normalized) else null
        val enabledRules = db.blockingRuleDao().getEnabledRules().map {
            CallScreeningDecisionEngine.RuleSpec(
                id = it.id,
                label = it.label,
                ruleType = runCatching { RuleType.valueOf(it.ruleType) }.getOrDefault(RuleType.PREFIX_PATTERN),
                pattern = it.pattern,
                action = runCatching { BlockAction.valueOf(it.action) }.getOrDefault(BlockAction.BLOCK_SILENTLY),
                enabled = it.enabled
            )
        }
        val recentBlocked = if (!isHidden) {
            db.callLogDao().getRecentBlockedOrSilenced(normalized).map { it.timestampMillis }
        } else {
            emptyList()
        }

        val localIntelSpec = intel?.let {
            CallScreeningDecisionEngine.LocalIntelRecord(
                normalizedNumber = it.normalizedNumber,
                callerName = it.callerName,
                riskScore = it.riskScore,
                category = runCatching { ThreatCategory.valueOf(it.category) }.getOrDefault(ThreatCategory.UNKNOWN),
                lineType = runCatching { LineType.valueOf(it.lineType) }.getOrDefault(LineType.UNKNOWN),
                carrier = it.carrier,
                isVerifiedBusiness = it.isVerifiedBusiness,
                isBlacklisted = it.isBlacklisted
            )
        }

        val isWhitelisted = (localContact?.isWhitelisted == true) || (intel?.isWhitelisted == true)
        val isKnownContact = (localContact != null) || !systemContactName.isNullOrBlank()
        val bloomHit = if (!isHidden) {
            ensureBloomReady()
            bloomFilter.mightContain(normalized)
        } else {
            false
        }

        val decision = decisionEngine.evaluate(
            CallScreeningDecisionEngine.ScreeningInput(
                rawHandle = rawHandle,
                verificationStatus = verificationStatus,
                isWhitelisted = isWhitelisted,
                isLocalContact = isKnownContact,
                localContactName = systemContactName ?: localContact?.displayName,
                intelRecord = localIntelSpec,
                bloomFilterHit = bloomHit,
                customRules = enabledRules,
                recentBlockedOrSilencedTimestampsMs = recentBlocked,
                config = CallScreeningDecisionEngine.ScreeningConfig(
                    masterProtectionEnabled = settings.masterProtectionEnabled,
                    protectionPreset = settings.protectionPreset,
                    zeroTelemetryMode = settings.zeroTelemetryMode,
                    blockHiddenNumbers = settings.blockHiddenNumbers,
                    blockUnverifiedVoip = settings.blockUnverifiedVoip,
                    repeatedCallerBypassEnabled = settings.repeatedCallerBypassEnabled,
                    repeatedCallerWindowMinutes = settings.repeatedCallerWindowMinutes,
                    defaultBlockAction = settings.defaultBlockAction
                )
            )
        )

        var finalDecision = decision
        if (decision.triggerAsyncEnrichment && intel == null && !isHidden) {
            val enriched = remoteProvider.enrich(normalized, settings.zeroTelemetryMode)
            if (enriched != null) {
                finalDecision = decision.copy(
                    displayName = enriched.callerName,
                    lineType = enriched.lineType,
                    carrier = enriched.carrier,
                    riskScore = enriched.riskScore,
                    category = enriched.category,
                    blockedExternalNetworkQuery = false
                )
                db.privacyAuditDao().insert(
                    PrivacyAuditEntity(
                        eventType = "SMART CLOUD CHECK",
                        detail = "Looked up ${maskNumber(normalized)} via ${remoteProvider.providerLabel}.",
                        leftDevice = true
                    )
                )
            } else {
                db.privacyAuditDao().insert(
                    PrivacyAuditEntity(
                        eventType = "ON-DEVICE CALL CHECK",
                        detail = "Private Offline Mode kept ${maskNumber(normalized)} on your phone (${decision.latencyMs}ms).",
                        leftDevice = false
                    )
                )
            }
        } else {
            db.privacyAuditDao().insert(
                PrivacyAuditEntity(
                    eventType = "ON-DEVICE CALL CHECK",
                    detail = "Checked ${maskNumber(normalized)} locally in ${decision.latencyMs}ms -> ${decision.disposition.badgeLabel}.",
                    leftDevice = false
                )
            )
        }

        if (persistLog) {
            db.callLogDao().insert(
                CallLogEntity(
                    normalizedNumber = finalDecision.normalizedNumber,
                    displayNumber = PhoneNumberNormalizer.formatForDisplay(finalDecision.normalizedNumber),
                    callerName = finalDecision.displayName,
                    disposition = finalDecision.disposition.name,
                    riskScore = finalDecision.riskScore,
                    category = finalDecision.category.name,
                    lineType = finalDecision.lineType.name,
                    carrier = finalDecision.carrier,
                    verificationStatus = finalDecision.verificationStatus.name,
                    matchedRule = finalDecision.matchedRule,
                    plainReason = finalDecision.plainReason,
                    responseFlagsSummary = finalDecision.responseFlags.summaryString(),
                    screeningLatencyMs = finalDecision.latencyMs,
                    simSlot = simSlot
                )
            )
        }

        finalDecision
    }

    override suspend fun reverseLookup(rawInput: String): PhoneIntelligenceReport = withContext(Dispatchers.IO) {
        ensureSeeded()
        val settings = preferences.getSnapshot()
        val normalized = PhoneNumberNormalizer.normalize(rawInput)
        val display = PhoneNumberNormalizer.formatForDisplay(normalized)

        val localContact = db.contactDao().findByNumber(normalized)
            ?: db.contactDao().getAllSnapshot().firstOrNull { DeviceDataSyncHelper.numbersMatch(it.normalizedNumber, normalized) }
        val entity = db.phoneIntelDao().findByNumber(normalized)

        if (entity != null) {
            db.privacyAuditDao().insert(
                PrivacyAuditEntity(
                    eventType = "NUMBER LOOKUP",
                    detail = "Checked ${maskNumber(normalized)} using your offline protection database.",
                    leftDevice = false
                )
            )
            return@withContext PhoneIntelligenceReport(
                normalizedNumber = entity.normalizedNumber,
                displayNumber = entity.displayNumber,
                callerName = localContact?.displayName ?: entity.callerName,
                riskScore = if (localContact?.isWhitelisted == true || entity.isWhitelisted) 0 else entity.riskScore,
                lineType = runCatching { LineType.valueOf(entity.lineType) }.getOrDefault(LineType.UNKNOWN),
                carrier = entity.carrier,
                region = entity.region,
                verificationStatus = runCatching { CarrierVerificationStatus.valueOf(entity.verificationDefault) }
                    .getOrDefault(CarrierVerificationStatus.UNVERIFIED),
                category = runCatching { ThreatCategory.valueOf(entity.category) }
                    .getOrDefault(ThreatCategory.UNKNOWN),
                communityReports = entity.communityReports,
                communityTags = entity.communityTagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                simSwapRisk = entity.simSwapRisk,
                callForwardingRisk = entity.callForwardingRisk,
                smsPumpingRisk = entity.smsPumpingRisk,
                isVerifiedBusiness = entity.isVerifiedBusiness,
                isFinancialInstitution = entity.isFinancialInstitution,
                isWhitelisted = entity.isWhitelisted || (localContact?.isWhitelisted == true),
                isBlacklisted = entity.isBlacklisted,
                sourceLabel = "Offline Protection Database"
            )
        }

        if (localContact != null) {
            return@withContext PhoneIntelligenceReport(
                normalizedNumber = normalized,
                displayNumber = display,
                callerName = localContact.displayName,
                riskScore = 0,
                lineType = LineType.MOBILE,
                carrier = "Your Contacts",
                region = "On Your Phone",
                verificationStatus = CarrierVerificationStatus.VERIFIED,
                category = ThreatCategory.PERSONAL,
                communityReports = 0,
                communityTags = listOf("Saved Contact", "Safe List"),
                simSwapRisk = "Low",
                callForwardingRisk = "None",
                smsPumpingRisk = "Low",
                isVerifiedBusiness = false,
                isFinancialInstitution = false,
                isWhitelisted = localContact.isWhitelisted,
                isBlacklisted = false,
                sourceLabel = "On-Device Contacts"
            )
        }

        val remote = remoteProvider.enrich(normalized, settings.zeroTelemetryMode)
        if (remote != null) {
            db.privacyAuditDao().insert(
                PrivacyAuditEntity(
                    eventType = "SMART CLOUD LOOKUP",
                    detail = "Checked ${maskNumber(normalized)} via ${remote.providerName}.",
                    leftDevice = true
                )
            )
            return@withContext PhoneIntelligenceReport(
                normalizedNumber = normalized,
                displayNumber = display,
                callerName = remote.callerName,
                riskScore = remote.riskScore,
                lineType = remote.lineType,
                carrier = remote.carrier,
                region = PhoneNumberNormalizer.extractCountryCode(normalized).ifEmpty { "Unknown" },
                verificationStatus = remote.verificationStatus,
                category = remote.category,
                communityReports = 0,
                communityTags = listOf("Smart Cloud", remote.lineType.label),
                simSwapRisk = remote.simSwapRisk,
                callForwardingRisk = remote.callForwardingRisk,
                smsPumpingRisk = remote.smsPumpingRisk,
                isVerifiedBusiness = false,
                isFinancialInstitution = false,
                isWhitelisted = false,
                isBlacklisted = false,
                sourceLabel = remote.providerName
            )
        }

        db.privacyAuditDao().insert(
            PrivacyAuditEntity(
                eventType = "OFFLINE LOOKUP",
                detail = "No local spam reports for ${maskNumber(normalized)}. Private Offline Mode blocked cloud query.",
                leftDevice = false
            )
        )
        PhoneIntelligenceReport(
            normalizedNumber = normalized,
            displayNumber = display,
            callerName = "Unlisted Caller",
            riskScore = 24,
            lineType = if (PhoneNumberNormalizer.extractUsAreaCode(normalized) in setOf("800", "888", "877", "866", "855", "844")) {
                LineType.TOLL_FREE
            } else {
                LineType.UNKNOWN
            },
            carrier = "Unknown (Works Without Internet)",
            region = PhoneNumberNormalizer.extractCountryCode(normalized).ifEmpty { "International" },
            verificationStatus = CarrierVerificationStatus.UNVERIFIED,
            category = ThreatCategory.UNKNOWN,
            communityReports = 0,
            communityTags = listOf("No Local Reports", "Private Offline"),
            simSwapRisk = "Not Checked Offline",
            callForwardingRisk = "Not Checked Offline",
            smsPumpingRisk = "Low",
            isVerifiedBusiness = false,
            isFinancialInstitution = false,
            isWhitelisted = false,
            isBlacklisted = false,
            sourceLabel = "On-Device Check (Offline Mode)"
        )
    }

    override suspend fun searchDirectory(query: String): List<PhoneIntelEntity> = withContext(Dispatchers.IO) {
        ensureSeeded()
        if (query.isBlank()) {
            db.phoneIntelDao().getAllSnapshot().take(12)
        } else {
            db.phoneIntelDao().search(query.trim())
        }
    }

    override suspend fun findIntelSnapshot(rawNumber: String): PhoneIntelEntity? = withContext(Dispatchers.IO) {
        val norm = PhoneNumberNormalizer.normalize(rawNumber)
        if (norm.isEmpty()) null else db.phoneIntelDao().findByNumber(norm)
    }

    override suspend fun restoreIntelSnapshot(rawNumber: String, previous: PhoneIntelEntity?): Unit =
        withContext(Dispatchers.IO) {
            val norm = PhoneNumberNormalizer.normalize(rawNumber)
            if (norm.isEmpty()) return@withContext
            if (previous != null) {
                db.phoneIntelDao().upsert(previous)
                val contact = db.contactDao().findByNumber(norm)
                if (contact != null) {
                    db.contactDao().update(contact.copy(isWhitelisted = previous.isWhitelisted))
                }
            } else {
                db.phoneIntelDao().deleteByNumber(norm)
            }
            rebuildBloomFilter()
        }

    override suspend fun reportNumber(
        rawNumber: String,
        callerName: String,
        category: ThreatCategory,
        blockLocally: Boolean
    ): PhoneIntelEntity? = withContext(Dispatchers.IO) {
        ensureSeeded()
        val normalized = PhoneNumberNormalizer.normalize(rawNumber)
        if (normalized.isEmpty()) return@withContext null
        val existing = db.phoneIntelDao().findByNumber(normalized)
        val isSafe = category == ThreatCategory.VERIFIED_BUSINESS || category == ThreatCategory.VERIFIED_BANK || category == ThreatCategory.PERSONAL
        val newRisk = if (isSafe) 5 else maxOf(existing?.riskScore ?: 0, 88)
        val updatedTags = buildList {
            existing?.communityTagsCsv?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.let { addAll(it) }
            add(category.label)
        }.distinct().joinToString(",")

        val updated = (existing ?: PhoneIntelEntity(
            normalizedNumber = normalized,
            displayNumber = PhoneNumberNormalizer.formatForDisplay(normalized),
            callerName = callerName.ifBlank { category.label },
            riskScore = newRisk,
            lineType = LineType.VOIP.name,
            carrier = "Community Reported",
            region = "User Reported",
            verificationDefault = if (isSafe) CarrierVerificationStatus.VERIFIED.name else CarrierVerificationStatus.UNVERIFIED.name,
            category = category.name,
            communityReports = 1,
            communityTagsCsv = updatedTags,
            simSwapRisk = if (isSafe) "Low" else "Moderate",
            callForwardingRisk = "Unknown",
            smsPumpingRisk = "Low",
            isVerifiedBusiness = isSafe,
            isFinancialInstitution = category == ThreatCategory.VERIFIED_BANK
        )).copy(
            callerName = callerName.ifBlank { existing?.callerName ?: category.label },
            riskScore = newRisk,
            category = category.name,
            communityReports = (existing?.communityReports ?: 0) + 1,
            communityTagsCsv = updatedTags,
            isVerifiedBusiness = isSafe,
            isBlacklisted = if (isSafe) false else blockLocally,
            isWhitelisted = if (isSafe) true else false,
            updatedAtMillis = System.currentTimeMillis()
        )
        db.phoneIntelDao().upsert(updated)
        rebuildBloomFilter()
        incrementMissionProgress("mission_report_spam")
        existing
    }

    override suspend fun setWhitelistStatus(rawNumber: String, displayName: String, whitelisted: Boolean): Unit =
        withContext(Dispatchers.IO) {
            ensureSeeded()
            val normalized = PhoneNumberNormalizer.normalize(rawNumber)
            if (normalized.isEmpty()) return@withContext
            val existing = db.phoneIntelDao().findByNumber(normalized)
            if (existing != null) {
                db.phoneIntelDao().upsert(
                    existing.copy(
                        isWhitelisted = whitelisted,
                        isBlacklisted = if (whitelisted) false else existing.isBlacklisted,
                        riskScore = if (whitelisted) 0 else existing.riskScore
                    )
                )
            } else {
                db.phoneIntelDao().upsert(
                    PhoneIntelEntity(
                        normalizedNumber = normalized,
                        displayNumber = PhoneNumberNormalizer.formatForDisplay(normalized),
                        callerName = displayName.ifBlank { "Trusted Caller" },
                        riskScore = 0,
                        lineType = LineType.MOBILE.name,
                        carrier = "On-Device Safe List",
                        region = "On Your Phone",
                        verificationDefault = CarrierVerificationStatus.VERIFIED.name,
                        category = ThreatCategory.PERSONAL.name,
                        communityReports = 0,
                        communityTagsCsv = "Safe List",
                        simSwapRisk = "Low",
                        callForwardingRisk = "None",
                        smsPumpingRisk = "Low",
                        isVerifiedBusiness = false,
                        isWhitelisted = whitelisted,
                        isBlacklisted = false
                    )
                )
            }
            val contact = db.contactDao().findByNumber(normalized)
            if (contact != null) {
                db.contactDao().update(contact.copy(isWhitelisted = whitelisted))
            }
            rebuildBloomFilter()
        }

    override suspend fun setBlacklistStatus(rawNumber: String, displayName: String, blacklisted: Boolean): Unit =
        withContext(Dispatchers.IO) {
            ensureSeeded()
            val normalized = PhoneNumberNormalizer.normalize(rawNumber)
            if (normalized.isEmpty()) return@withContext
            val existing = db.phoneIntelDao().findByNumber(normalized)
            if (existing != null) {
                db.phoneIntelDao().upsert(
                    existing.copy(
                        isBlacklisted = blacklisted,
                        isWhitelisted = if (blacklisted) false else existing.isWhitelisted,
                        riskScore = if (blacklisted) maxOf(existing.riskScore, 95) else existing.riskScore
                    )
                )
            } else {
                db.phoneIntelDao().upsert(
                    PhoneIntelEntity(
                        normalizedNumber = normalized,
                        displayNumber = PhoneNumberNormalizer.formatForDisplay(normalized),
                        callerName = displayName.ifBlank { "Blocked Number" },
                        riskScore = if (blacklisted) 96 else 20,
                        lineType = LineType.UNKNOWN.name,
                        carrier = "Personal Block List",
                        region = "Local",
                        verificationDefault = CarrierVerificationStatus.UNVERIFIED.name,
                        category = ThreatCategory.ROBOCALL.name,
                        communityReports = 1,
                        communityTagsCsv = "Blocked",
                        simSwapRisk = "Unknown",
                        callForwardingRisk = "Unknown",
                        smsPumpingRisk = "Low",
                        isVerifiedBusiness = false,
                        isWhitelisted = false,
                        isBlacklisted = blacklisted
                    )
                )
            }
            val contact = db.contactDao().findByNumber(normalized)
            if (contact != null && blacklisted) {
                db.contactDao().update(contact.copy(isWhitelisted = false))
            }
            rebuildBloomFilter()
        }

    override suspend fun addBlockingRule(
        label: String,
        ruleType: RuleType,
        pattern: String,
        action: BlockAction
    ): Unit = withContext(Dispatchers.IO) {
        db.blockingRuleDao().insert(
            BlockingRuleEntity(
                label = label.trim().ifBlank { "${ruleType.label} Rule" },
                ruleType = ruleType.name,
                pattern = pattern.trim(),
                action = action.name,
                enabled = true
            )
        )
    }

    override suspend fun restoreBlockingRule(rule: BlockingRuleEntity): Unit = withContext(Dispatchers.IO) {
        db.blockingRuleDao().insert(rule.copy(id = 0))
    }

    override suspend fun toggleBlockingRule(rule: BlockingRuleEntity) = withContext(Dispatchers.IO) {
        db.blockingRuleDao().update(rule.copy(enabled = !rule.enabled))
    }

    override suspend fun deleteBlockingRule(rule: BlockingRuleEntity) = withContext(Dispatchers.IO) {
        db.blockingRuleDao().delete(rule)
    }

    override suspend fun deleteCallLog(id: Long) = withContext(Dispatchers.IO) {
        db.callLogDao().deleteById(id)
    }

    override suspend fun restoreCallLog(log: CallLogEntity): Unit = withContext(Dispatchers.IO) {
        db.callLogDao().insert(log.copy(id = 0))
    }

    override suspend fun saveContact(
        displayName: String,
        rawNumber: String,
        roleOrLabel: String,
        whitelisted: Boolean
    ): Unit = withContext(Dispatchers.IO) {
        val normalized = PhoneNumberNormalizer.normalize(rawNumber)
        if (normalized.isEmpty()) return@withContext
        val palette = listOf(0xFF0EA5A4, 0xFF6366F1, 0xFF10B981, 0xFFF59E0B, 0xFFEC4899, 0xFF8B5CF6)
        val color = palette[kotlin.math.abs(displayName.hashCode()) % palette.size]
        db.contactDao().insert(
            ContactEntity(
                displayName = displayName.trim().ifBlank { normalized },
                normalizedNumber = normalized,
                displayNumber = PhoneNumberNormalizer.formatForDisplay(normalized),
                roleOrLabel = roleOrLabel.trim().ifBlank { "Saved Contact" },
                avatarColorHex = color,
                isWhitelisted = whitelisted,
                notes = "user_added"
            )
        )
    }

    override suspend fun syncDeviceContacts(context: Context): Int = withContext(Dispatchers.IO) {
        val deviceContacts = DeviceDataSyncHelper.readDeviceContacts(context)
        if (deviceContacts.isEmpty()) return@withContext 0

        val existing = db.contactDao().getAllSnapshot()
        val whitelistOverrides = existing.associate { it.normalizedNumber to it.isWhitelisted }
        val userAdded = existing.filter { it.notes == "user_added" }

        val merged = deviceContacts.map { c ->
            val override = whitelistOverrides[c.normalizedNumber]
            if (override != null) c.copy(isWhitelisted = override) else c
        } + userAdded.filter { u ->
            deviceContacts.none { DeviceDataSyncHelper.numbersMatch(it.normalizedNumber, u.normalizedNumber) }
        }

        db.contactDao().deleteAll()
        db.contactDao().insertAll(merged)
        deviceContacts.size
    }

    override suspend fun syncDeviceCallLogs(context: Context): Int = withContext(Dispatchers.IO) {
        val intel = db.phoneIntelDao().getAllSnapshot()
        val contacts = db.contactDao().getAllSnapshot()
        val deviceLogs = DeviceDataSyncHelper.readDeviceCallLogs(context, intel, contacts)
        if (deviceLogs.isEmpty()) return@withContext 0

        val legacyDemoNumbers = setOf(
            "+18885550194", "+18009359935", "+16465550154",
            "+14155550182", "+13475550176", "+224620550194"
        )
        val recentLiveScreened = db.callLogDao().getAllSnapshot().filter { existing ->
            existing.matchedRule != "Phone Call Log" &&
                (existing.normalizedNumber !in legacyDemoNumbers ||
                    (System.currentTimeMillis() - existing.timestampMillis) < 5 * 60_000L) &&
                deviceLogs.none { d ->
                    DeviceDataSyncHelper.numbersMatch(d.normalizedNumber, existing.normalizedNumber) &&
                        kotlin.math.abs(d.timestampMillis - existing.timestampMillis) < 120_000L
                }
        }

        db.callLogDao().deleteAll()
        db.callLogDao().insertAll(
            (recentLiveScreened + deviceLogs)
                .sortedByDescending { it.timestampMillis }
                .take(160)
        )
        deviceLogs.size
    }

    override suspend fun toggleContactWhitelist(contact: ContactEntity) = withContext(Dispatchers.IO) {
        db.contactDao().update(contact.copy(isWhitelisted = !contact.isWhitelisted))
    }

    override suspend fun findDuplicateContactGroups(): Map<String, List<ContactEntity>> = withContext(Dispatchers.IO) {
        db.contactDao().getAllSnapshot()
            .groupBy { it.normalizedNumber }
            .filterValues { it.size > 1 }
    }

    override suspend fun mergeDuplicateGroup(normalizedNumber: String): Int = withContext(Dispatchers.IO) {
        val group = db.contactDao().getAllSnapshot().filter { it.normalizedNumber == normalizedNumber }
        if (group.size <= 1) return@withContext 0
        val primary = group.sortedByDescending { (if (it.isFavorite) 10 else 0) + it.notes.length }.first()
        val mergedNotes = group.map { it.notes }.filter { it.isNotBlank() }.distinct().joinToString(" · ")
        val mergedFavorite = group.any { it.isFavorite }
        val mergedWhitelist = group.any { it.isWhitelisted }
        db.contactDao().update(
            primary.copy(
                notes = mergedNotes,
                isFavorite = mergedFavorite,
                isWhitelisted = mergedWhitelist
            )
        )
        val duplicateIds = group.filter { it.id != primary.id }.map { it.id }
        db.contactDao().deleteByIds(duplicateIds)
        duplicateIds.size
    }

    override suspend fun mergeAllDuplicates(): Int = withContext(Dispatchers.IO) {
        val groups = findDuplicateContactGroups()
        var merged = 0
        for (norm in groups.keys) {
            merged += mergeDuplicateGroup(norm)
        }
        merged
    }

    override suspend fun exportContactsToJson(): String = withContext(Dispatchers.IO) {
        val contacts = db.contactDao().getAllSnapshot()
        val arr = JSONArray()
        for (c in contacts) {
            arr.put(
                JSONObject().apply {
                    put("displayName", c.displayName)
                    put("normalizedNumber", c.normalizedNumber)
                    put("displayNumber", c.displayNumber)
                    put("roleOrLabel", c.roleOrLabel)
                    put("avatarColorHex", c.avatarColorHex)
                    put("isWhitelisted", c.isWhitelisted)
                    put("isFavorite", c.isFavorite)
                    put("notes", c.notes)
                }
            )
        }
        JSONObject().apply {
            put("app", "NTK CallShield")
            put("schemaVersion", 1)
            put("exportedAt", System.currentTimeMillis())
            put("contacts", arr)
        }.toString(2)
    }

    override suspend fun importContactsFromJson(json: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        val arr = root.getJSONArray("contacts")
        val existingNumbers = db.contactDao().getAllSnapshot().map { it.normalizedNumber }.toMutableSet()
        var imported = 0
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val norm = PhoneNumberNormalizer.normalize(obj.optString("normalizedNumber"))
            if (norm.isNotEmpty() && norm !in existingNumbers) {
                db.contactDao().insert(
                    ContactEntity(
                        displayName = obj.optString("displayName", norm),
                        normalizedNumber = norm,
                        displayNumber = obj.optString("displayNumber", PhoneNumberNormalizer.formatForDisplay(norm)),
                        roleOrLabel = obj.optString("roleOrLabel", "Restored Contact"),
                        avatarColorHex = obj.optLong("avatarColorHex", 0xFF0EA5A4),
                        isWhitelisted = obj.optBoolean("isWhitelisted", true),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        notes = obj.optString("notes", "")
                    )
                )
                existingNumbers.add(norm)
                imported++
            }
        }
        imported
    }

    override suspend fun applyRegionalDeltaUpdate(regionId: String): DeltaUpdateResult? = withContext(Dispatchers.IO) {
        val region = db.offlineRegionDao().getById(regionId) ?: return@withContext null
        val currentEntities = db.phoneIntelDao().getAllSnapshot()
            .filter { it.region.equals(region.regionName, ignoreCase = true) }
        val currentMap = currentEntities.associate { entity ->
            entity.normalizedNumber to OfflineDeltaUpdater.SnapshotRecord(
                normalizedNumber = entity.normalizedNumber,
                callerName = entity.callerName,
                riskScore = entity.riskScore,
                category = runCatching { ThreatCategory.valueOf(entity.category) }.getOrDefault(ThreatCategory.ROBOCALL),
                lineType = runCatching { LineType.valueOf(entity.lineType) }.getOrDefault(LineType.VOIP),
                carrier = entity.carrier,
                region = entity.region
            )
        }

        val pack = OfflineDeltaUpdater.buildRegionalDeltaPack(regionId, region.version)
        val applied = OfflineDeltaUpdater.applyDelta(currentMap, pack)

        for (added in pack.additions) {
            val norm = PhoneNumberNormalizer.normalize(added.normalizedNumber)
            db.phoneIntelDao().upsert(
                PhoneIntelEntity(
                    normalizedNumber = norm,
                    displayNumber = PhoneNumberNormalizer.formatForDisplay(norm),
                    callerName = added.callerName,
                    riskScore = added.riskScore,
                    lineType = added.lineType.name,
                    carrier = added.carrier,
                    region = added.region,
                    verificationDefault = CarrierVerificationStatus.SPOOF_WARNING.name,
                    category = added.category.name,
                    communityReports = 950,
                    communityTagsCsv = "Offline Update,${added.category.label}",
                    simSwapRisk = "Elevated",
                    callForwardingRisk = "Active Forwarding",
                    smsPumpingRisk = "Moderate",
                    isVerifiedBusiness = false
                )
            )
        }
        for (del in pack.deletions) {
            db.phoneIntelDao().deleteByNumber(PhoneNumberNormalizer.normalize(del))
        }

        val newEntryCount = (region.entryCount + applied.result.additionsCount - applied.result.deletionsCount)
            .coerceAtLeast(1)
        db.offlineRegionDao().upsert(
            region.copy(
                version = applied.result.newVersion,
                entryCount = newEntryCount,
                lastSyncedMillis = applied.result.updatedAtMillis,
                isDownloaded = true
            )
        )
        rebuildBloomFilter()
        db.privacyAuditDao().insert(
            PrivacyAuditEntity(
                eventType = "OFFLINE SHIELD UPDATE",
                detail = "${region.regionName} updated to v${applied.result.newVersion} (+${applied.result.additionsCount} added, -${applied.result.deletionsCount} removed).",
                leftDevice = false
            )
        )
        incrementMissionProgress("mission_delta_sync")
        applied.result.copy(
            previousCount = region.entryCount,
            newCount = newEntryCount
        )
    }

    override suspend fun recordPrivacyAudit(eventType: String, detail: String, leftDevice: Boolean) =
        withContext(Dispatchers.IO) {
            db.privacyAuditDao().insert(
                PrivacyAuditEntity(
                    eventType = eventType,
                    detail = detail,
                    leftDevice = leftDevice
                )
            )
        }

    override suspend fun clearPrivacyAudits() = withContext(Dispatchers.IO) {
        db.privacyAuditDao().clearAll()
    }

    override suspend fun incrementMissionProgress(missionId: String, step: Int) = withContext(Dispatchers.IO) {
        val mission = db.missionDao().getById(missionId) ?: return@withContext
        if (mission.completed) return@withContext
        val updatedProgress = (mission.currentProgress + step).coerceAtMost(mission.targetProgress)
        val nowCompleted = updatedProgress >= mission.targetProgress
        db.missionDao().upsert(
            mission.copy(
                currentProgress = updatedProgress,
                completed = nowCompleted
            )
        )
        if (nowCompleted) {
            preferences.addGuardianPoints(mission.rewardPoints)
        } else {
            preferences.addGuardianPoints(15)
        }
    }

    private fun maskNumber(normalized: String): String {
        if (normalized == "PRIVATE" || normalized.length < 6) return normalized
        return normalized.take(5) + "••••" + normalized.takeLast(2)
    }
}

class DefaultThreatAnalysisRepository(
    private val db: CallShieldDatabase,
    private val preferences: CallShieldPreferences,
    private val callerRepo: CallerIntelligenceRepository
) : ThreatAnalysisRepository {

    override val smsMessages: Flow<List<SmsMessageEntity>> = db.smsMessageDao().observeAll()

    override suspend fun analyzeUrlOrText(input: String, recordMission: Boolean): UrlRiskAnalysis =
        withContext(Dispatchers.Default) {
            val result = ScamUrlAnalyzer.analyze(input)
            if (result.riskScore >= 40) {
                preferences.incrementUrlsNeutralized()
            }
            if (recordMission) {
                callerRepo.incrementMissionProgress("mission_scan_url")
            }
            result
        }

    override suspend fun ingestSmsMessage(sender: String, body: String): SmsMessageEntity =
        withContext(Dispatchers.IO) {
            val (category, analysis) = ScamUrlAnalyzer.categorizeSms(sender, body)
            val entity = SmsMessageEntity(
                sender = sender.ifBlank { "Unknown Sender" },
                body = body.trim(),
                category = category.name,
                riskScore = analysis.riskScore,
                extractedOtp = analysis.extractedOtp,
                extractedUrl = analysis.extractedUrls.firstOrNull(),
                riskSummary = analysis.severity
            )
            val id = db.smsMessageDao().insert(entity)
            if (analysis.riskScore >= 40) {
                preferences.incrementUrlsNeutralized()
            }
            entity.copy(id = id)
        }

    override suspend fun deleteSms(id: Long) = withContext(Dispatchers.IO) {
        db.smsMessageDao().deleteById(id)
    }

    override suspend fun restoreSms(sms: SmsMessageEntity): Unit = withContext(Dispatchers.IO) {
        db.smsMessageDao().insert(sms.copy(id = 0))
    }

    override suspend fun syncDeviceSmsInbox(context: Context): Int = withContext(Dispatchers.IO) {
        val deviceSms = DeviceDataSyncHelper.readDeviceSmsInbox(context)
        if (deviceSms.isEmpty()) return@withContext 0
        db.smsMessageDao().deleteAll()
        db.smsMessageDao().insertAll(deviceSms)
        deviceSms.size
    }
}
