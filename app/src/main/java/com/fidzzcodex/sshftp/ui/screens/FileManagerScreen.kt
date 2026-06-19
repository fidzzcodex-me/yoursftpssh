package com.fidzzcodex.sshftp.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fidzzcodex.sshftp.data.model.*
import com.fidzzcodex.sshftp.ui.AppViewModel
import com.fidzzcodex.sshftp.ui.components.*
import com.fidzzcodex.sshftp.ui.theme.NeoColors
import com.fidzzcodex.sshftp.ui.theme.SSHFTPTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FileManagerScreen(vm: AppViewModel) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = getRealPathFromUri(context, uri)
            path?.let { vm.uploadFile(it) }
        }
    }

    if (!state.isConnected) {
        NotConnectedBanner()
        return
    }

    Column(Modifier.fillMaxSize().background(SSHFTPTheme.colors.background)) {
        // Transfer progress bar
        val activeTransfer = state.transfers.lastOrNull { it.status == TransferStatus.IN_PROGRESS }
        if (activeTransfer != null) {
            TransferProgressBar(activeTransfer)
        }

        // Sort row
        SortBar(state.sortBy, state.sortOrder, vm::setSortBy)

        // Dual pane
        Row(Modifier.weight(1f)) {
            // LOCAL pane
            FilePane(
                title = "LOCAL",
                path = state.currentLocalPath,
                modifier = Modifier.weight(1f),
                onNavigateUp = { vm.navigateLocalUp() },
                headerColor = NeoColors.Yellow,
            ) {
                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NeoLoadingSpinner(NeoColors.Yellow)
                    }
                } else {
                    LazyColumn {
                        items(state.localFiles, key = { it.absolutePath }) { file ->
                            LocalFileRow(
                                file = file,
                                onTap = {
                                    if (file.isDirectory) vm.loadLocalFiles(file.absolutePath)
                                },
                                onUpload = {
                                    if (!file.isDirectory) vm.uploadFile(file.absolutePath)
                                },
                            )
                        }
                    }
                }
            }

            // Divider
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(NeoColors.Black)
            )

            // REMOTE pane
            FilePane(
                title = "REMOTE",
                path = state.currentRemotePath,
                modifier = Modifier.weight(1f),
                onNavigateUp = { vm.navigateRemoteUp() },
                headerColor = NeoColors.Blue,
                actions = {
                    SmallIconButton(Icons.Default.CreateNewFolder, "New Folder", NeoColors.Blue, Color.White) {
                        // handled below via state
                    }
                    SmallIconButton(Icons.Default.Upload, "Upload", NeoColors.Yellow, NeoColors.Black) {
                        filePicker.launch("*/*")
                    }
                    SmallIconButton(Icons.Default.Refresh, "Refresh", Color(0xFFEEEEEE), NeoColors.Black) {
                        vm.loadRemoteFiles(state.currentRemotePath)
                    }
                },
            ) {
                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NeoLoadingSpinner(NeoColors.Blue)
                    }
                } else {
                    LazyColumn {
                        items(state.remoteFiles, key = { it.fullPath }) { file ->
                            RemoteFileRow(
                                file = file,
                                onTap = {
                                    if (file.isDirectory) vm.loadRemoteFiles(file.fullPath)
                                },
                                onDownload = { vm.downloadFile(file) },
                                onDelete = { vm.deleteRemoteFile(file) },
                                onRename = { newName -> vm.renameRemoteFile(file, newName) },
                                onChmod = { octal -> vm.chmodRemoteFile(file, octal) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilePane(
    title: String,
    path: String,
    modifier: Modifier,
    headerColor: Color,
    onNavigateUp: () -> Unit,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxHeight()) {
        // Pane header
        Column(
            Modifier
                .fillMaxWidth()
                .background(headerColor)
                .border(SSHFTPTheme.dimensions.borderWidth, NeoColors.Black)
                .padding(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = SSHFTPTheme.typography.label, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    actions?.invoke(this)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onNavigateUp() },
            ) {
                Icon(Icons.Default.ArrowUpward, "Up", tint = Color.White.copy(.7f), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(2.dp))
                Text(
                    path,
                    style = SSHFTPTheme.typography.monoSmall,
                    color = Color.White.copy(.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
fun LocalFileRow(file: File, onTap: () -> Unit, onUpload: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            null,
            tint = if (file.isDirectory) NeoColors.Yellow else SSHFTPTheme.colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name, style = SSHFTPTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!file.isDirectory) {
                Text(formatSize(file.length()), style = SSHFTPTheme.typography.monoSmall,
                    color = SSHFTPTheme.colors.textSecondary)
            }
        }
        if (!file.isDirectory) {
            IconButton(onClick = onUpload, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Upload, "Upload", tint = NeoColors.Blue, modifier = Modifier.size(16.dp))
            }
        }
        Divider(color = NeoColors.Black.copy(.1f), thickness = 0.5.dp)
    }
}

@Composable
fun RemoteFileRow(
    file: RemoteFile,
    onTap: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onChmod: (Int) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showChmodDialog by remember { mutableStateOf(false) }

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = { showMenu = true },
                    )
                }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                null,
                tint = if (file.isDirectory) NeoColors.Blue else SSHFTPTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, style = SSHFTPTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${file.permissionsOctal} ${if (!file.isDirectory) formatSize(file.size) else ""}",
                    style = SSHFTPTheme.typography.monoSmall,
                    color = SSHFTPTheme.colors.textSecondary,
                )
            }
            if (!file.isDirectory) {
                IconButton(onClick = onDownload, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Download, "Download", tint = NeoColors.Blue, modifier = Modifier.size(16.dp))
                }
            }
        }
        Divider(color = NeoColors.Black.copy(.1f), thickness = 0.5.dp)
    }

    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        DropdownMenuItem(text = { Text("Download") }, leadingIcon = { Icon(Icons.Default.Download, null) },
            onClick = { onDownload(); showMenu = false })
        DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Default.Edit, null) },
            onClick = { showRenameDialog = true; showMenu = false })
        DropdownMenuItem(text = { Text("Permissions (chmod)") }, leadingIcon = { Icon(Icons.Default.Lock, null) },
            onClick = { showChmodDialog = true; showMenu = false })
        Divider()
        DropdownMenuItem(text = { Text("Delete", color = NeoColors.Red) }, leadingIcon = {
            Icon(Icons.Default.Delete, null, tint = NeoColors.Red)
        }, onClick = { onDelete(); showMenu = false })
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(file.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename") },
            text = {
                NeoTextField(value = newName, onValueChange = { newName = it }, label = "New name",
                    leadingIcon = Icons.Default.Edit)
            },
            confirmButton = {
                NeoButton("Rename", { onRename(newName); showRenameDialog = false },
                    modifier = Modifier.wrapContentSize())
            },
            dismissButton = {
                NeoButton("Cancel", { showRenameDialog = false }, backgroundColor = Color(0xFFEEEEEE),
                    textColor = NeoColors.Black, modifier = Modifier.wrapContentSize())
            },
        )
    }

    if (showChmodDialog) {
        var octal by remember { mutableStateOf(file.permissionsOctal.takeLast(3)) }
        AlertDialog(
            onDismissRequest = { showChmodDialog = false },
            title = { Text("Chmod") },
            text = {
                Column {
                    Text("Current: ${file.permissions}", style = SSHFTPTheme.typography.mono)
                    Spacer(Modifier.height(8.dp))
                    NeoTextField(value = octal, onValueChange = { octal = it }, label = "Octal (e.g. 755)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                NeoButton("Apply", { onChmod(octal.toIntOrNull(8) ?: 644); showChmodDialog = false },
                    modifier = Modifier.wrapContentSize())
            },
            dismissButton = {
                NeoButton("Cancel", { showChmodDialog = false }, backgroundColor = Color(0xFFEEEEEE),
                    textColor = NeoColors.Black, modifier = Modifier.wrapContentSize())
            },
        )
    }
}

