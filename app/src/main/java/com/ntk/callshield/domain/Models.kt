package com.ntk.callshield.domain

import android.telecom.CallScreeningService

enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED"),
    SYSTEM("System")
}

enum class ProtectionPreset(
    val title: String,
    val badge: String?,
    val subtitle: String
) {
    GENTLE(
        title = "Gentle",
        badge = null,
        subtitle = "Shows Caller ID warnings; never auto-blocks calls."
    ),
    BALANCED(
        title = "Balanced",
        badge = "Recommended",
        subtitle = "Auto-blocks confirmed scams & robocalls; warns on suspicious callers."
    ),
    STRICT(
        title = "Strict",
        badge = null,
        subtitle = "Blocks all reported spam, hidden numbers, and spoofed caller IDs."
    )
}

enum class BlockAction(val label: String) {
    BLOCK_SILENTLY("Block Silently"),
    BLOCK_SHOW_MISSED("Block & Show Missed"),
    SILENCE_RINGTONE("Silence Ringtone"),
    SEND_TO_VOICEMAIL("Send to Voicemail")
}

enum class RuleType(val label: String) {
    COUNTRY_CODE("Country Code"),
    AREA_CODE("Area Code"),
    PREFIX_PATTERN("Prefix Pattern"),
    HIDDEN_PRIVATE("Private / Hidden"),
    UNVERIFIED_VOIP("Unverified Internet Call")
}

enum class CarrierVerificationStatus(
    val plainLabel: String,
    val technicalLabel: String,
    val code: Int
) {
    VERIFIED("Carrier Verified ✓", "STIR/SHAKEN PASSED (A)", 1),
    SPOOF_WARNING("Spoof Warning ⚠️", "STIR/SHAKEN FAILED", 2),
    UNVERIFIED("Unverified Caller", "NOT VERIFIED", 0);

    companion object {
        fun fromConnectionVerification(status: Int): CarrierVerificationStatus = when (status) {
            1 -> VERIFIED // Connection.VERIFICATION_STATUS_PASSED
            2 -> SPOOF_WARNING // Connection.VERIFICATION_STATUS_FAILED
            else -> UNVERIFIED
        }
    }
}

enum class LineType(val label: String) {
    MOBILE("Mobile"),
    LANDLINE("Landline"),
    VOIP("Internet VoIP"),
    TOLL_FREE("Toll-Free"),
    UNKNOWN("Unknown")
}

enum class ThreatCategory(val label: String) {
    VERIFIED_BANK("Verified Bank"),
    VERIFIED_BUSINESS("Verified Business"),
    PERSONAL("Personal"),
    TELEMARKETER("Telemarketer"),
    BANK_IMPERSONATION("Fake Bank Scam"),
    ROBOCALL("Scam Robocall"),
    DELIVERY_SCAM("Delivery Scam"),
    TECH_SUPPORT_SCAM("Tech Support Scam"),
    INTERNATIONAL_FRAUD("International Fraud"),
    UNKNOWN("Unknown Caller")
}

enum class CallDisposition(val label: String, val badgeLabel: String) {
    ALLOWED("Allowed", "Safe"),
    BLOCKED("Blocked", "Blocked Scam"),
    SILENCED("Silenced", "Caution"),
    MISSED("Missed", "Missed"),
    BYPASSED_REPEAT("Allowed (Repeat Call)", "Repeat Call")
}

enum class SmsCategory(val label: String) {
    PERSONAL("Personal"),
    CODES_ALERTS("Codes & Alerts"),
    PROMOS("Promos"),
    BLOCKED_SCAMS("Blocked Scams")
}

enum class OverlayStyle(val label: String, val description: String) {
    COMPACT_PILL("Compact Pill", "Slim badge showing caller name and safety dot"),
    RICH_CARD("Rich Card", "Full caller name, line type, and safety meter"),
    ALERT_BANNER("Full Alert Banner", "High-contrast color header for instant visibility")
}

/**
 * Strictly validated representation of Android CallScreeningService.CallResponse flags.
 * Enforces Android OS invariants:
 * - rejectCall, skipCallLog, skipNotification may ONLY be true if disallowCall == true.
 * - silenceCall may ONLY be true if disallowCall == false.
 */
