package com.ntk.callshield.telecom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.ntk.callshield.data.local.CallLogEntity
import com.ntk.callshield.data.local.ContactEntity
import com.ntk.callshield.data.local.PhoneIntelEntity
import com.ntk.callshield.data.local.SmsMessageEntity
import com.ntk.callshield.domain.CallDisposition
import com.ntk.callshield.domain.CarrierVerificationStatus
import com.ntk.callshield.domain.LineType
import com.ntk.callshield.domain.PhoneNumberNormalizer
import com.ntk.callshield.domain.ScamUrlAnalyzer
import com.ntk.callshield.domain.ThreatCategory
import kotlin.math.abs

/**
 * Strictly on-device ContentResolver reader for live device Contacts, Call Log, and SMS Inbox.
 * Runs only on user interaction / launch / resume with zero background polling and zero uploads.
 */
object DeviceDataSyncHelper {

    private val AVATAR_PALETTE = listOf(
        0xFF0D9488,
        0xFF0284C7,
        0xFF10B981,
        0xFFF59E0B,
        0xFFEC4899,
        0xFF6366F1
    )

    fun numbersMatch(a: String?, b: String?): Boolean {
        val digitsA = a.orEmpty().filter(Char::isDigit)
        val digitsB = b.orEmpty().filter(Char::isDigit)
        if (digitsA.isEmpty() || digitsB.isEmpty()) return false
        if (digitsA == digitsB) return true
        val minLen = minOf(digitsA.length, digitsB.length)
        if (minLen < 7) return false
        val compareLen = minOf(10, minLen)
        return digitsA.takeLast(compareLen) == digitsB.takeLast(compareLen)
    }

