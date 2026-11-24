package com.ntk.callshield.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ntk.callshield.data.local.ContactEntity
import com.ntk.callshield.ui.components.PremiumCard
import com.ntk.callshield.ui.components.StatusPillBadge
import com.ntk.callshield.ui.theme.LocalShieldColors
import com.ntk.callshield.ui.theme.LuminousMint
import com.ntk.callshield.ui.theme.MonoFontFamily
import com.ntk.callshield.ui.theme.ObsidianHeroMid
import com.ntk.callshield.ui.theme.ObsidianHeroTeal
import com.ntk.callshield.ui.theme.ObsidianHeroTop
import com.ntk.callshield.viewmodel.CallShieldViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: CallShieldViewModel,
    onOpenMyCallerCard: () -> Unit,
    onOpenDuplicateMerger: () -> Unit,
    onOpenCallsForNumber: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isGrid by viewModel.isVisualContactGrid.collectAsStateWithLifecycle()
    val searchQuery by viewModel.contactSearchQuery.collectAsStateWithLifecycle()
    val duplicates by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val exportedJson by viewModel.exportedContactsJson.collectAsStateWithLifecycle()
    val permStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val semantic = LocalShieldColors.current

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissionsAndSyncDeviceData(showToast = granted)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionsAndSyncDeviceData()
        viewModel.refreshDuplicates()
    }

    var selectedContactId by remember { mutableStateOf<Long?>(null) }
    var showAddContact by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }

    val filteredContacts = remember(contacts, searchQuery, isGrid) {
        val base = if (searchQuery.isBlank()) contacts else contacts.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                it.displayNumber.contains(searchQuery, ignoreCase = true) ||
                it.normalizedNumber.contains(searchQuery, ignoreCase = true) ||
                it.roleOrLabel.contains(searchQuery, ignoreCase = true)
        }
        if (isGrid) base else base.sortedBy { it.displayName.lowercase() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text("Contacts", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Stored only on your phone · Never uploaded",
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.verified,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshPermissionsAndSyncDeviceData(showToast = true) },
                        modifier = Modifier.testTag("sync_contacts_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync contacts")
                    }
                    IconButton(
                        onClick = viewModel::toggleContactViewMode,
                        modifier = Modifier.testTag("toggle_contact_view_mode")
                    ) {
                        Icon(
                            imageVector = if (isGrid) Icons.AutoMirrored.Filled.FormatListBulleted else Icons.Default.GridView,
                            contentDescription = if (isGrid) "Alphabetical list view" else "Visual photo grid"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Search contacts bar FIRST
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::updateContactSearch,
                        placeholder = { Text("Search ${contacts.size} contacts...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = semantic.verified) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Permission prompt card if READ_CONTACTS is not yet granted
                if (!permStatus.hasContactsPermission) {
                    item {
                        PremiumCard(
                            cornerRadius = 18.dp,
                            containerColor = semantic.verifiedContainer.copy(alpha = 0.45f),
                            borderColor = semantic.verified.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth().testTag("contacts_permission_prompt")
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(semantic.verifiedContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Contacts, contentDescription = null, tint = semantic.verified, modifier = Modifier.size(20.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Connect Your Phone Contacts", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "Show your real saved numbers here and tap any number to inspect its call history. Read strictly on-device.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Button(
                                    onClick = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(46.dp).testTag("allow_contacts_btn")
                                ) {
                                    Text("Allow Contacts Access", maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }

                // 2. Compact Luxury My NTK Caller Card Banner
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 2.dp else 4.dp),
                        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenMyCallerCard)
                            .testTag("open_my_caller_card")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(listOf(ObsidianHeroTop, ObsidianHeroMid, ObsidianHeroTeal))
                                )
                        ) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(LuminousMint.copy(alpha = 0.18f), Color.Transparent),
                                        center = Offset(size.width * 0.92f, size.height * 0.2f),
                                        radius = size.maxDimension * 0.5f
                                    ),
                                    center = Offset(size.width * 0.92f, size.height * 0.2f),
                                    radius = size.maxDimension * 0.5f
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x222DD4BF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = LuminousMint, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "MY CALLER CARD",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            letterSpacing = 0.8.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = LuminousMint,
                                        maxLines = 1
                                    )
                                    Text(
                                        settings.myCallerCard.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        settings.myCallerCard.roleCompany,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = Color(0x26FFFFFF),
                                    border = BorderStroke(0.75.dp, Color(0x33FFFFFF)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        "Edit",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Compact 2-Up Contact Helpers Row
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        PremiumCard(
                            cornerRadius = 16.dp,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 84.dp)
                                .clickable(onClick = onOpenDuplicateMerger)
                                .testTag("open_duplicate_merger")
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(semantic.cautionContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.MergeType, contentDescription = null, tint = semantic.caution, modifier = Modifier.size(16.dp))
                                }
                                Text("Duplicates", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (duplicates.isEmpty()) "All clean" else "${duplicates.size} to tidy",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (duplicates.isEmpty()) semantic.verified else semantic.caution,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        PremiumCard(
                            cornerRadius = 16.dp,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 84.dp)
                                .clickable {
                                    viewModel.exportContactsBackup()
                                    showBackupDialog = true
                                }
                                .testTag("open_json_backup")
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(semantic.infoContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, tint = semantic.info, modifier = Modifier.size(16.dp))
                                }
                                Text("Backup", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "Save JSON",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (filteredContacts.isEmpty()) {
                    item {
                        PremiumCard(
                            cornerRadius = 18.dp,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Contacts, contentDescription = null, tint = semantic.verified, modifier = Modifier.size(32.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No matching contacts"
                                    else if (permStatus.hasContactsPermission) "No phone contacts with numbers found"
                                    else "Contacts appear here once connected",
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (permStatus.hasContactsPermission) "Tap + below to add a contact or tap Sync above."
                                    else "Allow Contacts Access above to view your real phone contacts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (isGrid) {
                    val pairs = filteredContacts.chunked(2)
                    items(pairs) { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            pair.forEach { contact ->
                                VisualContactTile(
                                    contact = contact,
                                    expanded = selectedContactId == contact.id,
                                    onTap = { selectedContactId = if (selectedContactId == contact.id) null else contact.id },
                                    onViewCalls = { onOpenCallsForNumber(contact.normalizedNumber) },
                                    onDial = {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.normalizedNumber}")))
                                        }
                                    },
                                    onSms = {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contact.normalizedNumber}")))
                                        }
                                    },
                                    onToggleSafe = { viewModel.toggleContactWhitelist(contact) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                } else {
                    items(filteredContacts, key = { it.id }) { contact ->
                        ClassicContactRow(
                            contact = contact,
                            onViewCalls = { onOpenCallsForNumber(contact.normalizedNumber) },
                            onDial = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.normalizedNumber}")))
                                }
                            },
                            onToggleSafe = { viewModel.toggleContactWhitelist(contact) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddContact = true },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("add_contact_fab")
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
        }
    }

    if (showAddContact) {
        AddContactSheet(
            onSave = { name, phone, role, safe ->
                viewModel.saveContact(name, phone, role, safe)
                showAddContact = false
            },
            onDismiss = { showAddContact = false }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup & Restore") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Saved locally (${contacts.size} contacts). Nothing leaves your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    ) {
                        Text(
                            text = exportedJson?.take(380).orEmpty() + if ((exportedJson?.length ?: 0) > 380) "\n..." else "",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            exportedJson?.let {
                                clipboard.setText(AnnotatedString(it))
                                viewModel.showMessage("Copied contacts backup")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Backup Text")
                    }
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        label = { Text("Paste backup text to restore") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreContactsBackup(restoreJsonInput)
                        restoreJsonInput = ""
                        showBackupDialog = false
                    },
                    enabled = restoreJsonInput.isNotBlank()
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ContactAvatar(contact: ContactEntity, sizeDp: Int) {
    val initials = contact.displayName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
    val avatarColor = Color(contact.avatarColorHex)

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(avatarColor, avatarColor.copy(alpha = 0.72f)))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        if (!contact.avatarUri.isNullOrBlank()) {
            AsyncImage(
                model = contact.avatarUri,
                contentDescription = contact.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }
    }
}

@Composable
private fun VisualContactTile(
    contact: ContactEntity,
    expanded: Boolean,
    onTap: () -> Unit,
    onViewCalls: () -> Unit,
    onDial: () -> Unit,
    onSms: () -> Unit,
    onToggleSafe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalShieldColors.current

    PremiumCard(
        cornerRadius = 20.dp,
        borderColor = if (expanded) semantic.verified else semantic.cardBorder,
        modifier = modifier.clickable(onClick = onTap)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContactAvatar(contact = contact, sizeDp = 50)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(contact.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(contact.roleOrLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // Clickable phone number -> directs straight to Calls for this number
            Surface(
                color = semantic.verifiedContainer.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable(onClick = onViewCalls)
            ) {
                Text(
                    text = contact.displayNumber,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Medium),
                    color = semantic.verified,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(semantic.verifiedContainer).clickable(onClick = onDial),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = semantic.verified, modifier = Modifier.size(18.dp))
                        }
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(semantic.infoContainer).clickable(onClick = onSms),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Message", tint = semantic.info, modifier = Modifier.size(18.dp))
                        }
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(semantic.verifiedContainer).clickable(onClick = onViewCalls),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.History, contentDescription = "View Calls", tint = semantic.verified, modifier = Modifier.size(18.dp))
                        }
                    }
                    TextButton(onClick = onToggleSafe) {
                        Text(if (contact.isWhitelisted) "Remove Safe List" else "Add Safe List", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassicContactRow(
    contact: ContactEntity,
    onViewCalls: () -> Unit,
    onDial: () -> Unit,
    onToggleSafe: () -> Unit
) {
    val semantic = LocalShieldColors.current
    PremiumCard(
        cornerRadius = 16.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onViewCalls)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ContactAvatar(contact = contact, sizeDp = 42)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(contact.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = contact.displayNumber,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Medium),
                        color = semantic.verified,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "See calls",
                        tint = semantic.verified,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    contact.roleOrLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleSafe) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = "Toggle Safe List",
                    tint = if (contact.isWhitelisted) semantic.verified else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDial) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = semantic.verified)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddContactSheet(
    onSave: (name: String, phone: String, role: String, whitelisted: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var whitelisted by remember { mutableStateOf(true) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add Contact", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Relationship or Note") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Always allow on Safe List", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = whitelisted, onCheckedChange = { whitelisted = it })
            }
            Button(
                onClick = { onSave(name, phone, role, whitelisted) },
                enabled = name.isNotBlank() && phone.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save Contact")
            }
        }
    }
}
