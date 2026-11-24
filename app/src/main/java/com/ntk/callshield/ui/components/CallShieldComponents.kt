package com.ntk.callshield.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntk.callshield.data.local.ContactEntity
import com.ntk.callshield.data.local.PhoneIntelEntity
import com.ntk.callshield.domain.CarrierVerificationStatus
import com.ntk.callshield.domain.DaySafetyMetric
import com.ntk.callshield.domain.OverlayStyle
import com.ntk.callshield.domain.PhoneNumberNormalizer
import com.ntk.callshield.domain.ScreeningDecision
import com.ntk.callshield.domain.ThreatCategory
import com.ntk.callshield.ui.theme.LocalShieldColors
import com.ntk.callshield.ui.theme.LuminousMint
import com.ntk.callshield.ui.theme.MidnightSlate
import com.ntk.callshield.ui.theme.MonoFontFamily
import com.ntk.callshield.ui.theme.TrustTeal

@Composable
fun riskAccentColor(riskScore: Int): Color {
    val semantic = LocalShieldColors.current
    return when {
        riskScore >= 70 -> semantic.danger
        riskScore >= 35 -> semantic.caution
        else -> semantic.verified
    }
}

@Composable
fun riskContainerColor(riskScore: Int): Color {
    val semantic = LocalShieldColors.current
    return when {
        riskScore >= 70 -> semantic.dangerContainer
        riskScore >= 35 -> semantic.cautionContainer
        else -> semantic.verifiedContainer
    }
}

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val semantic = LocalShieldColors.current
    Card(
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(0.75.dp, borderColor ?: semantic.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 0.dp else 2.dp),
        modifier = modifier
    ) {
        Column(content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun callShieldSegmentedColors(): SegmentedButtonColors {
    val semantic = LocalShieldColors.current
    return SegmentedButtonDefaults.colors(
        activeContainerColor = semantic.verifiedContainer,
        activeContentColor = semantic.verified,
        activeBorderColor = semantic.verified.copy(alpha = 0.5f),
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        inactiveBorderColor = semantic.cardBorder
    )
}

@Composable
fun callShieldFilterChipColors(): SelectableChipColors {
    val semantic = LocalShieldColors.current
    return FilterChipDefaults.filterChipColors(
        selectedContainerColor = semantic.verifiedContainer,
        selectedLabelColor = semantic.verified,
        selectedLeadingIconColor = semantic.verified,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Official NTK CallShield vector logo drawn in Compose Canvas:
 * Symmetrical geometric shield fused with minimalist handset & two protective signal arcs
 * in Trust Teal -> Luminous Mint on a sleek metallic Midnight Slate badge.
 */
@Composable
fun NtkCallShieldLogo(
    size: Dp = 44.dp,
    showBadge: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        if (showBadge) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF15243B), Color(0xFF070D19))
                ),
                topLeft = Offset.Zero,
                size = Size(w, h),
                cornerRadius = CornerRadius(w * 0.28f, h * 0.28f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f),
                topLeft = Offset(0.5f, 0.5f),
                size = Size(w - 1f, h - 1f),
                cornerRadius = CornerRadius(w * 0.28f, h * 0.28f),
                style = Stroke(width = (w * 0.025f).coerceAtLeast(1.dp.toPx()))
            )
        }

        // Symmetrical Shield Path
        val pad = if (showBadge) w * 0.17f else w * 0.08f
        val topY = pad
        val bottomY = h - pad * 0.9f
        val leftX = pad
        val rightX = w - pad
        val midX = w / 2f
        val shoulderY = topY + (bottomY - topY) * 0.16f
        val midY = topY + (bottomY - topY) * 0.56f

        val shieldPath = Path().apply {
            moveTo(midX, topY)
            lineTo(rightX, shoulderY)
            lineTo(rightX, midY)
            cubicTo(rightX, bottomY - (bottomY - midY) * 0.35f, midX + (rightX - midX) * 0.45f, bottomY - 2f, midX, bottomY)
            cubicTo(midX - (midX - leftX) * 0.45f, bottomY - 2f, leftX, bottomY - (bottomY - midY) * 0.35f, leftX, midY)
            lineTo(leftX, shoulderY)
            close()
        }

        drawPath(
            path = shieldPath,
            brush = Brush.linearGradient(
                colors = listOf(TrustTeal, LuminousMint),
                start = Offset(leftX, topY),
                end = Offset(rightX, bottomY)
            )
        )

        drawPath(
            path = shieldPath,
            color = Color.White.copy(alpha = 0.45f),
            style = Stroke(width = (w * 0.025f).coerceAtLeast(1.2f))
        )

        // Minimalist white phone handset arc inside shield
        val phoneTopLeft = Offset(w * 0.35f, h * 0.37f)
        drawArc(
            color = Color.White,
            startAngle = 128f,
            sweepAngle = 104f,
            useCenter = false,
            topLeft = phoneTopLeft,
            size = Size(w * 0.28f, h * 0.28f),
            style = Stroke(width = (w * 0.07f).coerceAtLeast(2.4f), cap = StrokeCap.Round)
        )

        // Two subtle protective signal arcs
        drawArc(
            color = Color.White,
            startAngle = -85f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(w * 0.46f, h * 0.36f),
            size = Size(w * 0.18f, h * 0.18f),
            style = Stroke(width = (w * 0.036f).coerceAtLeast(1.5f), cap = StrokeCap.Round)
        )
        drawArc(
            color = Color.White.copy(alpha = 0.85f),
            startAngle = -85f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(w * 0.43f, h * 0.29f),
            size = Size(w * 0.29f, h * 0.29f),
            style = Stroke(width = (w * 0.034f).coerceAtLeast(1.4f), cap = StrokeCap.Round)
        )
    }
}