data class ScreeningCallResponseFlags(
    val disallowCall: Boolean,
    val rejectCall: Boolean,
    val silenceCall: Boolean,
    val skipCallLog: Boolean,
    val skipNotification: Boolean
) {
    init {
        if (!disallowCall) {
            require(!rejectCall) { "OS Invariant violated: rejectCall cannot be true when disallowCall is false" }
            require(!skipCallLog) { "OS Invariant violated: skipCallLog cannot be true when disallowCall is false" }
            require(!skipNotification) { "OS Invariant violated: skipNotification cannot be true when disallowCall is false" }
        } else {
            require(!silenceCall) { "OS Invariant violated: silenceCall cannot be true when disallowCall is true" }
        }
    }

    fun toAndroidCallResponse(): CallScreeningService.CallResponse {
        val builder = CallScreeningService.CallResponse.Builder()
            .setDisallowCall(disallowCall)
        if (disallowCall) {
            builder.setRejectCall(rejectCall)
            builder.setSkipCallLog(skipCallLog)
            builder.setSkipNotification(skipNotification)
        } else {
            builder.setSilenceCall(silenceCall)
        }
        return builder.build()
    }

    fun summaryString(): String = buildString {
        append("disallowCall=$disallowCall, ")
        append("rejectCall=$rejectCall, ")
        append("silenceCall=$silenceCall, ")
        append("skipCallLog=$skipCallLog, ")
        append("skipNotification=$skipNotification")
    }
}

data class ScreeningDecision(
    val normalizedNumber: String,
    val disposition: CallDisposition,
    val responseFlags: ScreeningCallResponseFlags,
    val matchedRule: String,
    val plainReason: String,
    val riskScore: Int,
    val displayName: String,
    val category: ThreatCategory,
    val lineType: LineType,
    val carrier: String,
    val verificationStatus: CarrierVerificationStatus,
    val latencyMs: Long,
    val showFloatingOverlay: Boolean,
    val triggerAsyncEnrichment: Boolean,
    val blockedExternalNetworkQuery: Boolean = false
)

data class PhoneIntelligenceReport(
    val normalizedNumber: String,
    val displayNumber: String,
    val callerName: String,
    val riskScore: Int,
    val lineType: LineType,
    val carrier: String,
    val region: String,
    val verificationStatus: CarrierVerificationStatus,
    val category: ThreatCategory,
    val communityReports: Int,
    val communityTags: List<String>,
    val simSwapRisk: String,
    val callForwardingRisk: String,
    val smsPumpingRisk: String,
    val isVerifiedBusiness: Boolean,
    val isFinancialInstitution: Boolean,
    val isWhitelisted: Boolean,
    val isBlacklisted: Boolean,
    val sourceLabel: String
)

data class UrlRiskFactor(
    val title: String,
    val detail: String,
    val weight: Int
)

data class UrlRiskAnalysis(
    val inputText: String,
    val extractedUrls: List<String>,
    val extractedOtp: String?,
    val riskScore: Int,
    val severity: String,
    val factors: List<UrlRiskFactor>,
    val recommendedAction: String
)

data class DeltaUpdateResult(
    val regionId: String,
    val regionName: String,
    val previousCount: Int,
    val additionsCount: Int,
    val deletionsCount: Int,
    val newCount: Int,
    val newVersion: String,
    val updatedAtMillis: Long
)

data class MyCallerCard(
    val displayName: String = "Alex Morgan",
    val roleCompany: String = "Product Lead · NTK Shield Studio",
    val phoneNumber: String = "+1 (415) 890-4421",
    val statusNote: String = "Calling about our scheduled check-in",
    val avatarUri: String? = null,
    val verifiedBadge: Boolean = true
)

data class SimulationScenario(
    val id: String,
    val title: String,
    val plainDescription: String,
    val phoneNumber: String,
    val verificationStatus: CarrierVerificationStatus,
    val simSlot: Int = 1,
    val isSmsAttack: Boolean = false,
    val smsBody: String? = null,
    val isRepeatTest: Boolean = false
)

data class SimulationTelemetry(
    val scenarioTitle: String,
    val plainOutcome: String,
    val timestampMillis: Long,
    val phase1LatencyMs: Long,
    val compliantUnder500Ms: Boolean,
    val decision: ScreeningDecision?,
    val smsAnalysis: UrlRiskAnalysis?,
    val technicalLog: String,
    val showOverlayPreview: Boolean,
    val showAfterCallReady: Boolean
)

data class DaySafetyMetric(
    val dayLabel: String,
    val blockedCount: Int,
    val cautionCount: Int,
    val safeCount: Int
)

data class GuardianBadge(
    val id: String,
    val title: String,
    val description: String,
    val requiredPoints: Int,
    val unlocked: Boolean
)
