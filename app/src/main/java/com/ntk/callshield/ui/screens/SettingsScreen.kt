package com.ntk.callshield.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntk.callshield.domain.BlockAction
import com.ntk.callshield.domain.RuleType
import com.ntk.callshield.domain.ThemeMode
import com.ntk.callshield.ui.components.InfoExplainChip
import com.ntk.callshield.ui.components.PremiumCard
import com.ntk.callshield.ui.components.StatusPillBadge
import com.ntk.callshield.ui.components.callShieldFilterChipColors
import com.ntk.callshield.ui.components.callShieldSegmentedColors
import com.ntk.callshield.ui.theme.LocalShieldColors
import com.ntk.callshield.ui.theme.MonoFontFamily
import com.ntk.callshield.viewmodel.CallShieldViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: CallShieldViewModel,
    onOpenSetupWizard: () -> Unit,
    onOpenOfflineDb: () -> Unit,
    onOpenSafeBlockLists: () -> Unit,
    onOpenPrivacyCenter: () -> Unit,
    onOpenOverlayCustomizer: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenTestLab: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val rules by viewModel.blockingRules.collectAsStateWithLifecycle()
    val regions by viewModel.offlineRegions.collectAsStateWithLifecycle()
    val permStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current

    var advancedRulesExpanded by remember { mutableStateOf(false) }
    var showAddRuleSheet by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Settings", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Ad-Free · Everything Unlocked",
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.verified,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Section 1: Essentials & Quick Preferences
            item { Text("Quick Preferences", style = MaterialTheme.typography.titleMedium) }

            item {
                PremiumCard(cornerRadius = 20.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(semantic.verifiedContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = semantic.verified, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Appearance Theme", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Follows your phone's theme by default, or pick Light/Dark.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            modes.forEachIndexed { index, mode ->
                                val isSelected = if (mode == ThemeMode.DARK) {
                                    settings.themeMode == ThemeMode.DARK || settings.themeMode == ThemeMode.AMOLED
                                } else {
                                    settings.themeMode == mode
                                }
                                SegmentedButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                                    colors = callShieldSegmentedColors(),
                                    modifier = Modifier.testTag("theme_chip_${mode.name.lowercase()}")
                                ) {
                                    Text(
                                        text = mode.label,
                                        maxLines = 1,
                                        softWrap = false,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        HorizontalDivider(thickness = 0.75.dp, color = semantic.cardBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("True Black (AMOLED)", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Pure black surfaces in dark mode",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.themeMode == ThemeMode.AMOLED,
                                onCheckedChange = { enabled ->
                                    viewModel.setThemeMode(if (enabled) ThemeMode.AMOLED else ThemeMode.DARK)
                                },
                                modifier = Modifier.testTag("theme_chip_amoled")
                            )
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            HorizontalDivider(thickness = 0.75.dp, color = semantic.cardBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Match Phone Colors", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Inherit your device's wallpaper accent colors",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.useDynamicColor,
                                    onCheckedChange = viewModel::setUseDynamicColor,
                                    modifier = Modifier.testTag("dynamic_color_switch")
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingsRowCard(
                    icon = Icons.Default.Layers,
                    title = "Caller ID Popup Style",
                    subtitle = "${settings.overlayStyle.label} · Position ${(settings.overlayVerticalPosition * 100).toInt()}%",
                    accent = semantic.caution,
                    badge = null,
                    onClick = onOpenOverlayCustomizer,
                    tag = "settings_overlay_customizer"
                )
            }

            item {
                PremiumCard(cornerRadius = 18.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(semantic.dangerContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = semantic.danger, modifier = Modifier.size(18.dp))
                            }
                            Text("App Lock", style = MaterialTheme.typography.titleSmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("4-Digit PIN Protection", style = MaterialTheme.typography.bodyMedium)
                                Text("Lock app with PIN (default 1234)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.pinLockEnabled,
                                onCheckedChange = { viewModel.togglePinLock(it) },
                                modifier = Modifier.testTag("pin_lock_switch")
                            )
                        }
                        HorizontalDivider(thickness = 0.75.dp, color = semantic.cardBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = semantic.verified, modifier = Modifier.size(18.dp))
                                Text("Quick Biometric Unlock", style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(checked = settings.biometricAllowed, onCheckedChange = viewModel::setBiometricAllowed)
                        }
                        HorizontalDivider(thickness = 0.75.dp, color = semantic.cardBorder)
                        TextButton(onClick = { showChangePinDialog = true }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Text("Change 4-Digit PIN")
                        }
                    }
                }
            }

            // Section 2: Privacy & Offline Shield
            item { Text("Privacy & Offline Shield", style = MaterialTheme.typography.titleMedium) }

            item {
                PremiumCard(
                    cornerRadius = 18.dp,
                    containerColor = if (settings.zeroTelemetryMode) semantic.verifiedContainer else MaterialTheme.colorScheme.surfaceContainer,
                    borderColor = if (settings.zeroTelemetryMode) semantic.verified.copy(alpha = 0.6f) else semantic.cardBorder
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = semantic.verified)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Private Offline-Only Mode", style = MaterialTheme.typography.titleSmall)
                                    InfoExplainChip(
                                        title = "Private Offline-Only Mode",
                                        explanation = "When turned on, every call check happens strictly on your phone using your offline scam list. Zero phone numbers or lookups leave your device."
                                    )
                                }
                                Text(
                                    if (settings.zeroTelemetryMode) "Zero internet traffic · Contacts stay on phone"
                                    else "Smart Cloud + Offline checks enabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = settings.zeroTelemetryMode,
                            onCheckedChange = viewModel::setZeroTelemetryMode,
                            modifier = Modifier.testTag("private_offline_switch")
                        )
                    }
                }
            }

            item {
                SettingsRowCard(
                    icon = Icons.Default.Storage,
                    title = "Works Without Internet (Offline Lists)",
                    subtitle = "${regions.count { it.isDownloaded }}/${regions.size} regions ready · Global & local spam lists",
                    accent = semantic.verified,
                    badge = "Offline DB",
                    onClick = onOpenOfflineDb,
                    tag = "settings_offline_db"
                )
            }

            item {
                SettingsRowCard(
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    title = "Safe List & Block List",
                    subtitle = "Trusted callers ring through; blocked numbers stay silent",
                    accent = semantic.info,
                    badge = null,
                    onClick = onOpenSafeBlockLists,
                    tag = "settings_safe_block_lists"
                )
            }

            item {
                SettingsRowCard(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Center & Unlist My Number",
                    subtitle = "Contacts never uploaded · Activity log & directory opt-out",
                    accent = semantic.verified,
                    badge = "Private",
                    onClick = onOpenPrivacyCenter,
                    tag = "settings_privacy_center"
                )
            }

            item {
                SettingsRowCard(
                    icon = Icons.Default.Security,
                    title = "Phone Permissions & Setup Wizard",
                    subtitle = "Caller ID role, Floating Card & Alerts (${permStatus.readinessScorePercent}% ready)",
                    accent = semantic.caution,
                    badge = if (permStatus.hasCallScreeningRole) "Ready" else "Setup",
                    onClick = onOpenSetupWizard,
                    tag = "settings_setup_wizard"
                )
            }

            // Section 3: Expandable Advanced Rules Drawer
            item {
                PremiumCard(
                    cornerRadius = 20.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { advancedRulesExpanded = !advancedRulesExpanded }
                                .testTag("advanced_rules_toggle"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(semantic.cautionContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Rule, contentDescription = null, tint = semantic.caution, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("Advanced Rules", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${rules.count { it.enabled }} active custom rules · Repeat caller bypass",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (advancedRulesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Advanced Rules"
                            )
                        }

                        AnimatedVisibility(visible = advancedRulesExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HorizontalDivider(thickness = 0.75.dp, color = semantic.cardBorder)
                                // Private / Hidden toggle with (?)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                        Text("Block Private / Hidden Numbers", style = MaterialTheme.typography.bodyMedium)
                                        InfoExplainChip(
                                            title = "Private / Hidden Numbers",
                                            explanation = "Stops callers who hide their phone number ('Unknown' or 'Private'). You can turn this off if you expect calls from hospitals or clinics that withhold caller ID."
                                        )
                                    }
                                    Switch(checked = settings.blockHiddenNumbers, onCheckedChange = viewModel::setBlockHiddenNumbers)
                                }

                                // Unverified VoIP toggle with (?)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                        Text("Silence Unverified Internet (VoIP) Calls", style = MaterialTheme.typography.bodyMedium)
                                        InfoExplainChip(
                                            title = "Unverified Internet Calls",
                                            explanation = "Many automated scam dialers place cheap internet (VoIP) calls without carrier verification. Turning this on silences those calls so you can review them calmly."
                                        )
                                    }
                                    Switch(checked = settings.blockUnverifiedVoip, onCheckedChange = viewModel::setBlockUnverifiedVoip)
                                }

                                // Emergency Repeat Caller Bypass with (?)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Repeat, contentDescription = null, tint = semantic.verified, modifier = Modifier.size(16.dp))
                                        Text("Emergency Repeat Caller Bypass", style = MaterialTheme.typography.bodyMedium)
                                        InfoExplainChip(
                                            title = "Emergency Repeat Caller Bypass",
                                            explanation = "If an unknown person calls twice within ${settings.repeatedCallerWindowMinutes} minutes, NTK CallShield lets the second call ring through automatically. This helps urgent callbacks reach you."
                                        )
                                    }
                                    Switch(
                                        checked = settings.repeatedCallerBypassEnabled,
                                        onCheckedChange = { viewModel.setRepeatedCallerBypass(it) }
                                    )
                                }

                                if (settings.repeatedCallerBypassEnabled) {
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Repeat window", style = MaterialTheme.typography.labelMedium)
                                            Text("${settings.repeatedCallerWindowMinutes} minutes", style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily), color = semantic.verified)
                                        }
                                        Slider(
                                            value = settings.repeatedCallerWindowMinutes.toFloat(),
                                            onValueChange = { viewModel.setRepeatedCallerBypass(true, it.roundToInt()) },
                                            valueRange = 3f..15f,
                                            steps = 11
                                        )
                                    }
                                }

                                Text("Default Action When Blocking", style = MaterialTheme.typography.labelLarge)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BlockAction.entries.forEach { action ->
                                        FilterChip(
                                            selected = settings.defaultBlockAction == action,
                                            onClick = { viewModel.setDefaultBlockAction(action) },
                                            label = { Text(action.label, maxLines = 1, softWrap = false) },
                                            colors = callShieldFilterChipColors(),
                                            shape = RoundedCornerShape(999.dp)
                                        )
                                    }
                                }

                                Text("Custom Country, Area & Prefix Rules", style = MaterialTheme.typography.labelLarge)
                                rules.forEach { rule ->
                                    val act = runCatching { BlockAction.valueOf(rule.action) }.getOrDefault(BlockAction.BLOCK_SILENTLY)
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(rule.label, style = MaterialTheme.typography.titleSmall)
                                                Text("${rule.pattern} · ${act.label}", style = MaterialTheme.typography.bodySmall, color = semantic.verified)
                                            }
                                            Switch(checked = rule.enabled, onCheckedChange = { viewModel.toggleBlockingRule(rule) })
                                            IconButton(onClick = { viewModel.deleteBlockingRuleWithUndo(rule) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete rule", tint = semantic.danger)
                                            }
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { showAddRuleSheet = true },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_add_rule_sheet")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add Country / Prefix Rule")
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Community Guardian & Test Lab
            item { Text("Community & Testing", style = MaterialTheme.typography.titleMedium) }

            item {
                SettingsRowCard(
                    icon = Icons.Default.EmojiEvents,
                    title = "Guardian Badges & Missions",
                    subtitle = "${settings.guardianPoints} points · Earn badges for stopping scams",
                    accent = semantic.caution,
                    badge = "${settings.guardianPoints} pts",
                    onClick = onOpenMissions,
                    tag = "settings_missions"
                )
            }

            item {
                SettingsRowCard(
                    icon = Icons.Default.Science,
                    title = "Test My Shield (Live Simulator)",
                    subtitle = "Try 6 safe, scam, spoofed, and phishing text scenarios",
                    accent = semantic.verified,
                    badge = "Simulator",
                    onClick = onOpenTestLab,
                    tag = "settings_test_lab"
                )
            }
        }
    }

    if (showAddRuleSheet) {
        AddRuleBottomSheet(
            onSave = { label, type, pattern, action ->
                viewModel.addBlockingRule(label, type, pattern, action)
                showAddRuleSheet = false
            },
            onDismiss = { showAddRuleSheet = false }
        )
    }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change 4-Digit PIN") },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) newPin = it },
                    label = { Text("Enter 4 digits") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.togglePinLock(enabled = true, customPin = newPin)
                        newPin = ""
                        showChangePinDialog = false
                    },
                    enabled = newPin.length == 4
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddRuleBottomSheet(
    onSave: (label: String, type: RuleType, pattern: String, action: BlockAction) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("+234-700-*") }
    var selectedType by remember { mutableStateOf(RuleType.PREFIX_PATTERN) }
    var selectedAction by remember { mutableStateOf(BlockAction.BLOCK_SILENTLY) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Create Custom Rule", style = MaterialTheme.typography.headlineSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RuleType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            pattern = when (type) {
                                RuleType.COUNTRY_CODE -> "+224"
                                RuleType.AREA_CODE -> "888"
                                RuleType.PREFIX_PATTERN -> "+234-700-*"
                                RuleType.HIDDEN_PRIVATE -> "PRIVATE"
                                RuleType.UNVERIFIED_VOIP -> "VOIP"
                            }
                        },
                        label = { Text(type.label, maxLines = 1, softWrap = false) },
                        colors = callShieldFilterChipColors(),
                        shape = RoundedCornerShape(999.dp)
                    )
                }
            }
            OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Rule Name") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            if (selectedType != RuleType.HIDDEN_PRIVATE && selectedType != RuleType.UNVERIFIED_VOIP) {
                OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Code or Pattern (e.g. +234-700-*, +1-800-*)") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            }
            Text("Action", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BlockAction.entries.forEach { act ->
                    FilterChip(
                        selected = selectedAction == act,
                        onClick = { selectedAction = act },
                        label = { Text(act.label, maxLines = 1, softWrap = false) },
                        colors = callShieldFilterChipColors(),
                        shape = RoundedCornerShape(999.dp)
                    )
                }
            }
            Button(
                onClick = { onSave(label, selectedType, pattern, selectedAction) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Save Rule")
            }
        }
    }
}

@Composable
private fun SettingsRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    badge: String?,
    onClick: () -> Unit,
    tag: String
) {
    PremiumCard(
        cornerRadius = 18.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (badge != null) {
                StatusPillBadge(text = badge, accentColor = accent, containerColor = accent.copy(alpha = 0.12f))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
