package com.ntk.callshield.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ntk.callshield.NtkCallShieldApplication
import com.ntk.callshield.data.local.BlockingRuleEntity
import com.ntk.callshield.data.local.CallLogEntity
import com.ntk.callshield.data.local.ContactEntity
import com.ntk.callshield.data.local.MissionProgressEntity
import com.ntk.callshield.data.local.OfflineRegionEntity
import com.ntk.callshield.data.local.PhoneIntelEntity
import com.ntk.callshield.data.local.PrivacyAuditEntity
import com.ntk.callshield.data.local.SmsMessageEntity
import com.ntk.callshield.data.prefs.UserSettings
import com.ntk.callshield.domain.BlockAction
import com.ntk.callshield.domain.CallDisposition
import com.ntk.callshield.domain.CarrierVerificationStatus
import com.ntk.callshield.domain.DaySafetyMetric
import com.ntk.callshield.domain.DeltaUpdateResult
import com.ntk.callshield.domain.GuardianBadge
import com.ntk.callshield.domain.MyCallerCard
import com.ntk.callshield.domain.OverlayStyle
import com.ntk.callshield.domain.PhoneIntelligenceReport
import com.ntk.callshield.domain.PhoneNumberNormalizer
import com.ntk.callshield.domain.ProtectionPreset
import com.ntk.callshield.domain.RuleType
import com.ntk.callshield.domain.ScreeningDecision
import com.ntk.callshield.domain.SimulationScenario
import com.ntk.callshield.domain.SimulationTelemetry
import com.ntk.callshield.domain.SmsCategory
import com.ntk.callshield.domain.ThemeMode
import com.ntk.callshield.domain.ThreatCategory
import com.ntk.callshield.domain.UrlRiskAnalysis
import com.ntk.callshield.telecom.RoleAndPermissionHelper
import com.ntk.callshield.telecom.TelecomPermissionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CallFilter(val label: String) {
    ALL("All"),
    BLOCKED("Blocked"),
    VERIFIED("Verified"),
    MISSED("Missed")
}

data class WeeklySafetySummary(
    val callsChecked: Int = 0,
    val scamsStopped: Int = 0,
    val badLinksCaught: Int = 0,
    val timeSavedMinutes: Int = 0,
    val dailyBreakdown: List<DaySafetyMetric> = emptyList()
)

data class SnackbarEvent(
    val id: Long = System.nanoTime(),
    val message: String,
    val undoLabel: String? = null,
    val onUndo: (() -> Unit)? = null
)

data class TestLabState(
    val isRunning: Boolean = false,
    val activeTelemetry: SimulationTelemetry? = null,
    val history: List<SimulationTelemetry> = emptyList(),
    val showLiveOverlayBanner: Boolean = false,
    val activePostCallDecision: ScreeningDecision? = null
)

class CallShieldViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NtkCallShieldApplication
    private val callerRepo = app.callerRepository
    private val threatRepo = app.threatRepository
    private val prefs = app.preferences

    val settings: StateFlow<UserSettings> = prefs.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val callLogs: StateFlow<List<CallLogEntity>> = callerRepo.callLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val phoneIntelList: StateFlow<List<PhoneIntelEntity>> = callerRepo.allPhoneIntel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockingRules: StateFlow<List<BlockingRuleEntity>> = callerRepo.blockingRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<ContactEntity>> = callerRepo.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val smsMessages: StateFlow<List<SmsMessageEntity>> = threatRepo.smsMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineRegions: StateFlow<List<OfflineRegionEntity>> = callerRepo.offlineRegions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privacyAudits: StateFlow<List<PrivacyAuditEntity>> = callerRepo.privacyAudits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val missions: StateFlow<List<MissionProgressEntity>> = callerRepo.missions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklySummary: StateFlow<WeeklySafetySummary> = combine(callLogs, settings) { logs, userSettings ->
        val blocked = logs.count { it.disposition == CallDisposition.BLOCKED.name }
        val silenced = logs.count { it.disposition == CallDisposition.SILENCED.name }
        WeeklySafetySummary(
            callsChecked = logs.size,
            scamsStopped = blocked,
            badLinksCaught = userSettings.urlsNeutralizedCount,
            timeSavedMinutes = (blocked * 3) + (silenced * 2),
            dailyBreakdown = buildSevenDayBreakdown(logs)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklySafetySummary())

    val guardianBadges: StateFlow<List<GuardianBadge>> = settings
        .combine(missions) { s, _ ->
            val pts = s.guardianPoints
            listOf(
                GuardianBadge("b1", "Calm Watcher", "Reach 100 Guardian Points", 100, pts >= 100),
                GuardianBadge("b2", "Scam Stopper", "Reach 250 Guardian Points", 250, pts >= 250),
                GuardianBadge("b3", "Trusted Sentinel", "Reach 450 Guardian Points", 450, pts >= 450),
                GuardianBadge("b4", "Shield Champion", "Reach 700 Guardian Points", 700, pts >= 700)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCallFilter = MutableStateFlow(CallFilter.ALL)
    val selectedCallFilter: StateFlow<CallFilter> = _selectedCallFilter.asStateFlow()

    private val _selectedSimFilter = MutableStateFlow(0)
    val selectedSimFilter: StateFlow<Int> = _selectedSimFilter.asStateFlow()

    private val _selectedCallNumberFilter = MutableStateFlow<String?>(null)
    val selectedCallNumberFilter: StateFlow<String?> = _selectedCallNumberFilter.asStateFlow()

    private val _isSyncingDeviceData = MutableStateFlow(false)
    val isSyncingDeviceData: StateFlow<Boolean> = _isSyncingDeviceData.asStateFlow()

    private val _lookupQuery = MutableStateFlow("")
    val lookupQuery: StateFlow<String> = _lookupQuery.asStateFlow()

    private val _lookupReport = MutableStateFlow<PhoneIntelligenceReport?>(null)
    val lookupReport: StateFlow<PhoneIntelligenceReport?> = _lookupReport.asStateFlow()

    private val _isLookingUpNumber = MutableStateFlow(false)
    val isLookingUpNumber: StateFlow<Boolean> = _isLookingUpNumber.asStateFlow()

    private val _urlScanInput = MutableStateFlow("")
    val urlScanInput: StateFlow<String> = _urlScanInput.asStateFlow()

    private val _urlScanResult = MutableStateFlow<UrlRiskAnalysis?>(null)
    val urlScanResult: StateFlow<UrlRiskAnalysis?> = _urlScanResult.asStateFlow()

    private val _selectedSmsCategory = MutableStateFlow<SmsCategory?>(null)
    val selectedSmsCategory: StateFlow<SmsCategory?> = _selectedSmsCategory.asStateFlow()

    private val _isVisualContactGrid = MutableStateFlow(false)
    val isVisualContactGrid: StateFlow<Boolean> = _isVisualContactGrid.asStateFlow()

    private val _contactSearchQuery = MutableStateFlow("")
    val contactSearchQuery: StateFlow<String> = _contactSearchQuery.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<Map<String, List<ContactEntity>>>(emptyMap())
    val duplicateGroups: StateFlow<Map<String, List<ContactEntity>>> = _duplicateGroups.asStateFlow()

    private val _exportedContactsJson = MutableStateFlow<String?>(null)
    val exportedContactsJson: StateFlow<String?> = _exportedContactsJson.asStateFlow()

    private val _lastDeltaResult = MutableStateFlow<DeltaUpdateResult?>(null)
    val lastDeltaResult: StateFlow<DeltaUpdateResult?> = _lastDeltaResult.asStateFlow()

    private val _permissionStatus = MutableStateFlow(RoleAndPermissionHelper.inspectStatus(application))
    val permissionStatus: StateFlow<TelecomPermissionStatus> = _permissionStatus.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val _snackbarEvent = MutableStateFlow<SnackbarEvent?>(null)
    val snackbarEvent: StateFlow<SnackbarEvent?> = _snackbarEvent.asStateFlow()

    private val _testLabState = MutableStateFlow(TestLabState())
    val testLabState: StateFlow<TestLabState> = _testLabState.asStateFlow()

    private val _reopenOnboarding = MutableStateFlow(false)
    val reopenOnboarding: StateFlow<Boolean> = _reopenOnboarding.asStateFlow()

    val simulationScenarios: List<SimulationScenario> = listOf(
        SimulationScenario(
            id = "sim_verified_bank",
            title = "1. Verified Bank Call",
            plainDescription = "Carrier Verified ✓ · Shows a calm Trust Teal popup so you know it's safe.",
            phoneNumber = "+18009359935",
            verificationStatus = CarrierVerificationStatus.VERIFIED,
            simSlot = 1
        ),
        SimulationScenario(
            id = "sim_scam_robocall",
            title = "2. Known Scam Robocall",
            plainDescription = "Matches your offline scam list and gets blocked silently before ringing.",
            phoneNumber = "+18885550194",
            verificationStatus = CarrierVerificationStatus.SPOOF_WARNING,
            simSlot = 1
        ),
        SimulationScenario(
            id = "sim_spoofed_caller",
            title = "3. Spoofed Caller Alert",
            plainDescription = "Carrier couldn't verify the caller · Silences the ring and warns you in amber.",
            phoneNumber = "+16465550154",
            verificationStatus = CarrierVerificationStatus.SPOOF_WARNING,
            simSlot = 2
        ),
        SimulationScenario(
            id = "sim_voip_telemarketer",
            title = "4. Unknown VoIP Telemarketer",
            plainDescription = "Looks up caller details and shows a floating Caller ID card + post-call summary.",
            phoneNumber = "+14155550182",
            verificationStatus = CarrierVerificationStatus.UNVERIFIED,
            simSlot = 1
        ),
        SimulationScenario(
            id = "sim_repeat_urgent",
            title = "5. Urgent Repeat Caller",
            plainDescription = "Calls twice in a row to show how Repeat Caller Bypass lets emergencies ring through.",
            phoneNumber = "+13105550168",
            verificationStatus = CarrierVerificationStatus.UNVERIFIED,
            simSlot = 1,
            isRepeatTest = true
        ),
        SimulationScenario(
            id = "sim_phishing_sms",
            title = "6. Fake Bank Phishing SMS",
            plainDescription = "Places a scam text in Smart SMS and triggers an instant high-risk link alert.",
            phoneNumber = "+13055550181",
            verificationStatus = CarrierVerificationStatus.UNVERIFIED,
            simSlot = 2,
            isSmsAttack = true,
            smsBody = "FINAL NOTICE: Your account suspended due to unpaid toll. Verify immediately at http://usps-redeliver-check.xyz/claim?id=9481 within 24 hours."
        )
    )

    init {
        viewModelScope.launch {
            callerRepo.ensureSeeded()
            refreshPermissionsAndSyncDeviceData()
        }
    }

    fun clearSnackbar() {
        _snackbarEvent.value = null
    }

    fun showMessage(msg: String) {
        _snackbarEvent.value = SnackbarEvent(message = msg)
    }

    fun refreshPermissionStatus() {
        _permissionStatus.value = RoleAndPermissionHelper.inspectStatus(app)
    }

    fun refreshPermissionsAndSyncDeviceData(showToast: Boolean = false) {
        val status = RoleAndPermissionHelper.inspectStatus(app)
        _permissionStatus.value = status
        viewModelScope.launch {
            if (_isSyncingDeviceData.value) return@launch
            _isSyncingDeviceData.value = true
            try {
                callerRepo.ensureSeeded()
                val syncedContacts = if (status.hasContactsPermission) {
                    callerRepo.syncDeviceContacts(app)
                } else {
                    0
                }
                val syncedCalls = if (status.hasCallLogPermission) {
                    callerRepo.syncDeviceCallLogs(app)
                } else {
                    0
                }
                val syncedSms = if (status.hasSmsPermission) {
                    threatRepo.syncDeviceSmsInbox(app)
                } else {
                    0
                }
                if (syncedContacts > 0) {
                    _duplicateGroups.value = callerRepo.findDuplicateContactGroups()
                }
                if (showToast) {
                    showMessage("Synced $syncedContacts contacts · $syncedCalls calls · $syncedSms messages")
                }
            } finally {
                _isSyncingDeviceData.value = false
            }
        }
    }

    // --- Onboarding ---
    fun completeOnboarding(preset: ProtectionPreset) {
        viewModelScope.launch {
            prefs.setProtectionPreset(preset)
            prefs.setMasterProtectionEnabled(true)
            prefs.setOnboardingCompleted(true)
            _reopenOnboarding.value = false
            showMessage("NTK CallShield is ready (${preset.title})")
        }
    }

    fun exploreDemoFirst() {
        viewModelScope.launch {
            prefs.setOnboardingCompleted(true)
            _reopenOnboarding.value = false
        }
    }

    fun reopenSetupWizard() {
        _reopenOnboarding.value = true
    }

    fun closeSetupWizard() {
        _reopenOnboarding.value = false
    }

    // --- PIN Lock ---
    fun verifyPin(pin: String) {
        val currentHash = settings.value.pinHash
        if (UserSettings.hashPin(pin) == currentHash) {
            _isAppUnlocked.value = true
            _pinError.value = null
        } else {
            _pinError.value = "Incorrect 4-digit PIN. Please try again."
        }
    }

    fun unlockWithBiometric() {
        _isAppUnlocked.value = true
        _pinError.value = null
    }

    fun togglePinLock(enabled: Boolean, customPin: String? = null) {
        viewModelScope.launch {
            prefs.setPinLock(enabled, customPin)
            if (enabled) {
                _isAppUnlocked.value = true
                showMessage(if (customPin != null) "PIN updated & App Lock turned on" else "App Lock turned on (PIN: 1234)")
            } else {
                showMessage("App Lock turned off")
            }
        }
    }

    // --- Master Shield & Presets ---
    fun setMasterProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setMasterProtectionEnabled(enabled)
            showMessage(if (enabled) "NTK CallShield is active" else "Protection paused")
        }
    }

    fun setProtectionPreset(preset: ProtectionPreset) {
        viewModelScope.launch {
            prefs.setProtectionPreset(preset)
            showMessage("Protection set to ${preset.title}")
        }
    }

    // --- Calls, Filtering & Undo Actions ---
    fun setCallFilter(filter: CallFilter) {
        _selectedCallFilter.value = filter
    }

    fun setSimFilter(sim: Int) {
        _selectedSimFilter.value = sim
    }

    fun setCallNumberFilter(rawNumber: String?) {
        val normalized = rawNumber?.let {
            if (PhoneNumberNormalizer.isHiddenOrPrivate(it)) "PRIVATE"
            else PhoneNumberNormalizer.normalize(it)
        }?.takeIf { it.isNotEmpty() }
        _selectedCallNumberFilter.value = normalized
        if (normalized != null) {
            _selectedCallFilter.value = CallFilter.ALL
            _selectedSimFilter.value = 0
        }
    }

    fun clearCallNumberFilter() {
        _selectedCallNumberFilter.value = null
    }

    fun blockNumberWithUndo(number: String, name: String) {
        viewModelScope.launch {
            val previous = callerRepo.findIntelSnapshot(number)
            callerRepo.setBlacklistStatus(number, name, blacklisted = true)
            if (_lookupReport.value?.normalizedNumber == PhoneNumberNormalizer.normalize(number)) {
                _lookupReport.value = callerRepo.reverseLookup(number)
            }
            _snackbarEvent.value = SnackbarEvent(
                message = "Blocked ${PhoneNumberNormalizer.formatForDisplay(number)}",
                undoLabel = "Undo",
                onUndo = {
                    viewModelScope.launch {
                        callerRepo.restoreIntelSnapshot(number, previous)
                        if (_lookupReport.value?.normalizedNumber == PhoneNumberNormalizer.normalize(number)) {
                            _lookupReport.value = callerRepo.reverseLookup(number)
                        }
                    }
                }
            )
        }
    }

    fun whitelistNumberWithUndo(number: String, name: String) {
        viewModelScope.launch {
            val previous = callerRepo.findIntelSnapshot(number)
            callerRepo.setWhitelistStatus(number, name, whitelisted = true)
            if (_lookupReport.value?.normalizedNumber == PhoneNumberNormalizer.normalize(number)) {
                _lookupReport.value = callerRepo.reverseLookup(number)
            }
            _snackbarEvent.value = SnackbarEvent(
                message = "Added ${PhoneNumberNormalizer.formatForDisplay(number)} to Safe List",
                undoLabel = "Undo",
                onUndo = {
                    viewModelScope.launch {
                        callerRepo.restoreIntelSnapshot(number, previous)
                        if (_lookupReport.value?.normalizedNumber == PhoneNumberNormalizer.normalize(number)) {
                            _lookupReport.value = callerRepo.reverseLookup(number)
                        }
                    }
                }
            )
        }
    }

    fun removeFromSafeOrBlockListWithUndo(entity: PhoneIntelEntity, wasWhitelist: Boolean) {
        viewModelScope.launch {
            val previous = entity
            if (wasWhitelist) {
                callerRepo.setWhitelistStatus(entity.normalizedNumber, entity.callerName, false)
            } else {
                callerRepo.setBlacklistStatus(entity.normalizedNumber, entity.callerName, false)
            }
            _snackbarEvent.value = SnackbarEvent(
                message = "Removed ${entity.callerName}",
                undoLabel = "Undo",
                onUndo = {
                    viewModelScope.launch {
                        callerRepo.restoreIntelSnapshot(entity.normalizedNumber, previous)
                    }
                }
            )
        }
    }

    fun deleteCallLogWithUndo(log: CallLogEntity) {
        viewModelScope.launch {
            callerRepo.deleteCallLog(log.id)
            _snackbarEvent.value = SnackbarEvent(
                message = "Removed call from history",
                undoLabel = "Undo",
                onUndo = {
                    viewModelScope.launch { callerRepo.restoreCallLog(log) }
                }
            )
        }
    }

    fun reportNumberWithUndo(rawNumber: String, callerName: String, category: ThreatCategory, blockLocally: Boolean) {
        viewModelScope.launch {
            val previous = callerRepo.findIntelSnapshot(rawNumber)
            callerRepo.reportNumber(rawNumber, callerName, category, blockLocally)
            if (_lookupReport.value?.normalizedNumber == PhoneNumberNormalizer.normalize(rawNumber)) {
                _lookupReport.value = callerRepo.reverseLookup(rawNumber)
            }
            _snackbarEvent.value = SnackbarEvent(
                message = "Reported as ${category.label}",
                undoLabel = "Undo",
                onUndo = {
                    viewModelScope.launch {
                        callerRepo.restoreIntelSnapshot(rawNumber, previous)
                        if (_lookupReport.value?.normalizedNumber == PhoneNumberNormalizer.normalize(rawNumber)) {
                            _lookupReport.value = callerRepo.reverseLookup(rawNumber)
                        }
                    }
                }
            )
        }
    }

    // --- Rules & Guards ---
    fun setBlockHiddenNumbers(enabled: Boolean) {
        viewModelScope.launch { prefs.setBlockHiddenNumbers(enabled) }
    }

    fun setBlockUnverifiedVoip(enabled: Boolean) {
        viewModelScope.launch { prefs.setBlockUnverifiedVoip(enabled) }
    }

    fun setRepeatedCallerBypass(enabled: Boolean, windowMinutes: Int? = null) {
        viewModelScope.launch { prefs.setRepeatedCallerBypass(enabled, windowMinutes) }
    }

    fun setDefaultBlockAction(action: BlockAction) {
        viewModelScope.launch { prefs.setDefaultBlockAction(action) }
    }

    fun addBlockingRule(label: String, ruleType: RuleType, pattern: String, action: BlockAction) {
        if (pattern.isBlank() && ruleType != RuleType.HIDDEN_PRIVATE && ruleType != RuleType.UNVERIFIED_VOIP) {
            showMessage("Please enter a country code, area code, or number pattern")
            return
        }
        viewModelScope.launch {
            val effectivePattern = when (ruleType) {
                RuleType.HIDDEN_PRIVATE -> "PRIVATE"
                RuleType.UNVERIFIED_VOIP -> "VOIP-UNVERIFIED"
                else -> pattern.trim()
            }
            callerRepo.addBlockingRule(label, ruleType, effectivePattern, action)
            showMessage("Added rule: ${label.ifBlank { effectivePattern }}")
        }
    }

    fun toggleBlockingRule(rule: BlockingRuleEntity) {
        viewModelScope.launch { callerRepo.toggleBlockingRule(rule) }
    }

    fun deleteBlockingRuleWithUndo(rule: BlockingRuleEntity) {
        viewModelScope.launch {
            callerRepo.deleteBlockingRule(rule)
            _snackbarEvent.value = SnackbarEvent(
                message = "Deleted rule '${rule.label}'",
                undoLabel = "Undo",
                onUndo = {
                    viewModelScope.launch { callerRepo.restoreBlockingRule(rule) }
                }
            )
        }
    }

    // --- Offline Database ---
    fun applyRegionalDeltaUpdate(regionId: String) {
        viewModelScope.launch {
            val result = callerRepo.applyRegionalDeltaUpdate(regionId)
            _lastDeltaResult.value = result
            if (result != null) {
                showMessage("${result.regionName} updated (+${result.additionsCount} added, -${result.deletionsCount} removed)")
            }
        }
    }

    fun setAutoUpdateWifiOnly(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoUpdateWifiOnly(enabled) }
    }

    // --- Check Hub (Reverse Lookup, URL Scanner, Smart SMS) ---
    fun updateLookupQuery(q: String) {
        _lookupQuery.value = q
    }

    fun runReverseLookup(inputOverride: String? = null) {
        val target = (inputOverride ?: _lookupQuery.value).trim()
        if (target.isEmpty()) return
        _lookupQuery.value = target
        viewModelScope.launch {
            _isLookingUpNumber.value = true
            delay(160L)
            _lookupReport.value = callerRepo.reverseLookup(target)
            _isLookingUpNumber.value = false
        }
    }

    fun updateUrlScanInput(text: String) {
        _urlScanInput.value = text
    }

    fun runUrlPhishingScan(inputOverride: String? = null) {
        val target = (inputOverride ?: _urlScanInput.value).trim()
        if (target.isEmpty()) return
        _urlScanInput.value = target
        viewModelScope.launch {
            _urlScanResult.value = threatRepo.analyzeUrlOrText(target, recordMission = true)
        }
    }

    fun setSmsCategoryFilter(category: SmsCategory?) {
        _selectedSmsCategory.value = category
    }

    fun pasteAndIngestSms(sender: String, body: String) {
        if (body.isBlank()) return
        viewModelScope.launch {
            val ingested = threatRepo.ingestSmsMessage(sender, body)
            _urlScanResult.value = threatRepo.analyzeUrlOrText(body, recordMission = true)
            showMessage("Sorted into ${SmsCategory.valueOf(ingested.category).label}")
        }
    }

    fun deleteSmsWithUndo(sms: SmsMessageEntity) {
        viewModelScope.launch {
            threatRepo.deleteSms(sms.id)
            _snackbarEvent.value = SnackbarEvent(
                message = "Deleted message from ${sms.sender}",
                undoLabel = "Undo",
                onUndo = {
                    viewModelScope.launch { threatRepo.restoreSms(sms) }
                }
            )
        }
    }

    // --- Contacts, Duplicates, Backup & Caller Card ---
    fun toggleContactViewMode() {
        _isVisualContactGrid.update { !it }
    }

    fun updateContactSearch(query: String) {
        _contactSearchQuery.value = query
    }

    fun saveContact(displayName: String, rawNumber: String, roleOrLabel: String, whitelisted: Boolean) {
        viewModelScope.launch {
            callerRepo.saveContact(displayName, rawNumber, roleOrLabel, whitelisted)
            refreshDuplicates()
            showMessage("Saved $displayName to your contacts")
        }
    }

    fun toggleContactWhitelist(contact: ContactEntity) {
        viewModelScope.launch {
            callerRepo.toggleContactWhitelist(contact)
            showMessage(
                if (!contact.isWhitelisted) "${contact.displayName} added to Safe List"
                else "${contact.displayName} removed from Safe List"
            )
        }
    }

    fun refreshDuplicates() {
        viewModelScope.launch {
            _duplicateGroups.value = callerRepo.findDuplicateContactGroups()
        }
    }

    fun mergeDuplicateGroup(normalizedNumber: String) {
        viewModelScope.launch {
            val merged = callerRepo.mergeDuplicateGroup(normalizedNumber)
            refreshDuplicates()
            showMessage("Merged $merged duplicate entry")
        }
    }

    fun mergeAllDuplicates() {
        viewModelScope.launch {
            val count = callerRepo.mergeAllDuplicates()
            refreshDuplicates()
            showMessage(if (count > 0) "Merged $count duplicate contacts" else "Your contacts are already clean")
        }
    }

    fun exportContactsBackup() {
        viewModelScope.launch {
            _exportedContactsJson.value = callerRepo.exportContactsToJson()
            showMessage("Contacts backup ready")
        }
    }

    fun restoreContactsBackup(json: String) {
        viewModelScope.launch {
            runCatching {
                val restored = callerRepo.importContactsFromJson(json)
                refreshDuplicates()
                showMessage("Restored $restored contacts")
            }.onFailure {
                showMessage("Could not read backup JSON")
            }
        }
    }

    fun updateMyCallerCard(card: MyCallerCard) {
        viewModelScope.launch {
            prefs.updateMyCallerCard(card)
            showMessage("Updated My NTK Caller Card")
        }
    }

    // --- Privacy, Appearance & Overlay ---
    fun setZeroTelemetryMode(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setZeroTelemetryMode(enabled)
            callerRepo.recordPrivacyAudit(
                eventType = "PRIVACY SETTING",
                detail = if (enabled) "Private Offline-Only Mode turned ON. Zero network lookups."
                else "Smart Cloud + Offline Mode turned ON for unlisted callers.",
                leftDevice = false
            )
            if (enabled) callerRepo.incrementMissionProgress("mission_zero_telemetry")
            showMessage(if (enabled) "Private Offline Mode is ON" else "Smart Cloud + Offline Mode is ON")
        }
    }

    fun submitUnlistingRequest(rawNumber: String) {
        val norm = PhoneNumberNormalizer.normalize(rawNumber)
        if (norm.isEmpty()) {
            showMessage("Enter a valid phone number")
            return
        }
        viewModelScope.launch {
            prefs.setUnlistingRequest(norm, true)
            callerRepo.recordPrivacyAudit(
                eventType = "NUMBER UNLISTED",
                detail = "Recorded public directory removal for ${PhoneNumberNormalizer.formatForDisplay(norm)}.",
                leftDevice = false
            )
            showMessage("Your number is marked for unlisting")
        }
    }

    fun clearPrivacyAudits() {
        viewModelScope.launch {
            callerRepo.clearPrivacyAudits()
            showMessage("Cleared privacy activity log")
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefs.setUseDynamicColor(enabled) }
    }

    fun updateOverlayConfig(style: OverlayStyle? = null, verticalPosition: Float? = null) {
        viewModelScope.launch { prefs.setOverlayConfig(style, verticalPosition) }
    }

    fun setBiometricAllowed(allowed: Boolean) {
        viewModelScope.launch { prefs.setBiometricAllowed(allowed) }
    }

    // --- Test My Shield Live Simulator ---
    fun runSimulationScenario(scenario: SimulationScenario) {
        viewModelScope.launch {
            _testLabState.update { it.copy(isRunning = true) }

            if (scenario.isSmsAttack) {
                val startNanos = System.nanoTime()
                val body = scenario.smsBody.orEmpty()
                val ingested = threatRepo.ingestSmsMessage(scenario.phoneNumber, body)
                val analysis = threatRepo.analyzeUrlOrText(body, recordMission = true)
                val elapsed = ((System.nanoTime() - startNanos) / 1_000_000L).coerceAtLeast(1L)
                callerRepo.incrementMissionProgress("mission_test_lab")

                val telemetry = SimulationTelemetry(
                    scenarioTitle = scenario.title,
                    plainOutcome = "Caught a fake bank text! Moved to Blocked Scams with Risk ${analysis.riskScore}/100.",
                    timestampMillis = System.currentTimeMillis(),
                    phase1LatencyMs = elapsed,
                    compliantUnder500Ms = elapsed < 500L,
                    decision = null,
                    smsAnalysis = analysis,
                    technicalLog = "SMS #${ingested.id} -> ${SmsCategory.valueOf(ingested.category).label} · Score ${analysis.riskScore}/100 · URL: ${analysis.extractedUrls.firstOrNull() ?: "None"}",
                    showOverlayPreview = false,
                    showAfterCallReady = false
                )
                _testLabState.update {
                    it.copy(
                        isRunning = false,
                        activeTelemetry = telemetry,
                        history = (listOf(telemetry) + it.history).take(12),
                        showLiveOverlayBanner = false,
                        activePostCallDecision = null
                    )
                }
                showMessage("Fake phishing text caught (${analysis.riskScore}/100)")
                return@launch
            }

            val decision = if (scenario.isRepeatTest) {
                val prevPreset = settings.value.protectionPreset
                val prevMaster = settings.value.masterProtectionEnabled
                if (!prevMaster) prefs.setMasterProtectionEnabled(true)
                if (prevPreset == ProtectionPreset.GENTLE) prefs.setProtectionPreset(ProtectionPreset.STRICT)

                val firstAttempt = callerRepo.screenIncomingCall(
                    rawHandle = scenario.phoneNumber,
                    verificationStatus = CarrierVerificationStatus.SPOOF_WARNING,
                    simSlot = scenario.simSlot,
                    persistLog = true
                )
                val secondAttempt = callerRepo.screenIncomingCall(
                    rawHandle = scenario.phoneNumber,
                    verificationStatus = scenario.verificationStatus,
                    simSlot = scenario.simSlot,
                    persistLog = true
                )

                if (!prevMaster) prefs.setMasterProtectionEnabled(false)
                if (prevPreset == ProtectionPreset.GENTLE) prefs.setProtectionPreset(prevPreset)

                secondAttempt.copy(
                    matchedRule = "Call #1 ${firstAttempt.disposition.badgeLabel} -> Call #2 ${secondAttempt.matchedRule}"
                )
            } else {
                callerRepo.screenIncomingCall(
                    rawHandle = scenario.phoneNumber,
                    verificationStatus = scenario.verificationStatus,
                    simSlot = scenario.simSlot,
                    persistLog = true
                )
            }

            callerRepo.incrementMissionProgress("mission_test_lab")

            val techLog = buildString {
                append("Phase-1 Latency: ${decision.latencyMs}ms (<500ms SLA ${if (decision.latencyMs < 500) "PASS" else "WARN"})\n")
                append("CallResponse Flags: ${decision.responseFlags.summaryString()}\n")
                append("Matched Rule: ${decision.matchedRule}\n")
                append("Network Egress: ${if (decision.blockedExternalNetworkQuery) "Blocked by Private Offline Mode (0 bytes sent)" else "On-device / Allowed"}")
            }

            val telemetry = SimulationTelemetry(
                scenarioTitle = scenario.title,
                plainOutcome = "${decision.disposition.badgeLabel}: ${decision.plainReason}",
                timestampMillis = System.currentTimeMillis(),
                phase1LatencyMs = decision.latencyMs,
                compliantUnder500Ms = decision.latencyMs < 500L,
                decision = decision,
                smsAnalysis = null,
                technicalLog = techLog,
                showOverlayPreview = decision.showFloatingOverlay,
                showAfterCallReady = true
            )

            _testLabState.update {
                it.copy(
                    isRunning = false,
                    activeTelemetry = telemetry,
                    history = (listOf(telemetry) + it.history).take(12),
                    showLiveOverlayBanner = decision.showFloatingOverlay,
                    activePostCallDecision = null
                )
            }
            showMessage("${scenario.title}: ${decision.disposition.badgeLabel}")
        }
    }

    fun openPostCallSummary() {
        val currentDecision = _testLabState.value.activeTelemetry?.decision ?: return
        _testLabState.update {
            it.copy(
                showLiveOverlayBanner = false,
                activePostCallDecision = currentDecision
            )
        }
    }

    fun dismissPostCallSummary() {
        _testLabState.update { it.copy(activePostCallDecision = null) }
    }

    private fun buildSevenDayBreakdown(logs: List<CallLogEntity>): List<DaySafetyMetric> {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("EEE", Locale.US)
        val result = mutableListOf<DaySafetyMetric>()

        for (offset in 6 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            val targetYear = cal.get(Calendar.YEAR)
            val targetDay = cal.get(Calendar.DAY_OF_YEAR)
            val dayLabel = sdf.format(Date(cal.timeInMillis))

            val dayLogs = logs.filter { log ->
                val c = Calendar.getInstance().apply { timeInMillis = log.timestampMillis }
                c.get(Calendar.YEAR) == targetYear && c.get(Calendar.DAY_OF_YEAR) == targetDay
            }
            result.add(
                DaySafetyMetric(
                    dayLabel = dayLabel,
                    blockedCount = dayLogs.count { it.disposition == CallDisposition.BLOCKED.name },
                    cautionCount = dayLogs.count { it.disposition == CallDisposition.SILENCED.name || it.disposition == CallDisposition.MISSED.name },
                    safeCount = dayLogs.count { it.disposition == CallDisposition.ALLOWED.name || it.disposition == CallDisposition.BYPASSED_REPEAT.name }
                )
            )
        }
        return result
    }
}
