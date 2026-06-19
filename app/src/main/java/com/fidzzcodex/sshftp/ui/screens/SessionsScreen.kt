package com.fidzzcodex.sshftp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.fidzzcodex.sshftp.data.model.SSHSession
import com.fidzzcodex.sshftp.ui.AppViewModel
import com.fidzzcodex.sshftp.ui.components.*
import com.fidzzcodex.sshftp.ui.theme.NeoColors
import com.fidzzcodex.sshftp.ui.theme.SSHFTPTheme

@Composable
fun SessionsScreen(vm: AppViewModel, onConnected: () -> Unit) {
    val state by vm.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editSession by remember { mutableStateOf<SSHSession?>(null) }

    // Listen for events
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            // toast handled in main activity
        }
    }

    Box(Modifier.fillMaxSize().background(SSHFTPTheme.colors.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "SAVED SESSIONS",
                    style = SSHFTPTheme.typography.h2,
                    color = SSHFTPTheme.colors.text,
                )
                NeoButton(
                    text = "New",
                    onClick = { showAddDialog = true },
                    icon = Icons.Default.Add,
                    backgroundColor = NeoColors.Blue,
                    modifier = Modifier.wrapContentWidth(),
                )
            }

            Spacer(Modifier.height(16.dp))

            if (state.sessions.isEmpty()) {
                EmptySessionsPlaceholder { showAddDialog = true }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.sessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            isActive = state.isConnected && state.connectedHost == session.host,
                            isLoading = state.isLoading,
                            onConnect = {
                                vm.connect(session, tabIndex = 0)
                                onConnected()
                            },
                            onEdit = { editSession = session },
                            onDelete = { vm.deleteSession(session) },
                        )
                    }
                }
            }
        }

        // FAB-style add if list is populated
        if (state.sessions.isNotEmpty()) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                NeoButton(
                    text = "Add Session",
                    onClick = { showAddDialog = true },
                    icon = Icons.Default.Add,
                    backgroundColor = NeoColors.Yellow,
                    textColor = NeoColors.Black,
                )
            }
        }
    }

    if (showAddDialog) {
        AddSessionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, host, port, user, pass ->
                vm.saveSession(name, host, port, user, pass)
                showAddDialog = false
            },
        )
    }

    editSession?.let { session ->
        AddSessionDialog(
            existingSession = session,
            onDismiss = { editSession = null },
            onSave = { name, host, port, user, pass ->
                vm.saveSession(name, host, port, user, pass)
                editSession = null
            },
        )
    }
}

@Composable
fun SessionCard(
    session: SSHSession,
    isActive: Boolean,
    isLoading: Boolean,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    NeoCard(
        backgroundColor = if (isActive) NeoColors.Blue.copy(alpha = 0.08f) else SSHFTPTheme.colors.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isActive) {
                        NeoChip("Active", NeoColors.Green.copy(alpha = 0.15f))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(session.name, style = SSHFTPTheme.typography.h3, color = SSHFTPTheme.colors.text)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${session.username}@${session.host}:${session.port}",
                    style = SSHFTPTheme.typography.mono,
                    color = NeoColors.Blue,
                )
                if (session.lastConnected > 0L) {
                    Text(
                        "Last: ${formatTime(session.lastConnected)}",
                        style = SSHFTPTheme.typography.bodySmall,
                        color = SSHFTPTheme.colors.textSecondary,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallIconButton(Icons.Default.Edit, "Edit", NeoColors.Yellow, NeoColors.Black) { onEdit() }
                SmallIconButton(Icons.Default.Delete, "Delete", NeoColors.Red, Color.White) { showDeleteConfirm = true }
            }
        }

        Spacer(Modifier.height(12.dp))

        NeoButton(
            text = if (isActive) "Connected" else "Connect",
            onClick = onConnect,
            enabled = !isLoading && !isActive,
            icon = if (isActive) Icons.Default.CheckCircle else Icons.Default.Link,
            backgroundColor = if (isActive) NeoColors.Green else NeoColors.Blue,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Session", style = SSHFTPTheme.typography.h3) },
            text = { Text("Delete '${session.name}'? This cannot be undone.") },
            confirmButton = {
                NeoButton("Delete", { onDelete(); showDeleteConfirm = false },
                    backgroundColor = NeoColors.Red, modifier = Modifier.wrapContentSize())
            },
            dismissButton = {
                NeoButton("Cancel", { showDeleteConfirm = false },
                    backgroundColor = Color(0xFFEEEEEE), textColor = NeoColors.Black,
                    modifier = Modifier.wrapContentSize())
            },
        )
    }
}

