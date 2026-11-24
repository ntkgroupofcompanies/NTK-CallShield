package com.ntk.callshield.ui.screens

import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntk.callshield.domain.ProtectionPreset
import com.ntk.callshield.ui.components.NtkCallShieldLogo
import com.ntk.callshield.ui.components.PremiumCard
import com.ntk.callshield.ui.components.SevenDaySafetyChart
import com.ntk.callshield.ui.components.StatusPillBadge
import com.ntk.callshield.ui.theme.LocalShieldColors
import com.ntk.callshield.ui.theme.LuminousMint
import com.ntk.callshield.ui.theme.MonoFontFamily
import com.ntk.callshield.ui.theme.ObsidianHeroAmber
import com.ntk.callshield.ui.theme.ObsidianHeroMid
import com.ntk.callshield.ui.theme.ObsidianHeroTeal
import com.ntk.callshield.ui.theme.ObsidianHeroTop
import com.ntk.callshield.ui.theme.TrustTeal
import com.ntk.callshield.viewmodel.CallFilter
import com.ntk.callshield.viewmodel.CallShieldViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CallShieldViewModel,
    onCheckNumberClick: () -> Unit,
    onScanLinkClick: () -> Unit,
    onTestShieldClick: () -> Unit,
    onFinishSetupClick: () -> Unit,
    onOpenCallsTab: (CallFilter) -> Unit = {}
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val summary by viewModel.weeklySummary.collectAsStateWithLifecycle()
    val permStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current
    val haptics = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NtkCallShieldLogo(size = 34.dp, showBadge = true)
                    Column {
                        Text("NTK CallShield", style = MaterialTheme.typography.titleLarge, maxLines = 1)
                        Text(
                            "Know before you answer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            actions = {
                Surface(
                    color = semantic.verifiedContainer,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(0.75.dp, semantic.verified.copy(alpha = 0.28f)),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(semantic.verified)
                        )
                        Text(
                            text = if (settings.zeroTelemetryMode) "Offline" else "Cloud",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = semantic.verified,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize().testTag("home_list")
        ) {
            // 1. Signature Obsidian-Teal Gradient Hero Card
            item {
                val active = settings.masterProtectionEnabled
                val setupIncomplete = active && !permStatus.hasCallScreeningRole
                val heroEndColor = if (active) ObsidianHeroTeal else ObsidianHeroAmber
                val ringColor = if (!active || setupIncomplete) Color(0xFFF59E0B) else LuminousMint

                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 2.dp else 6.dp),
                    border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ObsidianHeroTop, ObsidianHeroMid, heroEndColor)
                                )
                            )
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        (if (active) LuminousMint else Color(0xFFF59E0B)).copy(alpha = 0.22f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.92f, size.height * 0.08f),
                                    radius = size.maxDimension * 0.7f
                                ),
                                center = Offset(size.width * 0.92f, size.height * 0.08f),
                                radius = size.maxDimension * 0.7f
                            )
                        }

                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                ShieldLogoHalo(active = active, ringColor = ringColor)

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = if (active) "NTK CallShield is Active" else "Protection is Paused",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = when {
                                            !active -> "Tap below to turn protection back on."
                                            setupIncomplete -> "Finish setup to enable live call blocking."
                                            else -> "${settings.protectionPreset.title} mode · Quietly protecting calls."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 2
                                    )
                                }
                            }

                            // Mini at-a-glance stats strip inside Hero (clickable to jump straight to Calls / Scanner)
                            Surface(
                                color = Color(0x14FFFFFF),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HeroMiniStat(
                                        value = "${summary.callsChecked}",
                                        label = "Checked",
                                        accent = LuminousMint,
                                        modifier = Modifier.weight(1f).clickable { onOpenCallsTab(CallFilter.ALL) }
                                    )
                                    Box(modifier = Modifier.width(1.dp).height(26.dp).background(Color(0x24FFFFFF)))
                                    HeroMiniStat(
                                        value = "${summary.scamsStopped}",
                                        label = "Blocked",
                                        accent = Color(0xFFFCA5A5),
                                        modifier = Modifier.weight(1f).clickable { onOpenCallsTab(CallFilter.BLOCKED) }
                                    )
                                    Box(modifier = Modifier.width(1.dp).height(26.dp).background(Color(0x24FFFFFF)))
                                    HeroMiniStat(
                                        value = "${summary.badLinksCaught}",
                                        label = "Links",
                                        accent = Color(0xFFFCD34D),
                                        modifier = Modifier.weight(1f).clickable(onClick = onScanLinkClick)
                                    )
                                }
                            }

                            if (setupIncomplete) {
                                Surface(
                                    color = Color(0x2AF59E0B),
                                    border = BorderStroke(1.dp, Color(0x88F59E0B)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onFinishSetupClick)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Finish Setup · Live Blocking (${permStatus.readinessScorePercent}%)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color(0xFFFDE68A),
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color(0xFFFDE68A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Frosted Master Protection toggle row
                            Surface(
                                color = Color(0x16FFFFFF),
                                border = BorderStroke(0.75.dp, Color(0x26FFFFFF)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Master Protection",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (active) "${settings.protectionPreset.title} protection on" else "Paused by you",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Switch(
                                        checked = active,
                                        onCheckedChange = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.setMasterProtectionEnabled(it)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = TrustTeal,
                                            uncheckedThumbColor = Color(0xFFCBD5E1),
                                            uncheckedTrackColor = Color(0x33FFFFFF),
                                            uncheckedBorderColor = Color(0x44FFFFFF)
                                        ),
                                        modifier = Modifier.testTag("master_protection_switch")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Instant Search Shortcut Bar (1-tap clarity)
            item {
                PremiumCard(
                    cornerRadius = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCheckNumberClick)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = semantic.verified,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Look up any phone number...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        StatusPillBadge(
                            text = "Check",
                            accentColor = semantic.verified,
                            containerColor = semantic.verifiedContainer
                        )
                    }
                }
            }

            // 3. 1-Tap Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeQuickActionCard(
                        icon = Icons.Default.PhoneInTalk,
                        label = "Check\nNumber",
                        accent = semantic.verified,
                        onClick = onCheckNumberClick,
                        tag = "quick_check_number",
                        modifier = Modifier.weight(1f)
                    )
                    HomeQuickActionCard(
                        icon = Icons.Default.Link,
                        label = "Scan\nLink/SMS",
                        accent = semantic.info,
                        onClick = onScanLinkClick,
                        tag = "quick_scan_link",
                        modifier = Modifier.weight(1f)
                    )
                    HomeQuickActionCard(
                        icon = Icons.Default.Science,
                        label = "Test My\nShield",
                        accent = semantic.caution,
                        onClick = onTestShieldClick,
                        tag = "quick_test_shield",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Protection Presets Card
            item {
                PremiumCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Protection Level", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Choose how NTK CallShield handles unknown callers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ProtectionPreset.entries.forEach { preset ->
                            val selected = settings.protectionPreset == preset
                            Surface(
                                color = if (selected) {
                                    semantic.verifiedContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                shape = RoundedCornerShape(15.dp),
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 0.75.dp,
                                    color = if (selected) semantic.verified else semantic.cardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.setProtectionPreset(preset)
                                    }
                                    .testTag("home_preset_${preset.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(preset.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = semantic.verified
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Weekly Safety Summary + 7-Day Chart
            item {
                PremiumCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Weekly Safety Summary", style = MaterialTheme.typography.titleLarge)
                            StatusPillBadge(
                                text = "${summary.timeSavedMinutes}m saved",
                                accentColor = semantic.verified,
                                containerColor = semantic.verifiedContainer
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SafetyStatPill("Calls", "${summary.callsChecked}", semantic.info, Modifier.weight(1f))
                            SafetyStatPill("Stopped", "${summary.scamsStopped}", semantic.danger, Modifier.weight(1f))
                            SafetyStatPill("Links", "${summary.badLinksCaught}", semantic.caution, Modifier.weight(1f))
                        }

                        SevenDaySafetyChart(metrics = summary.dailyBreakdown)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShieldLogoHalo(
    active: Boolean,
    ringColor: Color
) {
    val context = LocalContext.current
    val pulse = remember { Animatable(0f) }

    LaunchedEffect(active) {
        if (!active) {
            pulse.snapTo(0f)
            return@LaunchedEffect
        }
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSave = powerManager?.isPowerSaveMode == true
        val animScale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)

        if (isPowerSave || animScale == 0f) {
            pulse.snapTo(1f)
        } else {
            pulse.snapTo(0f)
            pulse.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier.size(68.dp),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseRadius = size.minDimension * 0.36f
                val maxExpand = size.minDimension * 0.13f
                val progress = pulse.value
                if (progress in 0.01f..0.99f) {
                    drawCircle(
                        color = ringColor.copy(alpha = (1f - progress) * 0.50f),
                        radius = baseRadius + maxExpand * progress,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                drawCircle(
                    color = ringColor.copy(alpha = 0.28f),
                    radius = baseRadius + 2.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = ringColor.copy(alpha = 0.12f),
                    radius = baseRadius + 6.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
        NtkCallShieldLogo(size = 46.dp, showBadge = true)
    }
}

@Composable
private fun HeroMiniStat(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = accent,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFCBD5E1),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun HomeQuickActionCard(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    PremiumCard(
        cornerRadius = 18.dp,
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun SafetyStatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
