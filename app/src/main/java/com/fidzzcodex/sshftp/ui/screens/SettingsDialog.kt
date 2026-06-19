package com.fidzzcodex.sshftp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fidzzcodex.sshftp.ui.AppViewModel
import com.fidzzcodex.sshftp.ui.components.NeoButton
import com.fidzzcodex.sshftp.ui.components.NeoCard
import com.fidzzcodex.sshftp.ui.theme.NeoColors
import com.fidzzcodex.sshftp.ui.theme.SSHFTPTheme

@Composable
fun SettingsDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val state by vm.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("SETTINGS", style = SSHFTPTheme.typography.h2, color = SSHFTPTheme.colors.text)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Connection status section
                NeoCard(backgroundColor = if (state.isConnected) NeoColors.Green.copy(.08f) else Color(0xFFF5F5F5)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("CONNECTION", style = SSHFTPTheme.typography.label,
                                color = SSHFTPTheme.colors.textSecondary)
                            if (state.isConnected) {
                                Text("${state.connectedUser}@${state.connectedHost}",
                                    style = SSHFTPTheme.typography.mono, color = NeoColors.Blue)
                            } else {
                                Text("Not connected", style = SSHFTPTheme.typography.body,
                                    color = SSHFTPTheme.colors.textSecondary)
                            }
                        }
                        if (state.isConnected) {
                            NeoButton(
                                text = "Disconnect",
                                onClick = { vm.disconnect(); onDismiss() },
                                backgroundColor = NeoColors.Red,
                                modifier = Modifier.wrapContentSize(),
                            )
                        }
                    }
                }

                // Active tabs
                NeoCard {
                    Text("ACTIVE TABS", style = SSHFTPTheme.typography.label,
                        color = SSHFTPTheme.colors.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    val activeTabs = vm.tabManager.activeTabs
                    if (activeTabs.isEmpty()) {
                        Text("No active tabs", style = SSHFTPTheme.typography.body,
                            color = SSHFTPTheme.colors.textSecondary)
                    } else {
                        activeTabs.forEach { tab ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Tab ${tab + 1}", style = SSHFTPTheme.typography.body)
                                NeoButton(
                                    text = "Close",
                                    onClick = { vm.disconnect(tab) },
                                    backgroundColor = NeoColors.Red,
                                    modifier = Modifier.wrapContentSize(),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }

                // App info
                NeoCard(backgroundColor = Color(0xFFF0F4FF)) {
                    Text("ABOUT", style = SSHFTPTheme.typography.label,
                        color = SSHFTPTheme.colors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text("SSH + SFTP Client", style = SSHFTPTheme.typography.h3)
                    Text("Version 1.0.0", style = SSHFTPTheme.typography.body,
                        color = SSHFTPTheme.colors.textSecondary)
                    Text("by fidzzcodex", style = SSHFTPTheme.typography.bodySmall,
                        color = NeoColors.Blue)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "All credentials are encrypted with Android Keystore. No data is sent to external servers.",
                        style = SSHFTPTheme.typography.bodySmall,
                        color = SSHFTPTheme.colors.textSecondary,
                    )
                }
            }
        },
        confirmButton = {
            NeoButton(
                text = "Close",
                onClick = onDismiss,
                icon = Icons.Default.Close,
                modifier = Modifier.wrapContentSize(),
            )
        },
    )
}
