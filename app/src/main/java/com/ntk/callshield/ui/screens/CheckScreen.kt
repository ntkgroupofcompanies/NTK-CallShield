package com.ntk.callshield.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntk.callshield.data.local.SmsMessageEntity
import com.ntk.callshield.domain.PhoneIntelligenceReport
import com.ntk.callshield.domain.SmsCategory
import com.ntk.callshield.domain.UrlRiskAnalysis
import com.ntk.callshield.ui.components.PremiumCard
import com.ntk.callshield.ui.components.ReportScamBottomSheet
import com.ntk.callshield.ui.components.SafetyMeterGauge
import com.ntk.callshield.ui.components.ShimmerLookupSkeleton
import com.ntk.callshield.ui.components.StatusPillBadge
import com.ntk.callshield.ui.components.callShieldFilterChipColors
import com.ntk.callshield.ui.components.callShieldSegmentedColors
import com.ntk.callshield.ui.components.riskAccentColor
import com.ntk.callshield.ui.components.riskContainerColor
import com.ntk.callshield.ui.theme.LocalShieldColors
import com.ntk.callshield.ui.theme.MonoFontFamily
import com.ntk.callshield.viewmodel.CallShieldViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckScreen(
    viewModel: CallShieldViewModel,
    initialSection: Int = 0,
    onOpenCallsForNumber: (String) -> Unit = {}
) {
    var selectedSegment by remember(initialSection) { mutableIntStateOf(initialSection) }
    val segments = listOf("Phone", "Link & Text", "Inbox")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Scam & Caller Check", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Look up any number, link, or suspicious text",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            segments.forEachIndexed { idx, label ->
                SegmentedButton(
                    selected = selectedSegment == idx,
                    onClick = { selectedSegment = idx },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = segments.size),
                    colors = callShieldSegmentedColors(),
                    modifier = Modifier.testTag("check_segment_$idx")
                ) {
                    Text(label, maxLines = 1, softWrap = false, fontSize = 12.sp)
                }
            }
        }

        when (selectedSegment) {
            0 -> ReversePhoneLookupSection(viewModel, onOpenCallsForNumber)
            1 -> LinkAndSmsScannerSection(viewModel)
            else -> SmartSmsInboxSection(viewModel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReversePhoneLookupSection(
    viewModel: CallShieldViewModel,
    onOpenCallsForNumber: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val query by viewModel.lookupQuery.collectAsStateWithLifecycle()
    val report by viewModel.lookupReport.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLookingUpNumber.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current
    var showReportSheet by remember { mutableStateOf(false) }

    val quickSamples = listOf(
        "Chase Bank" to "+18009359935",
        "Robocall Bot" to "+18885550194",
        "+224 Fraud" to "+224620550194",
        "VoIP Solar" to "+14155550182"
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().testTag("reverse_lookup_list")
    ) {
        item {
            PremiumCard(cornerRadius = 20.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::updateLookupQuery,
                        label = { Text("Type or paste a phone number") },
                        leadingIcon = { Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = semantic.verified) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateLookupQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("reverse_lookup_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.runReverseLookup() },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.weight(1f).height(50.dp).testTag("reverse_lookup_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Check Number", maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = {
                                val clip = clipboard.getText()?.text?.trim()
                                if (!clip.isNullOrBlank()) {
                                    viewModel.runReverseLookup(clip)
                                } else {
                                    viewModel.showMessage("Clipboard is empty")
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(50.dp).testTag("paste_clipboard_number_btn")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Paste", maxLines = 1, softWrap = false)
                        }
                    }

                    Text("Try an example:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickSamples.forEach { (label, num) ->
                            AssistChip(
                                onClick = { viewModel.runReverseLookup(num) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        if (isLoading) {
            item { ShimmerLookupSkeleton() }
        } else if (report != null) {
            item {
                CallerIntelligenceReportCard(
                    report = report!!,
                    onViewCalls = { onOpenCallsForNumber(report!!.normalizedNumber) },
                    onBlock = { viewModel.blockNumberWithUndo(report!!.normalizedNumber, report!!.callerName) },
                    onSafeList = { viewModel.whitelistNumberWithUndo(report!!.normalizedNumber, report!!.callerName) },
                    onReport = { showReportSheet = true }
                )
            }
        } else {
            item {
                PremiumCard(
                    cornerRadius = 18.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = semantic.verified, modifier = Modifier.size(36.dp))
                        Text("Instant Caller Intelligence", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Check verified banks, delivery couriers, hospitals, or known scam numbers in under a second.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showReportSheet && report != null) {
        ReportScamBottomSheet(
            phoneNumber = report!!.normalizedNumber,
            initialName = report!!.callerName,
            onSubmit = { name, cat, block ->
                viewModel.reportNumberWithUndo(report!!.normalizedNumber, name, cat, block)
            },
            onDismiss = { showReportSheet = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CallerIntelligenceReportCard(
    report: PhoneIntelligenceReport,
    onViewCalls: () -> Unit,
    onBlock: () -> Unit,
    onSafeList: () -> Unit,
    onReport: () -> Unit
) {
    val semantic = LocalShieldColors.current
    val accent = riskAccentColor(report.riskScore)
    val container = riskContainerColor(report.riskScore)
    val safetyText = when {
        report.riskScore >= 70 -> "High Risk"
        report.riskScore >= 35 -> "Caution"
        else -> "Safe"
    }
    val verdictTitle = when {
        report.riskScore >= 70 -> "High risk — Do not answer or share codes"
        report.riskScore >= 35 -> "Use caution — Unverified caller"
        else -> "Safe to answer — Verified trusted caller"
    }

    PremiumCard(
        cornerRadius = 22.dp,
        borderColor = accent.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth().testTag("intelligence_report_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusPillBadge(
                        text = report.verificationStatus.plainLabel,
                        accentColor = accent,
                        containerColor = container
                    )
                    Text(report.callerName, style = MaterialTheme.typography.headlineSmall)
                    Surface(
                        color = semantic.verifiedContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable(onClick = onViewCalls)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = semantic.verified, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${report.displayNumber} · Calls",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Medium),
                                color = semantic.verified
                            )
                        }
                    }
                    Text(
                        report.sourceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = semantic.verified
                    )
                }
                SafetyMeterGauge(score = report.riskScore, label = safetyText, size = 100.dp)
            }

            // Plain-English Verdict Banner
            Surface(
                color = container,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.75.dp, accent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (report.riskScore >= 35) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = verdictTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = accent
                    )
                }
            }

            if (report.isFinancialInstitution) {
                Surface(
                    color = semantic.cautionContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.75.dp, semantic.caution.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().testTag("bank_safety_tip_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = semantic.caution, modifier = Modifier.size(18.dp))
                        Text(
                            "Tip: Scammers can fake real numbers. If unsure, hang up and dial the number on your card.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.75.dp, semantic.cardBorder)
            ) {
                val rows = listOf(
                    "Line Type" to report.lineType.label,
                    "Carrier" to report.carrier,
                    "Carrier Check" to report.verificationStatus.plainLabel,
                    "Region" to report.region,
                    "SIM Swap Risk" to report.simSwapRisk,
                    "Call Forwarding" to report.callForwardingRisk,
                    "Community Reports" to "${report.communityReports}"
                )
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    rows.forEachIndexed { index, (label, value) ->
                        InsightRow(label, value, modifier = Modifier.padding(vertical = 8.dp))
                        if (index < rows.lastIndex) {
                            HorizontalDivider(thickness = 0.75.dp, color = semantic.cardBorder)
                        }
                    }
                }
            }

            if (report.communityTags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    report.communityTags.forEach { tag ->
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(999.dp)) {
                            Text(
                                "#$tag",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onBlock,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = semantic.danger),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Block", maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = onSafeList,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Safe List", maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = onReport,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Report", maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun InsightRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
            textAlign = TextAlign.End
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinkAndSmsScannerSection(viewModel: CallShieldViewModel) {
    val input by viewModel.urlScanInput.collectAsStateWithLifecycle()
    val result by viewModel.urlScanResult.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current

    val samplePayloads = listOf(
        "Toll Text Scam" to "FINAL NOTICE: Unpaid toll $6.99. Account suspended within 24 hours: http://usps-toll-pay.xyz/verify",
        "Fake PayPal" to "PayPa1 Security: Unauthorized login. Verify immediately via https://bit.ly/paypa1-recover",
        "Real Bank Link" to "Your Chase receipt is ready at https://www.chase.com/personal/banking"
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            PremiumCard(cornerRadius = 20.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Link & Message Scam Scanner", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = input,
                        onValueChange = viewModel::updateUrlScanInput,
                        label = { Text("Paste any suspicious text message or link") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = semantic.verified) },
                        minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("url_scanner_input")
                    )
                    Button(
                        onClick = { viewModel.runUrlPhishingScan() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("url_scanner_button")
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Check for Scams (On-Device)", maxLines = 1, softWrap = false)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        samplePayloads.forEach { (title, payload) ->
                            AssistChip(onClick = { viewModel.runUrlPhishingScan(payload) }, label = { Text(title) })
                        }
                    }
                }
            }
        }

        if (result != null) {
            item { LinkScanResultCard(analysis = result!!) }
        }
    }
}

@Composable
private fun LinkScanResultCard(analysis: UrlRiskAnalysis) {
    val accent = riskAccentColor(analysis.riskScore)
    PremiumCard(
        cornerRadius = 22.dp,
        borderColor = accent.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth().testTag("url_risk_result_card")
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPillBadge(
                        text = analysis.severity,
                        accentColor = accent,
                        containerColor = riskContainerColor(analysis.riskScore)
                    )
                    Text(analysis.recommendedAction, style = MaterialTheme.typography.bodyMedium)
                }
                SafetyMeterGauge(score = analysis.riskScore, label = "Risk", size = 96.dp)
            }

            if (analysis.factors.isNotEmpty()) {
                Text("Why we flagged this:", style = MaterialTheme.typography.labelLarge)
                analysis.factors.forEach { factor ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(factor.title, style = MaterialTheme.typography.titleSmall)
                            Text(factor.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartSmsInboxSection(viewModel: CallShieldViewModel) {
    val messages by viewModel.smsMessages.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedSmsCategory.collectAsStateWithLifecycle()
    val permStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current
    val clipboard = LocalClipboardManager.current

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissionsAndSyncDeviceData(showToast = granted)
    }

    var pasteSender by remember { mutableStateOf("") }
    var pasteBody by remember { mutableStateOf("") }
    var showPasteComposer by remember { mutableStateOf(false) }

    val filtered = remember(messages, selectedCategory) {
        if (selectedCategory == null) messages else messages.filter { it.category == selectedCategory?.name }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (!permStatus.hasSmsPermission) {
            item {
                PremiumCard(
                    cornerRadius = 18.dp,
                    containerColor = semantic.verifiedContainer.copy(alpha = 0.45f),
                    borderColor = semantic.verified.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Connect Your SMS Inbox", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Automatically sort OTP verification codes, highlight phishing texts, and keep everything on-device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { smsPermissionLauncher.launch(Manifest.permission.READ_SMS) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Allow SMS Access", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Smart SMS Inbox", style = MaterialTheme.typography.titleMedium)
                AssistChip(
                    onClick = { showPasteComposer = !showPasteComposer },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Paste & Check") }
                )
            }
        }

        if (showPasteComposer) {
            item {
                PremiumCard(
                    cornerRadius = 18.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = pasteSender,
                            onValueChange = { pasteSender = it },
                            label = { Text("Sender (optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = pasteBody,
                            onValueChange = { pasteBody = it },
                            label = { Text("Paste any message to sort & check") },
                            minLines = 2,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                viewModel.pasteAndIngestSms(pasteSender, pasteBody)
                                pasteSender = ""
                                pasteBody = ""
                                showPasteComposer = false
                            },
                            enabled = pasteBody.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Sort & Check Message")
                        }
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.setSmsCategoryFilter(null) },
                        label = { Text("All (${messages.size})", maxLines = 1, softWrap = false) },
                        colors = callShieldFilterChipColors(),
                        shape = RoundedCornerShape(999.dp)
                    )
                }
                items(SmsCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.setSmsCategoryFilter(if (selectedCategory == cat) null else cat) },
                        label = { Text(cat.label, maxLines = 1, softWrap = false) },
                        colors = callShieldFilterChipColors(),
                        shape = RoundedCornerShape(999.dp)
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { sms ->
            SmsCard(
                sms = sms,
                onCopyOtp = { otp ->
                    clipboard.setText(AnnotatedString(otp))
                    viewModel.showMessage("Copied code $otp")
                }
            )
        }
    }
}

@Composable
private fun SmsCard(
    sms: SmsMessageEntity,
    onCopyOtp: (String) -> Unit
) {
    val semantic = LocalShieldColors.current
    val cat = runCatching { SmsCategory.valueOf(sms.category) }.getOrDefault(SmsCategory.PERSONAL)
    val accent = when (cat) {
        SmsCategory.BLOCKED_SCAMS -> semantic.danger
        SmsCategory.CODES_ALERTS -> semantic.verified
        SmsCategory.PROMOS -> semantic.caution
        SmsCategory.PERSONAL -> semantic.info
    }
    val container = when (cat) {
        SmsCategory.BLOCKED_SCAMS -> semantic.dangerContainer
        SmsCategory.CODES_ALERTS -> semantic.verifiedContainer
        SmsCategory.PROMOS -> semantic.cautionContainer
        SmsCategory.PERSONAL -> semantic.infoContainer
    }

    PremiumCard(
        cornerRadius = 18.dp,
        borderColor = if (cat == SmsCategory.BLOCKED_SCAMS) semantic.danger.copy(alpha = 0.55f) else semantic.cardBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    Text(sms.sender, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                StatusPillBadge(text = cat.label, accentColor = accent, containerColor = container)
            }
            Text(sms.body, style = MaterialTheme.typography.bodyMedium)

            if (!sms.extractedOtp.isNullOrBlank()) {
                Surface(
                    color = semantic.verifiedContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCopyOtp(sms.extractedOtp) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Verification Code", style = MaterialTheme.typography.labelSmall, color = semantic.verified)
                            Text(
                                sms.extractedOtp,
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                                color = semantic.verified
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = semantic.verified, modifier = Modifier.size(18.dp))
                            Text("Copy Code", style = MaterialTheme.typography.labelLarge, color = semantic.verified)
                        }
                    }
                }
            }

            if (sms.riskScore >= 40) {
                Surface(
                    color = semantic.dangerContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Scam Alert · ${sms.riskSummary}",
                        style = MaterialTheme.typography.labelMedium,
                        color = semantic.danger,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