/**
 * Small (?) info chip that opens a calm plain-English 2-sentence explanation bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoExplainChip(
    title: String,
    explanation: String,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable { open = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = "Explain $title",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text("?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }

    if (open) {
        ModalBottomSheet(onDismissRequest = { open = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    explanation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { open = false }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Got it")
                }
            }
        }
    }
}

@Composable
fun ShimmerLookupSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer_lookup")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    val blockColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.width(120.dp).height(22.dp).clip(RoundedCornerShape(999.dp)).background(blockColor))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(26.dp).clip(RoundedCornerShape(8.dp)).background(blockColor))
            Box(modifier = Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(14.dp)).background(blockColor))
        }
    }
}

@Composable
fun SafetyMeterGauge(
    score: Int,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp
) {
    val animatedSweep by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f) * 240f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
        label = "safety_meter_sweep"
    )
    val accent = riskAccentColor(score)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val stroke = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
            val arcSize = Size(this.size.width, this.size.height)
            drawArc(
                color = trackColor,
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = Offset.Zero,
                size = arcSize,
                style = stroke
            )
            drawArc(
                color = accent,
                startAngle = 150f,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = Offset.Zero,
                size = arcSize,
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = MonoFontFamily,
                    color = accent
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SevenDaySafetyChart(
    metrics: List<DaySafetyMetric>,
    modifier: Modifier = Modifier
) {
    val semantic = LocalShieldColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val maxVal = (metrics.maxOfOrNull { it.blockedCount + it.cautionCount + it.safeCount } ?: 1).coerceAtLeast(3)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartLegendDot(semantic.danger, "Blocked")
            ChartLegendDot(semantic.caution, "Caution")
            ChartLegendDot(semantic.verified, "Safe")
        }
        Spacer(Modifier.height(12.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
        ) {
            val count = metrics.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.44f).coerceAtMost(24.dp.toPx())
            val chartHeight = size.height - 6.dp.toPx()

            for (i in 0..2) {
                val y = chartHeight * (i / 2f)
                drawLine(
                    color = gridColor.copy(alpha = 0.6f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            metrics.forEachIndexed { idx, metric ->
                val centerX = slotWidth * idx + slotWidth / 2f
                val left = centerX - barWidth / 2f
                val total = metric.blockedCount + metric.cautionCount + metric.safeCount
                if (total == 0) {
                    drawRoundRect(
                        color = gridColor.copy(alpha = 0.45f),
                        topLeft = Offset(left, chartHeight - 4.dp.toPx()),
                        size = Size(barWidth, 4.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                } else {
                    var currentBottom = chartHeight
                    val segments = listOf(
                        metric.safeCount to semantic.verified,
                        metric.cautionCount to semantic.caution,
                        metric.blockedCount to semantic.danger
                    )
                    for ((segVal, color) in segments) {
                        if (segVal <= 0) continue
                        val segH = (segVal.toFloat() / maxVal.toFloat()) * (chartHeight - 8.dp.toPx())
                        val top = (currentBottom - segH).coerceAtLeast(0f)
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, segH.coerceAtLeast(4.dp.toPx())),
                            cornerRadius = CornerRadius(5.dp.toPx())
                        )
                        currentBottom = top - 2.dp.toPx()
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            metrics.forEach { metric ->
                Text(
                    text = metric.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StatusPillBadge(
    text: String,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(0.75.dp, accentColor.copy(alpha = 0.28f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = accentColor,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NtkFloatingCallerIdPreviewCard(
    callerName: String,
    phoneNumber: String,
    riskScore: Int,
    categoryLabel: String,
    carrierAndLine: String,
    verificationStatus: CarrierVerificationStatus,
    style: OverlayStyle,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val semantic = LocalShieldColors.current
    val accent = riskAccentColor(riskScore)
    val badgeContainer = riskContainerColor(riskScore)
    val isFullBanner = style == OverlayStyle.ALERT_BANNER
    val bgColor = if (isFullBanner) accent else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (isFullBanner) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (isFullBanner) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant

    val badgeIcon = when {
        riskScore >= 70 -> Icons.Default.Warning
        riskScore >= 35 -> Icons.Default.Shield
        else -> Icons.Default.VerifiedUser
    }

    Card(
        shape = RoundedCornerShape(if (style == OverlayStyle.COMPACT_PILL) 999.dp else 20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(
            width = 1.dp,
            color = if (isFullBanner) Color.White.copy(alpha = 0.35f) else accent.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 2.dp else 6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        if (style == OverlayStyle.COMPACT_PILL) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(badgeIcon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                Text(
                    callerName,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                StatusPillBadge(
                    text = verificationStatus.plainLabel,
                    accentColor = accent,
                    containerColor = badgeContainer
                )
                if (onDismiss != null) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = subColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(if (isFullBanner) Color.White.copy(alpha = 0.7f) else accent)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NtkCallShieldLogo(size = 38.dp, showBadge = true)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "NTK CALLSHIELD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    letterSpacing = 0.8.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isFullBanner) Color.White.copy(alpha = 0.9f) else accent,
                                maxLines = 1
                            )
                            Text(
                                text = callerName,
                                style = MaterialTheme.typography.titleMedium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (onDismiss != null) {
                            Surface(
                                color = if (isFullBanner) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp).clickable(onClick = onDismiss)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = subColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = phoneNumber,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = carrierAndLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusPillBadge(
                            text = verificationStatus.plainLabel,
                            accentColor = if (isFullBanner) Color.White else accent,
                            containerColor = if (isFullBanner) Color.Black.copy(alpha = 0.22f) else badgeContainer
                        )
                        StatusPillBadge(
                            text = categoryLabel,
                            accentColor = if (isFullBanner) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = if (isFullBanner) Color.Black.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    }
                }
            }
        }
    }
}

/**
 * Smooth ModalBottomSheet for post-call summary actions (Save / Block / Report Scam).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCallSummaryBottomSheet(
    decision: ScreeningDecision,
    onBlock: () -> Unit,
    onWhitelist: () -> Unit,
    onReport: () -> Unit,
    onSaveContact: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = riskAccentColor(decision.riskScore)
    val semantic = LocalShieldColors.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NtkCallShieldLogo(size = 36.dp, showBadge = true)
                Column {
                    Text("Call Summary", style = MaterialTheme.typography.titleLarge)
                    Text(decision.verificationStatus.plainLabel, style = MaterialTheme.typography.labelMedium, color = accent)
                }
            }

            Text(decision.displayName, style = MaterialTheme.typography.headlineSmall)
            Text(
                PhoneNumberNormalizer.formatForDisplay(decision.normalizedNumber),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(decision.plainReason, style = MaterialTheme.typography.bodyMedium)
                    Text("${decision.lineType.label} · ${decision.carrier}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onSaveContact, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save")
                }
                OutlinedButton(onClick = onReport, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Report Scam")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onWhitelist, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Safe List")
                }
                Button(
                    onClick = onBlock,
                    colors = ButtonDefaults.buttonColors(containerColor = semantic.danger),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Block")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportScamBottomSheet(
    phoneNumber: String,
    initialName: String,
    onSubmit: (callerName: String, category: ThreatCategory, blockLocally: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedCategory by remember { mutableStateOf(ThreatCategory.ROBOCALL) }
    var blockLocally by remember { mutableStateOf(true) }

    val selectableCategories = listOf(
        ThreatCategory.TELEMARKETER,
        ThreatCategory.BANK_IMPERSONATION,
        ThreatCategory.ROBOCALL,
        ThreatCategory.DELIVERY_SCAM,
        ThreatCategory.TECH_SUPPORT_SCAM,
        ThreatCategory.VERIFIED_BUSINESS
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Report Number", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = PhoneNumberNormalizer.formatForDisplay(phoneNumber),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Who was calling? (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("What kind of call was it?", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectableCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                            if (cat == ThreatCategory.VERIFIED_BUSINESS) blockLocally = false
                        },
                        label = { Text(cat.label) }
                    )
                }
            }

            if (selectedCategory != ThreatCategory.VERIFIED_BUSINESS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Also block this number on my phone", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = blockLocally, onCheckedChange = { blockLocally = it })
                }
            }

            Button(
                onClick = {
                    onSubmit(name, selectedCategory, blockLocally)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_report_button")
            ) {
                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Submit & Earn Points")
            }
        }
    }
}

/**
 * Smart Dialpad with live T9 / number match preview as the user types.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartDialpadBottomSheet(
    contacts: List<ContactEntity>,
    intelList: List<PhoneIntelEntity>,
    onLookupNumber: (String) -> Unit,
    onDialNumber: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var digits by remember { mutableStateOf("") }
    val keys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("+" to "", "0" to "+", "#" to "")
    )

    val liveContactMatch = remember(digits, contacts) {
        if (digits.length < 2) null
        else contacts.firstOrNull {
            it.normalizedNumber.contains(digits) || matchesT9Name(it.displayName, digits)
        }
    }
    val liveIntelMatch = remember(digits, intelList) {
        if (digits.length < 3) null
        else intelList.firstOrNull { it.normalizedNumber.contains(digits) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (digits.isEmpty()) "Type number or name (T9)" else digits,
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = MonoFontFamily),
                    color = if (digits.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (digits.isNotEmpty()) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            digits = digits.dropLast(1)
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete")
                }
            }

            // Live T9 / Caller ID Preview Strip
            if (liveContactMatch != null || liveIntelMatch != null) {
                val previewTitle = liveContactMatch?.displayName ?: liveIntelMatch?.callerName.orEmpty()
                val previewNumber = liveContactMatch?.displayNumber ?: liveIntelMatch?.displayNumber.orEmpty()
                val previewRisk = if (liveContactMatch != null) 0 else (liveIntelMatch?.riskScore ?: 0)
                val accent = riskAccentColor(previewRisk)
                Surface(
                    color = riskContainerColor(previewRisk),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            digits = (liveContactMatch?.normalizedNumber ?: liveIntelMatch?.normalizedNumber).orEmpty()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(previewTitle, style = MaterialTheme.typography.titleSmall, color = accent)
                            Text(previewNumber, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(
                            if (previewRisk >= 70) "High Risk" else if (previewRisk >= 35) "Caution" else "Safe",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
                    }
                }
            }

            HorizontalDivider()

            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { (digit, letters) ->
                        Column(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (digits.length < 18) digits += digit
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(digit, style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily))
                            if (letters.isNotEmpty()) {
                                Text(letters, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        val target = liveContactMatch?.normalizedNumber ?: digits
                        if (target.isNotBlank()) {
                            onLookupNumber(target)
                            onDismiss()
                        }
                    },
                    enabled = digits.length >= 3 || liveContactMatch != null,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Check")
                }
                Button(
                    onClick = {
                        val target = liveContactMatch?.normalizedNumber ?: digits
                        if (target.isNotBlank()) {
                            onDialNumber(target)
                            onDismiss()
                        }
                    },
                    enabled = digits.length >= 3 || liveContactMatch != null,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Call")
                }
            }
        }
    }
}

private fun matchesT9Name(name: String, digits: String): Boolean {
    if (digits.any { !it.isDigit() }) return false
    val t9 = buildString {
        for (ch in name.uppercase()) {
            when (ch) {
                'A', 'B', 'C' -> append('2')
                'D', 'E', 'F' -> append('3')
                'G', 'H', 'I' -> append('4')
                'J', 'K', 'L' -> append('5')
                'M', 'N', 'O' -> append('6')
                'P', 'Q', 'R', 'S' -> append('7')
                'T', 'U', 'V' -> append('8')
                'W', 'X', 'Y', 'Z' -> append('9')
            }
        }
    }
    return t9.contains(digits)
}

@Composable
fun PinLockGateScreen(
    biometricAllowed: Boolean,
    errorMessage: String?,
    onPinComplete: (String) -> Unit,
    onBiometricUnlock: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var entered by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NtkCallShieldLogo(size = 64.dp, showBadge = true)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("NTK CallShield Locked", style = MaterialTheme.typography.headlineSmall)
                }
                Text(
                    "Enter your 4-digit PIN (default: 1234)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    repeat(4) { idx ->
                        val filled = idx < entered.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (filled) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }
                if (errorMessage != null) {
                    Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = LocalShieldColors.current.danger)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { d ->
                            PinKeyButton(label = d) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (entered.length < 4) {
                                    entered += d
                                    if (entered.length == 4) {
                                        onPinComplete(entered)
                                        entered = ""
                                    }
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (biometricAllowed) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onBiometricUnlock()
                                }
                                .testTag("biometric_unlock_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = "Unlock with biometrics", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                        }
                    } else {
                        Spacer(Modifier.size(68.dp))
                    }
                    PinKeyButton(label = "0") {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (entered.length < 4) {
                            entered += "0"
                            if (entered.length == 4) {
                                onPinComplete(entered)
                                entered = ""
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable { if (entered.isNotEmpty()) entered = entered.dropLast(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeyButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .testTag("pin_key_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = MonoFontFamily, fontSize = 22.sp))
    }
}
