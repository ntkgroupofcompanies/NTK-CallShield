package com.ntk.callshield.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ntk.callshield.domain.BlockAction
import com.ntk.callshield.domain.MyCallerCard
import com.ntk.callshield.domain.OverlayStyle
import com.ntk.callshield.domain.ProtectionPreset
import com.ntk.callshield.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.callShieldDataStore: DataStore<Preferences> by preferencesDataStore(name = "ntk_callshield_prefs")

data class UserSettings(
    val masterProtectionEnabled: Boolean = true,
    val protectionPreset: ProtectionPreset = ProtectionPreset.BALANCED,
    val zeroTelemetryMode: Boolean = true,
    val autoUpdateWifiOnly: Boolean = true,
    val blockHiddenNumbers: Boolean = true,
    val blockUnverifiedVoip: Boolean = false,
    val repeatedCallerBypassEnabled: Boolean = true,
    val repeatedCallerWindowMinutes: Int = 5,
    val defaultBlockAction: BlockAction = BlockAction.BLOCK_SILENTLY,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val overlayStyle: OverlayStyle = OverlayStyle.RICH_CARD,
    val overlayVerticalPosition: Float = 0.22f,
    val pinLockEnabled: Boolean = false,
    val pinHash: String = hashPin("1234"),
    val biometricAllowed: Boolean = true,
    val myCallerCard: MyCallerCard = MyCallerCard(),
    val unlistedNumber: String = "",
    val unlistedSubmitted: Boolean = false,
    val guardianPoints: Int = 250,
    val urlsNeutralizedCount: Int = 6,
    val onboardingCompleted: Boolean = false
) {
    companion object {
        fun hashPin(pin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest("ntk_callshield_pin_v1:$pin".toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}

class CallShieldPreferences(context: Context) {
    private val dataStore = context.applicationContext.callShieldDataStore

    private object Keys {
        val MASTER_PROTECTION = booleanPreferencesKey("master_protection")
        val PROTECTION_PRESET = stringPreferencesKey("protection_preset")
        val ZERO_TELEMETRY = booleanPreferencesKey("zero_telemetry")
        val AUTO_UPDATE_WIFI = booleanPreferencesKey("auto_update_wifi")
        val BLOCK_HIDDEN = booleanPreferencesKey("block_hidden")
        val BLOCK_VOIP = booleanPreferencesKey("block_voip")
        val REPEAT_BYPASS = booleanPreferencesKey("repeat_bypass")
        val REPEAT_WINDOW_MIN = intPreferencesKey("repeat_window_min")
        val DEFAULT_ACTION = stringPreferencesKey("default_action")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val OVERLAY_STYLE = stringPreferencesKey("overlay_style")
        val OVERLAY_Y_POS = floatPreferencesKey("overlay_y_pos")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ALLOWED = booleanPreferencesKey("biometric_allowed")
        val CARD_NAME = stringPreferencesKey("card_name")
        val CARD_ROLE = stringPreferencesKey("card_role")
        val CARD_PHONE = stringPreferencesKey("card_phone")
        val CARD_NOTE = stringPreferencesKey("card_note")
        val CARD_AVATAR_URI = stringPreferencesKey("card_avatar_uri")
        val CARD_VERIFIED = booleanPreferencesKey("card_verified")
        val UNLISTED_NUMBER = stringPreferencesKey("unlisted_number")
        val UNLISTED_SUBMITTED = booleanPreferencesKey("unlisted_submitted")
        val GUARDIAN_POINTS = intPreferencesKey("guardian_points")
        val URLS_NEUTRALIZED = intPreferencesKey("urls_neutralized")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val settingsFlow: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            masterProtectionEnabled = prefs[Keys.MASTER_PROTECTION] ?: true,
            protectionPreset = prefs[Keys.PROTECTION_PRESET]?.let { runCatching { ProtectionPreset.valueOf(it) }.getOrNull() }
                ?: ProtectionPreset.BALANCED,
            zeroTelemetryMode = prefs[Keys.ZERO_TELEMETRY] ?: true,
            autoUpdateWifiOnly = prefs[Keys.AUTO_UPDATE_WIFI] ?: true,
            blockHiddenNumbers = prefs[Keys.BLOCK_HIDDEN] ?: true,
            blockUnverifiedVoip = prefs[Keys.BLOCK_VOIP] ?: false,
            repeatedCallerBypassEnabled = prefs[Keys.REPEAT_BYPASS] ?: true,
            repeatedCallerWindowMinutes = prefs[Keys.REPEAT_WINDOW_MIN] ?: 5,
            defaultBlockAction = prefs[Keys.DEFAULT_ACTION]?.let { runCatching { BlockAction.valueOf(it) }.getOrNull() }
                ?: BlockAction.BLOCK_SILENTLY,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            overlayStyle = prefs[Keys.OVERLAY_STYLE]?.let { runCatching { OverlayStyle.valueOf(it) }.getOrNull() }
                ?: OverlayStyle.RICH_CARD,
            overlayVerticalPosition = prefs[Keys.OVERLAY_Y_POS] ?: 0.22f,
            pinLockEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            pinHash = prefs[Keys.PIN_HASH] ?: UserSettings.hashPin("1234"),
            biometricAllowed = prefs[Keys.BIOMETRIC_ALLOWED] ?: true,
            myCallerCard = MyCallerCard(
                displayName = prefs[Keys.CARD_NAME] ?: "Alex Morgan",
                roleCompany = prefs[Keys.CARD_ROLE] ?: "Product Lead · NTK Shield Studio",
                phoneNumber = prefs[Keys.CARD_PHONE] ?: "+1 (415) 890-4421",
                statusNote = prefs[Keys.CARD_NOTE] ?: "Calling about our scheduled check-in",
                avatarUri = prefs[Keys.CARD_AVATAR_URI]?.ifBlank { null },
                verifiedBadge = prefs[Keys.CARD_VERIFIED] ?: true
            ),
            unlistedNumber = prefs[Keys.UNLISTED_NUMBER] ?: "",
            unlistedSubmitted = prefs[Keys.UNLISTED_SUBMITTED] ?: false,
            guardianPoints = prefs[Keys.GUARDIAN_POINTS] ?: 250,
            urlsNeutralizedCount = prefs[Keys.URLS_NEUTRALIZED] ?: 6,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun getSnapshot(): UserSettings = settingsFlow.first()

    suspend fun setMasterProtectionEnabled(enabled: Boolean) = dataStore.edit {
        it[Keys.MASTER_PROTECTION] = enabled
    }

    suspend fun setProtectionPreset(preset: ProtectionPreset) = dataStore.edit {
        it[Keys.PROTECTION_PRESET] = preset.name
    }

    suspend fun setZeroTelemetryMode(enabled: Boolean) = dataStore.edit {
        it[Keys.ZERO_TELEMETRY] = enabled
    }

    suspend fun setAutoUpdateWifiOnly(enabled: Boolean) = dataStore.edit {
        it[Keys.AUTO_UPDATE_WIFI] = enabled
    }

    suspend fun setBlockHiddenNumbers(enabled: Boolean) = dataStore.edit {
        it[Keys.BLOCK_HIDDEN] = enabled
    }

    suspend fun setBlockUnverifiedVoip(enabled: Boolean) = dataStore.edit {
        it[Keys.BLOCK_VOIP] = enabled
    }

    suspend fun setRepeatedCallerBypass(enabled: Boolean, windowMinutes: Int? = null) = dataStore.edit {
        it[Keys.REPEAT_BYPASS] = enabled
        if (windowMinutes != null) {
            it[Keys.REPEAT_WINDOW_MIN] = windowMinutes.coerceIn(3, 15)
        }
    }

    suspend fun setDefaultBlockAction(action: BlockAction) = dataStore.edit {
        it[Keys.DEFAULT_ACTION] = action.name
    }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit {
        it[Keys.THEME_MODE] = mode.name
    }

    suspend fun setUseDynamicColor(enabled: Boolean) = dataStore.edit {
        it[Keys.DYNAMIC_COLOR] = enabled
    }

    suspend fun setOverlayConfig(style: OverlayStyle? = null, verticalPosition: Float? = null) = dataStore.edit {
        if (style != null) it[Keys.OVERLAY_STYLE] = style.name
        if (verticalPosition != null) it[Keys.OVERLAY_Y_POS] = verticalPosition.coerceIn(0.08f, 0.72f)
    }

    suspend fun setPinLock(enabled: Boolean, newPin: String? = null) = dataStore.edit {
        it[Keys.PIN_ENABLED] = enabled
        if (!newPin.isNullOrBlank() && newPin.length == 4) {
            it[Keys.PIN_HASH] = UserSettings.hashPin(newPin)
        }
    }

    suspend fun setBiometricAllowed(allowed: Boolean) = dataStore.edit {
        it[Keys.BIOMETRIC_ALLOWED] = allowed
    }

    suspend fun updateMyCallerCard(card: MyCallerCard) = dataStore.edit {
        it[Keys.CARD_NAME] = card.displayName
        it[Keys.CARD_ROLE] = card.roleCompany
        it[Keys.CARD_PHONE] = card.phoneNumber
        it[Keys.CARD_NOTE] = card.statusNote
        it[Keys.CARD_AVATAR_URI] = card.avatarUri.orEmpty()
        it[Keys.CARD_VERIFIED] = card.verifiedBadge
    }

    suspend fun setUnlistingRequest(number: String, submitted: Boolean) = dataStore.edit {
        it[Keys.UNLISTED_NUMBER] = number
        it[Keys.UNLISTED_SUBMITTED] = submitted
    }

    suspend fun addGuardianPoints(delta: Int) = dataStore.edit {
        val current = it[Keys.GUARDIAN_POINTS] ?: 250
        it[Keys.GUARDIAN_POINTS] = (current + delta).coerceAtLeast(0)
    }

    suspend fun incrementUrlsNeutralized() = dataStore.edit {
        val current = it[Keys.URLS_NEUTRALIZED] ?: 6
        it[Keys.URLS_NEUTRALIZED] = current + 1
    }

    suspend fun setOnboardingCompleted(completed: Boolean) = dataStore.edit {
        it[Keys.ONBOARDING_COMPLETED] = completed
    }
}
