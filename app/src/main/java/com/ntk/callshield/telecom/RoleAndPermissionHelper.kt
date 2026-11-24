package com.ntk.callshield.telecom

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat

data class TelecomPermissionStatus(
    val hasCallScreeningRole: Boolean,
    val isCallScreeningRoleAvailable: Boolean,
    val hasOverlayPermission: Boolean,
    val hasNotificationsPermission: Boolean,
    val hasContactsPermission: Boolean,
    val hasCallLogPermission: Boolean,
    val hasSmsPermission: Boolean,
    val hasPhoneStatePermission: Boolean
) {
    val readinessScorePercent: Int
        get() {
            var score = 20 // On-device offline shield always works
            if (hasCallScreeningRole) score += 30
            if (hasContactsPermission) score += 15
            if (hasCallLogPermission) score += 15
            if (hasOverlayPermission) score += 10
            if (hasNotificationsPermission) score += 10
            return score.coerceAtMost(100)
        }
}

object RoleAndPermissionHelper {

    fun inspectStatus(context: Context): TelecomPermissionStatus {
        val roleManager = context.getSystemService(RoleManager::class.java)
        val roleAvailable = roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
        val roleHeld = roleAvailable && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

        return TelecomPermissionStatus(
            hasCallScreeningRole = roleHeld,
            isCallScreeningRoleAvailable = roleAvailable,
            hasOverlayPermission = Settings.canDrawOverlays(context),
            hasNotificationsPermission = hasNotificationPermission(context),
            hasContactsPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED,
            hasCallLogPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED,
            hasSmsPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED,
            hasPhoneStatePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun createCallScreeningRoleIntent(context: Context): Intent? {
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
        return if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        } else {
            null
        }
    }

    fun createOverlaySettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    /**
     * Reads on-device system contacts strictly locally (never uploaded) so saved contacts
     * are never erroneously blocked even if Android triggers onScreenCall() for them.
     */
    fun lookupSystemContactName(context: Context, rawPhoneNumber: String?): String? {
        if (rawPhoneNumber.isNullOrBlank()) return null
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null

        return runCatching {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(rawPhoneNumber)
            )
            context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }
}