    fun readDeviceContacts(context: Context): List<ContactEntity> {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return emptyList()

        val result = mutableListOf<ContactEntity>()
        val seen = HashSet<String>()

        runCatching {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.STARRED
            )
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val starIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

                while (cursor.moveToNext()) {
                    val rawNum = if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else ""
                    val normalized = PhoneNumberNormalizer.normalize(rawNum)
                    if (normalized.isEmpty()) continue

                    val displayName = (if (nameIdx >= 0) cursor.getString(nameIdx) else null)
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?: PhoneNumberNormalizer.formatForDisplay(normalized)

                    val dedupKey = "${displayName.lowercase()}|$normalized"
                    if (!seen.add(dedupKey)) continue

                    val phoneType = if (typeIdx >= 0) cursor.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    val customLabel = if (labelIdx >= 0) cursor.getString(labelIdx) else null
                    val roleLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        context.resources,
                        phoneType,
                        customLabel
                    ).toString().ifBlank { "Mobile" }

                    val thumbUri = if (photoIdx >= 0) cursor.getString(photoIdx) else null
                    val isStarred = if (starIdx >= 0) cursor.getInt(starIdx) == 1 else false
                    val color = AVATAR_PALETTE[abs(displayName.hashCode()) % AVATAR_PALETTE.size]

                    result += ContactEntity(
                        displayName = displayName,
                        normalizedNumber = normalized,
                        displayNumber = PhoneNumberNormalizer.formatForDisplay(normalized),
                        roleOrLabel = roleLabel,
                        avatarColorHex = color,
                        avatarUri = thumbUri,
                        isWhitelisted = true,
                        isFavorite = isStarred,
                        notes = "device_contact"
                    )
                }
            }
        }
        return result
    }

    fun readDeviceCallLogs(
        context: Context,
        intelList: List<PhoneIntelEntity>,
        contacts: List<ContactEntity>,
        limit: Int = 150
    ): List<CallLogEntity> {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return emptyList()

        val result = mutableListOf<CallLogEntity>()
        runCatching {
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.PHONE_ACCOUNT_ID
            )
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
                val accIdx = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

                while (cursor.moveToNext() && result.size < limit) {
                    val rawNumber = if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else ""
                    val isPrivate = PhoneNumberNormalizer.isHiddenOrPrivate(rawNumber)
                    val normalized = if (isPrivate) "PRIVATE" else PhoneNumberNormalizer.normalize(rawNumber)
                    if (normalized.isEmpty()) continue

                    val cachedName = if (nameIdx >= 0) cursor.getString(nameIdx)?.trim() else null
                    val callType = if (typeIdx >= 0) cursor.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                    val timestamp = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    val durationSec = if (durIdx >= 0) cursor.getLong(durIdx) else 0L
                    val phoneAccountId = if (accIdx >= 0) cursor.getString(accIdx).orEmpty() else ""
                    val simSlot = if (phoneAccountId.contains("2")) 2 else 1

                    val matchedContact = if (!isPrivate) {
                        contacts.firstOrNull { numbersMatch(it.normalizedNumber, normalized) }
                    } else null

                    val matchedIntel = if (!isPrivate) {
                        intelList.firstOrNull { numbersMatch(it.normalizedNumber, normalized) }
                    } else null

                    val disposition = when (callType) {
                        CallLog.Calls.BLOCKED_TYPE,
                        CallLog.Calls.REJECTED_TYPE -> CallDisposition.BLOCKED
                        CallLog.Calls.MISSED_TYPE -> CallDisposition.MISSED
                        else -> CallDisposition.ALLOWED
                    }

                    val displayName = matchedContact?.displayName
                        ?: cachedName?.takeIf { it.isNotEmpty() }
                        ?: matchedIntel?.callerName
                        ?: if (isPrivate) "Private Caller" else PhoneNumberNormalizer.formatForDisplay(normalized)

                    val verification = when {
                        matchedContact != null -> CarrierVerificationStatus.VERIFIED
                        matchedIntel != null -> runCatching {
                            CarrierVerificationStatus.valueOf(matchedIntel.verificationDefault)
                        }.getOrDefault(CarrierVerificationStatus.UNVERIFIED)
                        else -> CarrierVerificationStatus.UNVERIFIED
                    }

                    val riskScore = when {
                        matchedContact?.isWhitelisted == true || matchedIntel?.isWhitelisted == true -> 0
                        matchedIntel != null -> matchedIntel.riskScore
                        disposition == CallDisposition.BLOCKED -> 85
                        isPrivate -> 50
                        else -> 12
                    }

                    val category = when {
                        matchedContact != null -> ThreatCategory.PERSONAL.name
                        matchedIntel != null -> matchedIntel.category
                        else -> ThreatCategory.UNKNOWN.name
                    }

                    val durationLabel = formatDuration(durationSec)
                    val directionPrefix = when (callType) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed call"
                        CallLog.Calls.REJECTED_TYPE -> "Declined"
                        CallLog.Calls.BLOCKED_TYPE -> "Blocked"
                        CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
                        else -> "Call"
                    }

                    val reason = when {
                        matchedIntel != null && riskScore >= 70 -> "${matchedIntel.callerName} · $directionPrefix"
                        durationSec > 0 -> "$directionPrefix · $durationLabel"
                        else -> directionPrefix
                    }

                    result += CallLogEntity(
                        normalizedNumber = normalized,
                        displayNumber = if (isPrivate) "Private / Hidden" else PhoneNumberNormalizer.formatForDisplay(normalized),
                        callerName = displayName,
                        disposition = disposition.name,
                        riskScore = riskScore,
                        category = category,
                        lineType = matchedIntel?.lineType ?: LineType.MOBILE.name,
                        carrier = matchedIntel?.carrier ?: if (matchedContact != null) "Saved Contact" else "Phone Network",
                        verificationStatus = verification.name,
                        matchedRule = "Phone Call Log",
                        plainReason = reason,
                        responseFlagsSummary = "device_call_log_type=$callType",
                        screeningLatencyMs = 0L,
                        simSlot = simSlot,
                        timestampMillis = timestamp
                    )
                }
            }
        }
        return result
    }

    fun readDeviceSmsInbox(
        context: Context,
        limit: Int = 80
    ): List<SmsMessageEntity> {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return emptyList()

        val result = mutableListOf<SmsMessageEntity>()
        runCatching {
            val projection = arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val addrIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)

                while (cursor.moveToNext() && result.size < limit) {
                    val sender = (if (addrIdx >= 0) cursor.getString(addrIdx) else null)?.trim().orEmpty()
                    val body = (if (bodyIdx >= 0) cursor.getString(bodyIdx) else null)?.trim().orEmpty()
                    if (body.isEmpty()) continue
                    val timestamp = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    val (cat, analysis) = ScamUrlAnalyzer.categorizeSms(sender, body)
                    result += SmsMessageEntity(
                        sender = sender.ifBlank { "Unknown" },
                        body = body,
                        category = cat.name,
                        riskScore = analysis.riskScore,
                        extractedOtp = analysis.extractedOtp,
                        extractedUrl = analysis.extractedUrls.firstOrNull(),
                        riskSummary = analysis.severity,
                        timestampMillis = timestamp
                    )
                }
            }
        }
        return result
    }

    private fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return when {
            mins > 0 -> "${mins}m ${secs}s"
            else -> "${secs}s"
        }
    }
}
