package com.ntk.callshield.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ntk.callshield.domain.CarrierVerificationStatus
import com.ntk.callshield.domain.OverlayStyle
import com.ntk.callshield.ui.components.NtkFloatingCallerIdPreviewCard
import com.ntk.callshield.ui.components.StatusPillBadge
import com.ntk.callshield.ui.theme.LocalShieldColors
import com.ntk.callshield.ui.theme.MidnightSlate
import com.ntk.callshield.ui.theme.MonoFontFamily
import com.ntk.callshield.ui.theme.TrustTeal
import com.ntk.callshield.viewmodel.CallShieldViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineDbManagerScreen(
    viewModel: CallShieldViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val regions by viewModel.offlineRegions.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val lastDelta by viewModel.lastDeltaResult.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current
    val nf = remember { NumberFormat.getIntegerInstance(Locale.US) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Works Without Internet", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, semantic.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Offline Scam Lists", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Download small regional updates so NTK CallShield blocks scams even in airplane mode or poor signal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = semantic.verified, modifier = Modifier.size(18.dp))
                                Text("Update automatically on Wi-Fi only", style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(checked = settings.autoUpdateWifiOnly, onCheckedChange = viewModel::setAutoUpdateWifiOnly)
                        }
                    }
                }
            }

            if (lastDelta != null) {
                item {
                    val d = lastDelta!!
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = semantic.verifiedContainer),
                        border = BorderStroke(1.dp, semantic.verified)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Updated ${d.regionName} (v${d.newVersion})", style = MaterialTheme.typography.titleSmall, color = semantic.verified)
                            Text(
                                "${nf.format(d.previousCount)} + ${d.additionsCount} new − ${d.deletionsCount} retired = ${nf.format(d.newCount)} numbers",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily)
                            )
                        }
                    }
                }
            }

            items(regions, key = { it.regionId }) { region ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, semantic.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(region.regionName, style = MaterialTheme.typography.titleMedium)
                                Text(region.coverageDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusPillBadge(
                                text = if (region.isDownloaded) "Ready" else "Not Saved",
                                accentColor = if (region.isDownloaded) semantic.verified else semantic.caution,
                                containerColor = if (region.isDownloaded) semantic.verifiedContainer else semantic.cautionContainer
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${nf.format(region.entryCount)} protected numbers · ${region.sizeMb} MB", style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily))
                                Text(
                                    "Updated ${SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(region.lastSyncedMillis))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { viewModel.applyRegionalDeltaUpdate(region.regionId) },
                                modifier = Modifier.testTag("sync_region_${region.regionId}")
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (region.isDownloaded) "Update" else "Download")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeAndBlockListScreen(
    viewModel: CallShieldViewModel,
    onBack: () -> Unit,
    onOpenCallsForNumber: (String) -> Unit = {}
) {
    BackHandler(onBack = onBack)
    val intelList by viewModel.phoneIntelList.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current

    val safeList = remember(intelList) { intelList.filter { it.isWhitelisted } }
    val blockList = remember(intelList) { intelList.filter { it.isBlacklisted } }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Safe List & Block List", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = semantic.verifiedContainer),
                    border = BorderStroke(1.dp, semantic.verified.copy(alpha = 0.45f))
                ) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = semantic.verified)
                        Text(
                            "Your Safe List stays strictly on your phone so family, doctors, and trusted numbers always ring through.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item { Text("Safe List (Always Allowed · ${safeList.size})", style = MaterialTheme.typography.titleMedium) }
            if (safeList.isEmpty()) {
                item {
                    Text("No extra Safe List numbers yet. Swipe right on any call to add one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(safeList, key = { "safe_${it.normalizedNumber}" }) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth().clickable { onOpenCallsForNumber(item.normalizedNumber) }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.callerName, style = MaterialTheme.typography.titleSmall)
                                Text("${item.displayNumber} · View Calls ›", style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily), color = semantic.verified)
                            }
                            IconButton(onClick = { viewModel.removeFromSafeOrBlockListWithUndo(item, wasWhitelist = true) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = semantic.danger)
                            }
                        }
                    }
                }
            }

            item { Text("Personal Block List (${blockList.size})", style = MaterialTheme.typography.titleMedium) }
            if (blockList.isEmpty()) {
                item {
                    Text("No personally blocked numbers yet. Swipe left on any call to block it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(blockList, key = { "block_${it.normalizedNumber}" }) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth().clickable { onOpenCallsForNumber(item.normalizedNumber) }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.callerName, style = MaterialTheme.typography.titleSmall)
                                Text("${item.displayNumber} · View Calls ›", style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily), color = semantic.danger)
                            }
                            IconButton(onClick = { viewModel.removeFromSafeOrBlockListWithUndo(item, wasWhitelist = false) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Unblock", tint = semantic.danger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyCenterScreen(
    viewModel: CallShieldViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val audits by viewModel.privacyAudits.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current
    var unlistInput by remember(settings.unlistedNumber) { mutableStateOf(settings.unlistedNumber) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Privacy Center", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = semantic.verifiedContainer),
                    border = BorderStroke(1.dp, semantic.verified)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = semantic.verified)
                            Text("Contacts Never Uploaded", style = MaterialTheme.typography.titleMedium, color = semantic.verified)
                        }
                        Text(
                            "NTK CallShield never uploads your address book, never shows ads, and never sells call history.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, semantic.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Unlist My Number", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Ask to hide your personal number from public caller lookup directories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = unlistInput,
                            onValueChange = { unlistInput = it },
                            label = { Text("Your phone number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { viewModel.submitUnlistingRequest(unlistInput) },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_unlist_btn")
                        ) {
                            Text(if (settings.unlistedSubmitted) "Update Unlist Request" else "Unlist My Number")
                        }
                        if (settings.unlistedSubmitted) {
                            Text("Marked for unlisting: ${settings.unlistedNumber}", style = MaterialTheme.typography.labelMedium, color = semantic.verified)
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Privacy Activity Log", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = viewModel::clearPrivacyAudits) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }

            items(audits, key = { it.id }) { audit ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(audit.eventType, style = MaterialTheme.typography.labelSmall, color = semantic.verified)
                            StatusPillBadge(
                                text = if (audit.leftDevice) "Cloud Check" else "On-Device Only",
                                accentColor = if (audit.leftDevice) semantic.caution else semantic.verified,
                                containerColor = if (audit.leftDevice) semantic.cautionContainer else semantic.verifiedContainer
                            )
                        }
                        Text(audit.detail, style = MaterialTheme.typography.bodySmall)
                        Text(SimpleDateFormat("MMM d · HH:mm:ss", Locale.US).format(Date(audit.timestampMillis)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayCustomizerScreen(
    viewModel: CallShieldViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current
    var previewRisk by remember { mutableStateOf(64) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Caller ID Popup Style", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, semantic.cardBorder, RoundedCornerShape(24.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        "LIVE POPUP PREVIEW (${(settings.overlayVerticalPosition * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                    val topOffsetDp = (settings.overlayVerticalPosition * 175f).dp
                    Box(modifier = Modifier.padding(top = topOffsetDp).fillMaxWidth()) {
                        NtkFloatingCallerIdPreviewCard(
                            callerName = when {
                                previewRisk >= 75 -> "Fake Bank Alert Desk"
                                previewRisk >= 40 -> "Apex Solar Lead Center"
                                else -> "Chase Bank Fraud Desk"
                            },
                            phoneNumber = "+1 (415) 555-0182",
                            riskScore = previewRisk,
                            categoryLabel = when {
                                previewRisk >= 75 -> "High Risk"
                                previewRisk >= 40 -> "Telemarketer"
                                else -> "Verified Bank"
                            },
                            carrierAndLine = "Internet VoIP · RingCentral",
                            verificationStatus = if (previewRisk < 35) CarrierVerificationStatus.VERIFIED else CarrierVerificationStatus.SPOOF_WARNING,
                            style = settings.overlayStyle
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { previewRisk = 4 }, modifier = Modifier.weight(1f)) { Text("Safe") }
                    OutlinedButton(onClick = { previewRisk = 64 }, modifier = Modifier.weight(1f)) { Text("Caution") }
                    OutlinedButton(onClick = { previewRisk = 95 }, modifier = Modifier.weight(1f)) { Text("Scam") }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Popup Style", style = MaterialTheme.typography.titleMedium)
                    OverlayStyle.entries.forEach { style ->
                        val selected = settings.overlayStyle == style
                        Surface(
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, if (selected) MaterialTheme.colorScheme.primary else semantic.cardBorder),
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.updateOverlayConfig(style = style) }
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(style.label, style = MaterialTheme.typography.titleSmall)
                                    Text(style.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (selected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Vertical Position on Screen", style = MaterialTheme.typography.titleSmall)
                            Text("${(settings.overlayVerticalPosition * 100).toInt()}%", style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily))
                        }
                        Slider(
                            value = settings.overlayVerticalPosition,
                            onValueChange = { viewModel.updateOverlayConfig(verticalPosition = it) },
                            valueRange = 0.08f..0.72f
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsAndBadgesScreen(
    viewModel: CallShieldViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val missions by viewModel.missions.collectAsStateWithLifecycle()
    val badges by viewModel.guardianBadges.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Guardian Badges & Missions", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("GUARDIAN POINTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${settings.guardianPoints} PTS", style = MaterialTheme.typography.displaySmall.copy(fontFamily = MonoFontFamily), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(44.dp))
                    }
                }
            }

            item { Text("Milestone Badges", style = MaterialTheme.typography.titleMedium) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    badges.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            pair.forEach { badge ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (badge.unlocked) semantic.verifiedContainer else MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                    border = BorderStroke(1.dp, if (badge.unlocked) semantic.verified else semantic.cardBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier.size(34.dp).clip(CircleShape).background(if (badge.unlocked) semantic.verified else MaterialTheme.colorScheme.surfaceContainerLow),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.VerifiedUser,
                                                contentDescription = null,
                                                tint = if (badge.unlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(badge.title, style = MaterialTheme.typography.titleSmall)
                                        Text(badge.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Text("Active Missions", style = MaterialTheme.typography.titleMedium) }

            items(missions, key = { it.missionId }) { mission ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, if (mission.completed) semantic.verified else semantic.cardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(mission.title, style = MaterialTheme.typography.titleSmall)
                            StatusPillBadge(
                                text = if (mission.completed) "Done" else "+${mission.rewardPoints} pts",
                                accentColor = if (mission.completed) semantic.verified else semantic.caution,
                                containerColor = if (mission.completed) semantic.verifiedContainer else semantic.cautionContainer
                            )
                        }
                        Text(mission.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LinearProgressIndicator(
                            progress = { (mission.currentProgress.toFloat() / mission.targetProgress.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp))
                        )
                        Text("${mission.currentProgress} / ${mission.targetProgress}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateMergerScreen(
    viewModel: CallShieldViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val groups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current

    LaunchedEffect(Unit) { viewModel.refreshDuplicates() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Merge Duplicates", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (groups.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = semantic.verifiedContainer)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = semantic.verified, modifier = Modifier.size(40.dp))
                            Text("All Clean!", style = MaterialTheme.typography.titleLarge, color = semantic.verified)
                            Text("No duplicate phone numbers found.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                item {
                    Button(
                        onClick = viewModel::mergeAllDuplicates,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("merge_all_duplicates_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MergeType, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Merge All ${groups.size} Duplicate Groups")
                    }
                }

                items(groups.entries.toList(), key = { it.key }) { (normalized, entries) ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, semantic.caution.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(entries.first().displayNumber, style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily))
                                StatusPillBadge("${entries.size} Copies", semantic.caution, semantic.cautionContainer)
                            }
                            entries.forEach { c -> Text("• ${c.displayName} (${c.roleOrLabel})", style = MaterialTheme.typography.bodySmall) }
                            OutlinedButton(onClick = { viewModel.mergeDuplicateGroup(normalized) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Merge These Entries")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCallerCardScreen(
    viewModel: CallShieldViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val card = settings.myCallerCard

    var name by remember(card.displayName) { mutableStateOf(card.displayName) }
    var role by remember(card.roleCompany) { mutableStateOf(card.roleCompany) }
    var phone by remember(card.phoneNumber) { mutableStateOf(card.phoneNumber) }
    var note by remember(card.statusNote) { mutableStateOf(card.statusNote) }
    var verifiedBadge by remember(card.verifiedBadge) { mutableStateOf(card.verifiedBadge) }
    var avatarUri by remember(card.avatarUri) { mutableStateOf(card.avatarUri) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) avatarUri = uri.toString()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("My NTK Caller Card", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, TrustTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(MidnightSlate, Color(0xFF132D3B))))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                modifier = Modifier.size(60.dp).clip(CircleShape).background(TrustTeal),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!avatarUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = avatarUri,
                                        contentDescription = "Caller avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(name.take(2).uppercase(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(name.ifBlank { "Your Name" }, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                                    if (verifiedBadge) Icon(Icons.Default.Verified, contentDescription = "Verified", tint = Color(0xFF2DD4BF), modifier = Modifier.size(20.dp))
                                }
                                Text(role, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                                Text(phone, style = MaterialTheme.typography.labelSmall, color = Color(0xFF2DD4BF))
                            }
                        }

                        Surface(
                            color = Color(0x66162238),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0x442DD4BF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("STATUS NOTE SHOWN AFTER CALLS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2DD4BF))
                                Text(note.ifBlank { "No note added" }, style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Choose Photo")
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role or Business") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Optional Note After Calls") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Show Verified Badge", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = verifiedBadge, onCheckedChange = { verifiedBadge = it })
                    }
                    Button(
                        onClick = {
                            viewModel.updateMyCallerCard(
                                card.copy(
                                    displayName = name,
                                    roleCompany = role,
                                    phoneNumber = phone,
                                    statusNote = note,
                                    avatarUri = avatarUri,
                                    verifiedBadge = verifiedBadge
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_caller_card_btn")
                    ) {
                        Text("Save My Caller Card")
                    }
                }
            }
        }
    }
}
