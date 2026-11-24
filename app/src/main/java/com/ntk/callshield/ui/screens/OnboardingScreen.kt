package com.ntk.callshield.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntk.callshield.domain.ProtectionPreset
import com.ntk.callshield.telecom.RoleAndPermissionHelper
import com.ntk.callshield.ui.components.NtkCallShieldLogo
import com.ntk.callshield.ui.components.PremiumCard
import com.ntk.callshield.ui.components.StatusPillBadge
import com.ntk.callshield.ui.theme.LocalShieldColors
import com.ntk.callshield.ui.theme.LuminousMint
import com.ntk.callshield.ui.theme.TrustTeal
import com.ntk.callshield.viewmodel.CallShieldViewModel

@Composable
fun OnboardingScreen(
    viewModel: CallShieldViewModel,
    canDismiss: Boolean = false,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val permStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current

    var step by remember { mutableIntStateOf(1) }
    var chosenPreset by remember(settings.protectionPreset) { mutableStateOf(settings.protectionPreset) }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissionStatus()
    }
    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissionStatus()
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissionStatus()
    }
    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissionsAndSyncDeviceData()
    }
    val callLogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissionsAndSyncDeviceData()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionStatus()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            // Top step indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    IconButton(onClick = { step-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                } else if (canDismiss) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close setup")
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
                Text(
                    text = "Step $step of 3",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(48.dp))
            }

            LinearProgressIndicator(
                progress = { step / 3f },
                color = TrustTeal,
                trackColor = semantic.cardBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
            )

            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = step,
                label = "onboarding_step",
                modifier = Modifier.weight(1f)
            ) { currentStep ->
                when (currentStep) {
                    1 -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(136.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(LuminousMint.copy(alpha = 0.22f), Color.Transparent)
                                    )
                                )
                            }
                            NtkCallShieldLogo(size = 96.dp, showBadge = true)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "NTK CallShield",
                            style = MaterialTheme.typography.displaySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Know before you answer.",
                            style = MaterialTheme.typography.titleMedium,
                            color = semantic.verified,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        PremiumCard(cornerRadius = 20.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(semantic.verifiedContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = semantic.verified, modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        "We never upload your contacts.",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    "Your family, friends, and personal address book stay strictly on your phone. NTK CallShield quietly stops scam calls and fake links without ads or tracking.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                step = 2
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("onboarding_start_setup_btn")
                        ) {
                            Text("Set Up Protection", style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.exploreDemoFirst() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("onboarding_explore_demo_btn")
                        ) {
                            Text("Explore Demo First")
                        }
                    }

                    2 -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Turn on Phone Protection", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Allow NTK CallShield to check incoming calls and show friendly caller cards before you pick up.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OnboardingPermissionCard(
                            icon = Icons.Default.Shield,
                            title = "Caller ID & Scam Blocking",
                            description = "Lets NTK CallShield spot spam callers and stop known scams before your phone rings.",
                            granted = permStatus.hasCallScreeningRole,
                            buttonLabel = if (permStatus.isCallScreeningRoleAvailable) "Turn On Caller ID" else "Unavailable on Device",
                            buttonEnabled = permStatus.isCallScreeningRoleAvailable && !permStatus.hasCallScreeningRole,
                            onAction = {
                                val intent = RoleAndPermissionHelper.createCallScreeningRoleIntent(context)
                                if (intent != null) roleLauncher.launch(intent)
                            }
                        )

                        OnboardingPermissionCard(
                            icon = Icons.Default.PictureInPictureAlt,
                            title = "Floating Caller ID Card",
                            description = "Shows who is calling right on your screen with Safe, Caution, or Scam colors.",
                            granted = permStatus.hasOverlayPermission,
                            buttonLabel = "Allow Floating Card",
                            buttonEnabled = !permStatus.hasOverlayPermission,
                            onAction = {
                                overlayLauncher.launch(RoleAndPermissionHelper.createOverlaySettingsIntent(context))
                            }
                        )

                        OnboardingPermissionCard(
                            icon = Icons.Default.Contacts,
                            title = "Your Contacts (On-Device)",
                            description = "Shows your real saved contacts and ensures friends & family are never accidentally blocked.",
                            granted = permStatus.hasContactsPermission,
                            buttonLabel = "Allow Contacts Access",
                            buttonEnabled = !permStatus.hasContactsPermission,
                            onAction = {
                                contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        )

                        OnboardingPermissionCard(
                            icon = Icons.Default.History,
                            title = "Your Call History",
                            description = "Loads your real recent calls so tapping any contact number shows all calls with them.",
                            granted = permStatus.hasCallLogPermission,
                            buttonLabel = "Allow Call Log Access",
                            buttonEnabled = !permStatus.hasCallLogPermission,
                            onAction = {
                                callLogLauncher.launch(Manifest.permission.READ_CALL_LOG)
                            }
                        )

                        OnboardingPermissionCard(
                            icon = Icons.Default.Notifications,
                            title = "Safety Alerts",
                            description = "Notifies you if a scam call was stopped or shows a summary after a call ends.",
                            granted = permStatus.hasNotificationsPermission,
                            buttonLabel = "Allow Alerts",
                            buttonEnabled = !permStatus.hasNotificationsPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                            onAction = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )

                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { step = 3 },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("onboarding_step2_continue_btn")
                        ) {
                            Text("Continue")
                        }
                        TextButton(
                            onClick = { step = 3 },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("onboarding_skip_permissions_btn")
                        ) {
                            Text("Skip for now")
                        }
                    }

                    else -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Pick Your Comfort Level", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "You can change this anytime with one tap on the Home screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ProtectionPreset.entries.forEach { preset ->
                            val selected = chosenPreset == preset
                            PremiumCard(
                                cornerRadius = 18.dp,
                                containerColor = if (selected) {
                                    semantic.verifiedContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                                borderColor = if (selected) semantic.verified else semantic.cardBorder,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        chosenPreset = preset
                                    }
                                    .testTag("onboarding_preset_${preset.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(preset.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            if (preset.badge != null) {
                                                StatusPillBadge(preset.badge, semantic.verified, semantic.verifiedContainer)
                                            }
                                        }
                                        Text(
                                            preset.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (selected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = semantic.verified)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.completeOnboarding(chosenPreset)
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("onboarding_finish_btn")
                        ) {
                            Text("Start Protecting My Phone", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    buttonLabel: String,
    buttonEnabled: Boolean,
    onAction: () -> Unit
) {
    val semantic = LocalShieldColors.current
    PremiumCard(
        cornerRadius = 18.dp,
        borderColor = if (granted) semantic.verified else semantic.cardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (granted) semantic.verifiedContainer else MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (granted) Icons.Default.CheckCircle else icon,
                        contentDescription = null,
                        tint = if (granted) semantic.verified else semantic.verified
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (granted) "Ready ✓" else "Optional",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (granted) semantic.verified else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!granted) {
                OutlinedButton(
                    onClick = onAction,
                    enabled = buttonEnabled,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(buttonLabel)
                }
            }
        }
    }
}
