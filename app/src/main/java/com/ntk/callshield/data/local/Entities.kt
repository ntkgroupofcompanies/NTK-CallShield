package com.ntk.callshield.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phone_intel")
data class PhoneIntelEntity(
    @PrimaryKey val normalizedNumber: String,
    val displayNumber: String,
    val callerName: String,
    val riskScore: Int,
    val lineType: String, // LineType.name
    val carrier: String,
    val region: String,
    val verificationDefault: String, // CarrierVerificationStatus.name
    val category: String, // ThreatCategory.name
    val communityReports: Int,
    val communityTagsCsv: String,
    val simSwapRisk: String,
    val callForwardingRisk: String,
    val smsPumpingRisk: String,
    val isVerifiedBusiness: Boolean,
    val isFinancialInstitution: Boolean = false,
    val isWhitelisted: Boolean = false,
    val isBlacklisted: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val normalizedNumber: String,
    val displayNumber: String,
    val callerName: String,
    val disposition: String, // CallDisposition.name
    val riskScore: Int,
    val category: String, // ThreatCategory.name
    val lineType: String, // LineType.name
    val carrier: String,
    val verificationStatus: String, // CarrierVerificationStatus.name
    val matchedRule: String,
    val plainReason: String,
    val responseFlagsSummary: String,
    val screeningLatencyMs: Long,
    val simSlot: Int, // 1 or 2
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocking_rules")
data class BlockingRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val ruleType: String, // RuleType.name
    val pattern: String,
    val action: String, // BlockAction.name
    val enabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val normalizedNumber: String,
    val displayNumber: String,
    val roleOrLabel: String,
    val avatarColorHex: Long,
    val avatarUri: String? = null,
    val isWhitelisted: Boolean = true,
    val isFavorite: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "sms_messages")
data class SmsMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val category: String, // SmsCategory.name
    val riskScore: Int,
    val extractedOtp: String?,
    val extractedUrl: String?,
    val riskSummary: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "offline_regions")
data class OfflineRegionEntity(
    @PrimaryKey val regionId: String,
    val regionName: String,
    val coverageDescription: String,
    val version: String,
    val entryCount: Int,
    val sizeMb: Double,
    val lastSyncedMillis: Long,
    val isDownloaded: Boolean = true
)

@Entity(tableName = "privacy_audit_logs")
data class PrivacyAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val detail: String,
    val leftDevice: Boolean,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "guardian_missions")
data class MissionProgressEntity(
    @PrimaryKey val missionId: String,
    val title: String,
    val description: String,
    val currentProgress: Int,
    val targetProgress: Int,
    val rewardPoints: Int,
    val completed: Boolean = false
)