@Composable
fun SmallIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, bg: Color, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .neoBorder(2.dp, NeoColors.Black)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun EmptySessionsPlaceholder(onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NeoCard(backgroundColor = NeoColors.Yellow.copy(alpha = 0.1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Terminal, "No sessions",
                        tint = NeoColors.Blue, modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("NO SESSIONS YET", style = SSHFTPTheme.typography.h3, color = SSHFTPTheme.colors.text)
                    Text("Add a new SSH session to get started",
                        style = SSHFTPTheme.typography.body, color = SSHFTPTheme.colors.textSecondary)
                    Spacer(Modifier.height(16.dp))
                    NeoButton("Add First Session", onAdd, icon = Icons.Default.Add)
                }
            }
        }
    }
}

@Composable
fun AddSessionDialog(
    existingSession: SSHSession? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(existingSession?.name ?: "") }
    var host by remember { mutableStateOf(existingSession?.host ?: "") }
    var port by remember { mutableStateOf(existingSession?.port?.toString() ?: "22") }
    var user by remember { mutableStateOf(existingSession?.username ?: "") }
    var pass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existingSession == null) "NEW SESSION" else "EDIT SESSION",
                style = SSHFTPTheme.typography.h2,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NeoTextField(value = name, onValueChange = { name = it }, label = "Session Name",
                    placeholder = "My Server", leadingIcon = Icons.Default.Label)
                NeoTextField(value = host, onValueChange = { host = it }, label = "Hostname / IP",
                    placeholder = "192.168.1.1", leadingIcon = Icons.Default.Dns)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeoTextField(value = port, onValueChange = { port = it }, label = "Port",
                        placeholder = "22", modifier = Modifier.width(90.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    NeoTextField(value = user, onValueChange = { user = it }, label = "Username",
                        placeholder = "root", modifier = Modifier.weight(1f),
                        leadingIcon = Icons.Default.Person)
                }
                NeoTextField(value = pass, onValueChange = { pass = it }, label = "Password",
                    placeholder = "••••••••", isPassword = true,
                    leadingIcon = Icons.Default.Lock)
                Text(
                    "Password is encrypted with Android Keystore.",
                    style = SSHFTPTheme.typography.bodySmall,
                    color = SSHFTPTheme.colors.textSecondary,
                )
            }
        },
        confirmButton = {
            NeoButton(
                text = "Save",
                onClick = {
                    if (host.isNotBlank() && user.isNotBlank()) {
                        onSave(
                            name.ifBlank { host },
                            host, port.toIntOrNull() ?: 22,
                            user, pass,
                        )
                    }
                },
                icon = Icons.Default.Save,
                modifier = Modifier.wrapContentSize(),
            )
        },
        dismissButton = {
            NeoButton(
                "Cancel", onDismiss,
                backgroundColor = Color(0xFFEEEEEE),
                textColor = NeoColors.Black,
                modifier = Modifier.wrapContentSize(),
            )
        },
    )
}

private fun formatTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val mins = diff / 60_000
    val hours = mins / 60
    val days = hours / 24
    return when {
        days > 0  -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        mins > 0  -> "${mins}m ago"
        else      -> "Just now"
    }
}