@Composable
fun TransferProgressBar(job: TransferJob) {
    val progress = if (job.totalBytes > 0) job.transferredBytes.toFloat() / job.totalBytes else 0f
    Column(
        Modifier
            .fillMaxWidth()
            .background(NeoColors.Black)
            .padding(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${if (job.type == TransferType.UPLOAD) "UPLOADING" else "DOWNLOADING"} ${job.fileName}",
                style = SSHFTPTheme.typography.label, color = Color.White,
            )
            Text(
                "${(progress * 100).toInt()}%",
                style = SSHFTPTheme.typography.label, color = NeoColors.Yellow,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = NeoColors.Yellow,
            trackColor = Color.White.copy(.2f),
        )
        Text(
            "${formatSize(job.transferredBytes)} / ${formatSize(job.totalBytes)}",
            style = SSHFTPTheme.typography.monoSmall, color = Color.White.copy(.7f),
        )
    }
}

@Composable
fun SortBar(sortBy: SortBy, sortOrder: SortOrder, onSort: (SortBy, SortOrder) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(SSHFTPTheme.colors.surface)
            .border(1.dp, NeoColors.Black.copy(.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("SORT:", style = SSHFTPTheme.typography.label, color = SSHFTPTheme.colors.textSecondary)
        listOf(SortBy.NAME, SortBy.SIZE, SortBy.DATE).forEach { sort ->
            val selected = sort == sortBy
            Box(
                modifier = Modifier
                    .background(if (selected) NeoColors.Blue else Color.Transparent)
                    .border(1.dp, NeoColors.Black)
                    .clickable { onSort(sort, if (selected && sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC) }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    "${sort.name}${if (selected) if (sortOrder == SortOrder.ASC) " ↑" else " ↓" else ""}",
                    style = SSHFTPTheme.typography.label,
                    color = if (selected) Color.White else SSHFTPTheme.colors.text,
                )
            }
        }
    }
}

@Composable
fun NotConnectedBanner() {
    Box(Modifier.fillMaxSize().background(SSHFTPTheme.colors.background), contentAlignment = Alignment.Center) {
        NeoCard(backgroundColor = NeoColors.Red.copy(.08f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.WifiOff, null, tint = NeoColors.Red, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("NOT CONNECTED", style = SSHFTPTheme.typography.h2, color = NeoColors.Red)
                Text("Go to Sessions tab to connect first",
                    style = SSHFTPTheme.typography.body, color = SSHFTPTheme.colors.textSecondary)
            }
        }
    }
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024         -> "%.1f KB".format(bytes / 1_024.0)
    else                   -> "$bytes B"
}

fun getRealPathFromUri(context: android.content.Context, uri: Uri): String? {
    return try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val name = it.getString(idx)
                val file = File(context.cacheDir, name)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    file.outputStream().use { out -> stream.copyTo(out) }
                }
                file.absolutePath
            } else null
        }
    } catch (e: Exception) { null }
}
