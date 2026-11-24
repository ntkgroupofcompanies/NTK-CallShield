package com.ntk.callshield.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntk.callshield.data.local.CallLogEntity
import com.ntk.callshield.domain.CallDisposition
import com.ntk.callshield.domain.CarrierVerificationStatus
import com.ntk.callshield.domain.PhoneNumberNormalizer
import com.ntk.callshield.domain.ThreatCategory
import com.ntk.callshield.telecom.DeviceDataSyncHelper
import com.ntk.callshield.ui.components.PremiumCard
import com.ntk.callshield.ui.components.ReportScamBottomSheet
import com.ntk.callshield.ui.components.SmartDialpadBottomSheet
import com.ntk.callshield.ui.components.StatusPillBadge
import com.ntk.callshield.ui.components.callShieldFilterChipColors
import com.ntk.callshield.ui.components.riskAccentColor
import com.ntk.callshield.ui.components.riskContainerColor
import com.ntk.callshield.ui.theme.LocalShieldColors
import com.ntk.callshield.ui.theme.MonoFontFamily
import com.ntk.callshield.viewmodel.CallFilter
import com.ntk.callshield.viewmodel.CallShieldViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    viewModel: CallShieldViewModel,
    onInspectNumberInCheck: (String) -> Unit,
    onOpenTestLab: () -> Unit
) {
    val context = LocalContext.current
    val logs by viewModel.callLogs.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val intelList by viewModel.phoneIntelList.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedCallFilter.collectAsStateWithLifecycle()
    val selectedSim by viewModel.selectedSimFilter.collectAsStateWithLifecycle()
    val selectedNumberFilter by viewModel.selectedCallNumberFilter.collectAsStateWithLifecycle()
    val permStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current

    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissionsAndSyncDeviceData(showToast = granted)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionsAndSyncDeviceData()
    }

    var showDialpad by remember { mutableStateOf(false) }
    var detailTarget by remember { mutableStateOf<CallLogEntity?>(null) }
    var reportTarget by remember { mutableStateOf<CallLogEntity?>(null) }

    val filteredLogs = remember(logs, selectedFilter, selectedSim, selectedNumberFilter) {
        logs.filter { log ->
            val filterMatch = when (selectedFilter) {
                CallFilter.ALL -> true
                CallFilter.BLOCKED -> log.disposition == CallDisposition.BLOCKED.name || log.disposition == CallDisposition.SILENCED.name
                CallFilter.VERIFIED -> log.category == ThreatCategory.VERIFIED_BANK.name ||
                    log.category == ThreatCategory.VERIFIED_BUSINESS.name ||
                    log.verificationStatus == CarrierVerificationStatus.VERIFIED.name
                CallFilter.MISSED -> log.disposition == CallDisposition.MISSED.name || log.disposition == CallDisposition.SILENCED.name
            }
            val simMatch = selectedSim == 0 || log.simSlot == selectedSim
            val numberMatch = selectedNumberFilter == null ||
                DeviceDataSyncHelper.numbersMatch(log.normalizedNumber, selectedNumberFilter) ||
                log.normalizedNumber == selectedNumberFilter
            filterMatch && simMatch && numberMatch
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text("Recent Calls", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Tap a number to filter · Swipe row to act",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshPermissionsAndSyncDeviceData(showToast = true) },
                        modifier = Modifier.testTag("sync_calls_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync calls")
                    }
                    StatusPillBadge(
                        text = "${logs.count { it.disposition == CallDisposition.BLOCKED.name }} Blocked",
                        accentColor = semantic.danger,
                        containerColor = semantic.dangerContainer,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )

            // Active Number Filter Banner when user clicked a number
            selectedNumberFilter?.let { filterNum ->
                val formatted = if (filterNum == "PRIVATE") "Private / Hidden" else PhoneNumberNormalizer.formatForDisplay(filterNum)
                PremiumCard(
                    cornerRadius = 16.dp,
                    containerColor = semantic.verifiedContainer.copy(alpha = 0.5f),
                    borderColor = semantic.verified.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("active_number_filter_banner")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Calls for $formatted",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                color = semantic.verified,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${filteredLogs.size} call${if (filteredLogs.size == 1) "" else "s"} found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (filterNum != "PRIVATE") {
                            IconButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$filterNum")))
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Call number", tint = semantic.verified, modifier = Modifier.size(18.dp))
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .clickable { viewModel.clearCallNumberFilter() }
                                .testTag("clear_number_filter_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                                Text("Show All", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }

            if (!permStatus.hasCallLogPermission && selectedNumberFilter == null) {
                PremiumCard(
                    cornerRadius = 18.dp,
                    containerColor = semantic.verifiedContainer.copy(alpha = 0.45f),
                    borderColor = semantic.verified.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("calllog_permission_prompt")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(semantic.verifiedContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = semantic.verified, modifier = Modifier.size(19.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Connect Your Call History", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "See your real incoming, outgoing, and blocked calls matched with caller protection.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = { callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("allow_calllog_btn")
                        ) {
                            Text("Allow Call Log Access", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CallFilter.entries) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { viewModel.setCallFilter(filter) },
                        label = { Text(filter.label, maxLines = 1, softWrap = false) },
                        colors = callShieldFilterChipColors(),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("call_filter_${filter.name.lowercase()}")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedSim == 1,
                        onClick = { viewModel.setSimFilter(if (selectedSim == 1) 0 else 1) },
                        leadingIcon = { Icon(Icons.Default.SimCard, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("SIM 1", maxLines = 1, softWrap = false) },
                        colors = callShieldFilterChipColors(),
                        shape = RoundedCornerShape(999.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedSim == 2,
                        onClick = { viewModel.setSimFilter(if (selectedSim == 2) 0 else 2) },
                        leadingIcon = { Icon(Icons.Default.SimCard, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("SIM 2", maxLines = 1, softWrap = false) },
                        colors = callShieldFilterChipColors(),
                        shape = RoundedCornerShape(999.dp)
                    )
                }
            }

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = semantic.verified, modifier = Modifier.size(48.dp))
                        if (selectedNumberFilter != null) {
                            val formatted = PhoneNumberNormalizer.formatForDisplay(selectedNumberFilter!!)
                            Text(
                                "No calls with $formatted yet",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Calls made to or from this number appear here automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (selectedNumberFilter != "PRIVATE") {
                                    Button(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$selectedNumberFilter")))
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Call Now")
                                    }
                                }
                                OutlinedButton(
                                    onClick = { viewModel.clearCallNumberFilter() },
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Show All Calls")
                                }
                            }
                        } else {
                            Text("No calls here yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (permStatus.hasCallLogPermission) "Recent calls on your phone and live screened calls appear here."
                                else "Allow Call Log Access above or run a scenario in Test My Shield.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = onOpenTestLab, shape = RoundedCornerShape(14.dp)) {
                                Text("Try Test My Shield")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().testTag("calls_list")
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        SwipeableCallItem(
                            log = log,
                            onSwipeBlock = { viewModel.blockNumberWithUndo(log.normalizedNumber, log.callerName) },
                            onSwipeSafe = { viewModel.whitelistNumberWithUndo(log.normalizedNumber, log.callerName) },
                            onFilterByNumber = { viewModel.setCallNumberFilter(log.normalizedNumber) },
                            onClick = { detailTarget = log }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialpad = true },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("dialpad_fab")
        ) {
            Icon(Icons.Default.Dialpad, contentDescription = "Open Smart Dialpad")
        }
    }

    // Caller Detail Bottom Sheet
    detailTarget?.let { log ->
        CallerDetailBottomSheet(
            log = log,
            onCallBack = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${log.normalizedNumber}")))
                }
                detailTarget = null
            },
            onBlock = {
                viewModel.blockNumberWithUndo(log.normalizedNumber, log.callerName)
                detailTarget = null
            },
            onSafeList = {
                viewModel.whitelistNumberWithUndo(log.normalizedNumber, log.callerName)
                detailTarget = null
            },
            onReport = {
                reportTarget = log
                detailTarget = null
            },
            onFilterNumber = {
                viewModel.setCallNumberFilter(log.normalizedNumber)
                detailTarget = null
            },
            onInspectFullReport = {
                onInspectNumberInCheck(log.normalizedNumber)
                detailTarget = null
            },
            onDismiss = { detailTarget = null }
        )
    }

    if (showDialpad) {
        SmartDialpadBottomSheet(
            contacts = contacts,
            intelList = intelList,
            onLookupNumber = { number -> onInspectNumberInCheck(number) },
            onDialNumber = { number ->
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                }
            },
            onDismiss = { showDialpad = false }
        )
    }

    reportTarget?.let { target ->
        ReportScamBottomSheet(
            phoneNumber = target.normalizedNumber,
            initialName = target.callerName,
            onSubmit = { name, cat, block ->
                viewModel.reportNumberWithUndo(target.normalizedNumber, name, cat, block)
            },
            onDismiss = { reportTarget = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SwipeableCallItem(
    log: CallLogEntity,
    onSwipeBlock: () -> Unit,
    onSwipeSafe: () -> Unit,
    onFilterByNumber: () -> Unit,
    onClick: () -> Unit
) {
    val semantic = LocalShieldColors.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onSwipeSafe()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipeBlock()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val bgColor by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> semantic.verifiedContainer
                    SwipeToDismissBoxValue.EndToStart -> semantic.dangerContainer
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                label = "call_swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = semantic.verified)
                        Text("Add to Safe List", style = MaterialTheme.typography.labelLarge, color = semantic.verified)
                    }
                } else if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Block Number", style = MaterialTheme.typography.labelLarge, color = semantic.danger)
                        Icon(Icons.Default.Block, contentDescription = null, tint = semantic.danger)
                    }
                }
            }
        }
    ) {
        val disposition = runCatching { CallDisposition.valueOf(log.disposition) }.getOrDefault(CallDisposition.ALLOWED)
        val verification = runCatching { CarrierVerificationStatus.valueOf(log.verificationStatus) }
            .getOrDefault(CarrierVerificationStatus.UNVERIFIED)
        val accent = riskAccentColor(log.riskScore)
        val container = riskContainerColor(log.riskScore)

        val badgeText = when {
            disposition == CallDisposition.BLOCKED -> "Blocked"
            disposition == CallDisposition.MISSED -> "Missed"
            disposition == CallDisposition.SILENCED -> "Caution"
            verification == CarrierVerificationStatus.VERIFIED && log.riskScore < 20 -> "Verified ✓"
            log.riskScore >= 40 -> "Caution"
            else -> "Safe"
        }

        val icon = when (disposition) {
            CallDisposition.BLOCKED -> Icons.Default.Block
            CallDisposition.SILENCED -> Icons.AutoMirrored.Filled.VolumeOff
            CallDisposition.MISSED -> Icons.AutoMirrored.Filled.PhoneMissed
            CallDisposition.BYPASSED_REPEAT -> Icons.AutoMirrored.Filled.PhoneCallback
            CallDisposition.ALLOWED -> if (log.riskScore < 35) Icons.Default.VerifiedUser else Icons.Default.Warning
        }

        PremiumCard(
            cornerRadius = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = badgeText, tint = accent, modifier = Modifier.size(21.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = log.callerName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatRelativeTime(log.timestampMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = log.displayNumber,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Medium),
                            color = semantic.verified,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(onClick = onFilterByNumber)
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "SIM ${log.simSlot}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatusPillBadge(text = badgeText, accentColor = accent, containerColor = container)
                        Text(
                            text = log.plainReason,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = if (log.riskScore >= 70) semantic.danger else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open call details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallerDetailBottomSheet(
    log: CallLogEntity,
    onCallBack: () -> Unit,
    onBlock: () -> Unit,
    onSafeList: () -> Unit,
    onReport: () -> Unit,
    onFilterNumber: () -> Unit,
    onInspectFullReport: () -> Unit,
    onDismiss: () -> Unit
) {
    val semantic = LocalShieldColors.current
    val accent = riskAccentColor(log.riskScore)
    val verification = runCatching { CarrierVerificationStatus.valueOf(log.verificationStatus) }
        .getOrDefault(CarrierVerificationStatus.UNVERIFIED)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusPillBadge(
                text = verification.plainLabel,
                accentColor = accent,
                containerColor = riskContainerColor(log.riskScore)
            )
            Text(log.callerName, style = MaterialTheme.typography.headlineSmall)
            Text(
                log.displayNumber,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.SemiBold),
                color = semantic.verified,
                modifier = Modifier.clickable(onClick = onFilterNumber)
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(log.plainReason, style = MaterialTheme.typography.bodyMedium)
                    Text("Carrier: ${log.carrier} · SIM ${log.simSlot}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onCallBack, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Call Back")
                }
                OutlinedButton(onClick = onSafeList, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Safe List")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onReport, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Report Spam")
                }
                Button(
                    onClick = onBlock,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = semantic.danger),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Block")
                }
            }

            OutlinedButton(onClick = onFilterNumber, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("View All Calls with This Number")
            }

            OutlinedButton(onClick = onInspectFullReport, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("View Full Caller Report")
            }
        }
    }
}

private fun formatRelativeTime(timestampMillis: Long): String {
    val diffMinutes = ((System.currentTimeMillis() - timestampMillis) / 60_000L).coerceAtLeast(0L)
    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "${diffMinutes}m ago"
        diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
        else -> SimpleDateFormat("MMM d", Locale.US).format(Date(timestampMillis))
    }
}
