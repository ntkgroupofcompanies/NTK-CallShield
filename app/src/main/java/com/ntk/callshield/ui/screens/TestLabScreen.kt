package com.ntk.callshield.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntk.callshield.domain.PhoneNumberNormalizer
import com.ntk.callshield.domain.ThreatCategory
import com.ntk.callshield.ui.components.NtkFloatingCallerIdPreviewCard
import com.ntk.callshield.ui.components.PostCallSummaryBottomSheet
import com.ntk.callshield.ui.components.PremiumCard
import com.ntk.callshield.ui.components.StatusPillBadge
import com.ntk.callshield.ui.theme.LocalShieldColors
import com.ntk.callshield.ui.theme.MonoFontFamily
import com.ntk.callshield.viewmodel.CallShieldViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestLabScreen(
    viewModel: CallShieldViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val labState by viewModel.testLabState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current

    var techDetailsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = semantic.verified)
                    Text("Test My Shield", style = MaterialTheme.typography.titleLarge)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("test_lab_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize().testTag("test_lab_list")
        ) {
            // Interactive Floating Caller ID Popup Preview
            item {
                AnimatedVisibility(visible = labState.showLiveOverlayBanner && labState.activeTelemetry?.decision != null) {
                    val decision = labState.activeTelemetry!!.decision!!
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "LIVE CALLER ID POPUP PREVIEW",
                            style = MaterialTheme.typography.labelMedium,
                            color = semantic.verified
                        )
                        NtkFloatingCallerIdPreviewCard(
                            callerName = decision.displayName,
                            phoneNumber = PhoneNumberNormalizer.formatForDisplay(decision.normalizedNumber),
                            riskScore = decision.riskScore,
                            categoryLabel = decision.category.label,
                            carrierAndLine = "${decision.lineType.label} · ${decision.carrier}",
                            verificationStatus = decision.verificationStatus,
                            style = settings.overlayStyle,
                            onDismiss = viewModel::openPostCallSummary,
                            modifier = Modifier.testTag("live_overlay_preview_card")
                        )
                        OutlinedButton(
                            onClick = viewModel::openPostCallSummary,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("End Call & View Summary", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Friendly Outcome Card + Collapsible Technical Telemetry Accordion
            item {
                val telemetry = labState.activeTelemetry
                PremiumCard(
                    cornerRadius = 20.dp,
                    borderColor = semantic.verified.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth().testTag("telemetry_console_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Simulation Result", style = MaterialTheme.typography.titleMedium)
                            if (telemetry != null) {
                                StatusPillBadge(
                                    text = if (telemetry.compliantUnder500Ms) "${telemetry.phase1LatencyMs}ms · SLA ✓" else "${telemetry.phase1LatencyMs}ms",
                                    accentColor = semantic.verified,
                                    containerColor = semantic.verifiedContainer
                                )
                            }
                        }

                        if (telemetry == null) {
                            Text(
                                "Tap any test card below to see how NTK CallShield protects your phone in real time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = semantic.verified, modifier = Modifier.size(20.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(telemetry.scenarioTitle, style = MaterialTheme.typography.titleSmall)
                                    Text(telemetry.plainOutcome, style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            if (telemetry.showAfterCallReady) {
                                OutlinedButton(
                                    onClick = viewModel::openPostCallSummary,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().height(46.dp)
                                ) {
                                    Text("View Post-Call Summary", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            // Collapsible Technical Telemetry Details Accordion
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { techDetailsExpanded = !techDetailsExpanded }
                                    .testTag("tech_telemetry_accordion")
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Terminal, contentDescription = null, tint = semantic.info, modifier = Modifier.size(16.dp))
                                            Text("Technical Telemetry Details", style = MaterialTheme.typography.labelLarge)
                                        }
                                        Icon(
                                            imageVector = if (techDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "Toggle telemetry details"
                                        )
                                    }
                                    AnimatedVisibility(visible = techDetailsExpanded) {
                                        Text(
                                            text = telemetry.technicalLog,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Tap a Scenario to Test", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Results are saved automatically to Calls, Home Summary, and Missions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(viewModel.simulationScenarios, key = { it.id }) { scenario ->
                val isRisk = scenario.id.contains("scam", ignoreCase = true) ||
                    scenario.id.contains("spoof", ignoreCase = true) ||
                    scenario.id.contains("phish", ignoreCase = true)
                val iconAccent = if (isRisk) semantic.danger else semantic.verified
                val iconBg = if (isRisk) semantic.dangerContainer else semantic.verifiedContainer

                PremiumCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRisk) Icons.Default.Warning else Icons.Default.Shield,
                                contentDescription = null,
                                tint = iconAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(scenario.title, style = MaterialTheme.typography.titleSmall)
                            Text(scenario.plainDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${PhoneNumberNormalizer.formatForDisplay(scenario.phoneNumber)} · SIM ${scenario.simSlot}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = semantic.verified
                            )
                        }
                        Button(
                            onClick = { viewModel.runSimulationScenario(scenario) },
                            enabled = !labState.isRunning,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("run_${scenario.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Test", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }

    labState.activePostCallDecision?.let { decision ->
        PostCallSummaryBottomSheet(
            decision = decision,
            onBlock = {
                viewModel.blockNumberWithUndo(decision.normalizedNumber, decision.displayName)
                viewModel.dismissPostCallSummary()
            },
            onWhitelist = {
                viewModel.whitelistNumberWithUndo(decision.normalizedNumber, decision.displayName)
                viewModel.dismissPostCallSummary()
            },
            onReport = {
                viewModel.reportNumberWithUndo(
                    rawNumber = decision.normalizedNumber,
                    callerName = decision.displayName,
                    category = if (decision.riskScore >= 50) ThreatCategory.ROBOCALL else ThreatCategory.TELEMARKETER,
                    blockLocally = decision.riskScore >= 50
                )
                viewModel.dismissPostCallSummary()
            },
            onSaveContact = {
                viewModel.saveContact(decision.displayName, decision.normalizedNumber, decision.carrier, true)
                viewModel.dismissPostCallSummary()
            },
            onDismiss = viewModel::dismissPostCallSummary
        )
    }
}
