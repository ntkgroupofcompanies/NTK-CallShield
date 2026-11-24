package com.ntk.callshield.telecom

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntk.callshield.NtkCallShieldApplication
import com.ntk.callshield.domain.PhoneNumberNormalizer
import com.ntk.callshield.domain.ThreatCategory
import com.ntk.callshield.ui.components.NtkCallShieldLogo
import com.ntk.callshield.ui.components.StatusPillBadge
import com.ntk.callshield.ui.components.riskAccentColor
import com.ntk.callshield.ui.components.riskContainerColor
import com.ntk.callshield.ui.theme.LocalShieldColors
import com.ntk.callshield.ui.theme.MonoFontFamily
import com.ntk.callshield.ui.theme.NtkCallShieldTheme
import kotlinx.coroutines.launch

/**
 * Post-Call Summary Activity handling android.telecom.action.POST_CALL and direct summary launches.
 * Provides 1-tap Save, Block, or Report Scam actions without intrusive dialer replacement.
 */
class AfterCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val number = intent.getStringExtra(EXTRA_NUMBER)
            ?: intent.getStringExtra("android.telecom.extra.HANDLE")
            ?: "+18005550199"
        val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: PhoneNumberNormalizer.formatForDisplay(number)
        val riskScore = intent.getIntExtra(EXTRA_RISK_SCORE, 25)
        val disposition = intent.getStringExtra(EXTRA_DISPOSITION) ?: "Completed Call"
        val plainReason = intent.getStringExtra(EXTRA_PLAIN_REASON) ?: "Tap below to save, block, or report this caller."
        val verificationLabel = intent.getStringExtra(EXTRA_VERIFICATION) ?: "Unverified Caller"
        val lineAndCarrier = intent.getStringExtra(EXTRA_LINE_CARRIER) ?: "Carrier lookup available"

        val app = application as NtkCallShieldApplication

        setContent {
            val settings by app.preferences.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.ntk.callshield.data.prefs.UserSettings()
            )
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val useDark = when (settings.themeMode) {
                com.ntk.callshield.domain.ThemeMode.LIGHT -> false
                com.ntk.callshield.domain.ThemeMode.DARK, com.ntk.callshield.domain.ThemeMode.AMOLED -> true
                com.ntk.callshield.domain.ThemeMode.SYSTEM -> systemDark
            }
            val view = androidx.compose.ui.platform.LocalView.current
            if (!view.isInEditMode) {
                androidx.compose.runtime.SideEffect {
                    androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !useDark
                        isAppearanceLightNavigationBars = !useDark
                    }
                }
            }
            val scope = rememberCoroutineScope()

            NtkCallShieldTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.useDynamicColor
            ) {
                val semantic = LocalShieldColors.current
                val accent = riskAccentColor(riskScore)
                val container = riskContainerColor(riskScore)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.6f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        NtkCallShieldLogo(size = 34.dp, showBadge = true)
                                        Column {
                                            Text("NTK CallShield", style = MaterialTheme.typography.titleSmall)
                                            Text("Post-Call Summary", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { finish() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close")
                                    }
                                }

                                StatusPillBadge(text = "$disposition · $verificationLabel", accentColor = accent, containerColor = container)

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(callerName, style = MaterialTheme.typography.headlineSmall)
                                    Text(
                                        PhoneNumberNormalizer.formatForDisplay(number),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(lineAndCarrier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        plainReason,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                app.callerRepository.saveContact(callerName, number, "Saved After Call", true)
                                                finish()
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Save")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                app.callerRepository.reportNumber(number, callerName, ThreatCategory.ROBOCALL, true)
                                                finish()
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Report")
                                    }
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            app.callerRepository.setBlacklistStatus(number, callerName, true)
                                            finish()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = semantic.danger),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Block Number")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_NUMBER = "extra_number"
        private const val EXTRA_CALLER_NAME = "extra_caller_name"
        private const val EXTRA_RISK_SCORE = "extra_risk_score"
        private const val EXTRA_DISPOSITION = "extra_disposition"
        private const val EXTRA_PLAIN_REASON = "extra_plain_reason"
        private const val EXTRA_VERIFICATION = "extra_verification"
        private const val EXTRA_LINE_CARRIER = "extra_line_carrier"

        fun createIntent(
            context: Context,
            number: String,
            callerName: String,
            riskScore: Int,
            disposition: String,
            plainReason: String,
            verificationLabel: String,
            lineAndCarrier: String
        ): Intent = Intent(context, AfterCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NUMBER, number)
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_RISK_SCORE, riskScore)
            putExtra(EXTRA_DISPOSITION, disposition)
            putExtra(EXTRA_PLAIN_REASON, plainReason)
            putExtra(EXTRA_VERIFICATION, verificationLabel)
            putExtra(EXTRA_LINE_CARRIER, lineAndCarrier)
        }
    }
}
