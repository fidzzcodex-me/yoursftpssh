package com.fidzzcodex.sshftp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── SSH Session (persisted) ──────────────────────────────────────────────────
@Entity(tableName = "ssh_sessions")
data class SSHSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val encryptedPassword: String,  // AES via Android Keystore
    val lastConnected: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
)

// ─── Active connection (runtime only) ────────────────────────────────────────
data class ActiveConnection(
    val session: SSHSession,
    val tabIndex: Int,
    val isConnected: Boolean = false,
    val currentRemotePath: String = "/",
    val currentLocalPath: String = "/sdcard",
)

// ─── Remote file entry ────────────────────────────────────────────────────────
data class RemoteFile(
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val size: Long,
    val permissions: String,   // e.g. "rwxr-xr-x"
    val permissionsOctal: String, // e.g. "755"
    val owner: String,
    val group: String,
    val modifiedTime: Long,
)

// ─── Transfer job ─────────────────────────────────────────────────────────────
data class TransferJob(
    val id: String,
    val fileName: String,
    val totalBytes: Long,
    val transferredBytes: Long = 0L,
    val type: TransferType,
    val status: TransferStatus = TransferStatus.PENDING,
)

enum class TransferType { UPLOAD, DOWNLOAD }

enum class TransferStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED }

// ─── App event (one-shot UI events) ──────────────────────────────────────────
sealed class AppEvent {
    data class ShowToast(val message: String) : AppEvent()
    data class ShowError(val message: String) : AppEvent()
    object ConnectionLost : AppEvent()
}

// ─── File sort options ────────────────────────────────────────────────────────
enum class SortBy { NAME, SIZE, DATE }
enum class SortOrder { ASC, DESC }
