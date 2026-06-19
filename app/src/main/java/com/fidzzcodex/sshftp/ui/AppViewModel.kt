package com.fidzzcodex.sshftp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fidzzcodex.sshftp.data.db.AppDatabase
import com.fidzzcodex.sshftp.data.model.*
import com.fidzzcodex.sshftp.data.repository.SessionRepository
import com.fidzzcodex.sshftp.ssh.SSHManager
import com.fidzzcodex.sshftp.ssh.SSHTabManager
import com.fidzzcodex.sshftp.util.CryptoManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class MainUiState(
    val sessions: List<SSHSession> = emptyList(),
    val activeTabIndex: Int = 0,
    val isLoading: Boolean = false,
    val currentRemotePath: String = "/",
    val currentLocalPath: String = "/sdcard",
    val remoteFiles: List<RemoteFile> = emptyList(),
    val localFiles: List<java.io.File> = emptyList(),
    val transfers: List<TransferJob> = emptyList(),
    val sortBy: SortBy = SortBy.NAME,
    val sortOrder: SortOrder = SortOrder.ASC,
    val isConnected: Boolean = false,
    val connectedHost: String = "",
    val connectedUser: String = "",
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db: AppDatabase by lazy {
        AppDatabase.getInstance(app, CryptoManager.getDbPassphrase())
    }
    private val repo: SessionRepository by lazy {
        SessionRepository(db.sessionDao())
    }
    val tabManager = SSHTabManager()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = Channel<AppEvent>(Channel.BUFFERED)
    val events: Flow<AppEvent> = _events.receiveAsFlow()

    init {
        loadSessions()
        loadLocalFiles(_uiState.value.currentLocalPath)
    }

    // ── Sessions ─────────────────────────────────────────────────────────────
    fun loadSessions() = viewModelScope.launch {
        val sessions = repo.getAllSessions()
        _uiState.update { it.copy(sessions = sessions) }
    }

    fun saveSession(name: String, host: String, port: Int, username: String, password: String) =
        viewModelScope.launch {
            repo.saveSession(name, host, port, username, password)
            loadSessions()
            _events.send(AppEvent.ShowToast("Session '$name' saved"))
        }

    fun deleteSession(session: SSHSession) = viewModelScope.launch {
        repo.deleteSession(session)
        loadSessions()
    }

    // ── Connection ───────────────────────────────────────────────────────────
    fun connect(session: SSHSession, tabIndex: Int = 0) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val manager = tabManager.getOrCreate(tabIndex)
        val password = repo.decryptPassword(session)
        val result = manager.connect(session, password)
        if (result.isSuccess) {
            repo.markConnected(session.id)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isConnected = true,
                    connectedHost = session.host,
                    connectedUser = session.username,
                    activeTabIndex = tabIndex,
                )
            }
            loadRemoteFiles("/")
            _events.send(AppEvent.ShowToast("Connected to ${session.host}"))
        } else {
            _uiState.update { it.copy(isLoading = false) }
            _events.send(AppEvent.ShowError("Connection failed: ${result.exceptionOrNull()?.message}"))
        }
    }

    fun disconnect(tabIndex: Int = _uiState.value.activeTabIndex) {
        tabManager.close(tabIndex)
        _uiState.update {
            it.copy(
                isConnected = false,
                connectedHost = "",
                connectedUser = "",
                remoteFiles = emptyList(),
                currentRemotePath = "/",
            )
        }
    }

    // ── File Manager ─────────────────────────────────────────────────────────
    fun loadRemoteFiles(path: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val manager = tabManager.get(_uiState.value.activeTabIndex) ?: return@launch
        val result = manager.listDirectory(path)
        if (result.isSuccess) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentRemotePath = path,
                    remoteFiles = sortFiles(result.getOrThrow(), it.sortBy, it.sortOrder),
                )
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
            _events.send(AppEvent.ShowError("Failed to list: ${result.exceptionOrNull()?.message}"))
        }
    }

    fun loadLocalFiles(path: String) {
        val dir = File(path)
        val files = dir.listFiles()?.toList() ?: emptyList()
        _uiState.update {
            it.copy(
                currentLocalPath = path,
                localFiles = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })),
            )
        }
    }

    fun navigateRemoteUp() {
        val current = _uiState.value.currentRemotePath
        if (current == "/") return
        val parent = current.substringBeforeLast("/").ifEmpty { "/" }
        loadRemoteFiles(parent)
    }

    fun navigateLocalUp() {
        val current = File(_uiState.value.currentLocalPath)
        current.parentFile?.let { loadLocalFiles(it.absolutePath) }
    }

    fun deleteRemoteFile(file: RemoteFile) = viewModelScope.launch {
        val manager = tabManager.get(_uiState.value.activeTabIndex) ?: return@launch
        val result = manager.deleteFile(file.fullPath)
        if (result.isSuccess) {
            _events.send(AppEvent.ShowToast("Deleted ${file.name}"))
            loadRemoteFiles(_uiState.value.currentRemotePath)
        } else {
            _events.send(AppEvent.ShowError("Delete failed"))
        }
    }

    fun renameRemoteFile(file: RemoteFile, newName: String) = viewModelScope.launch {
        val manager = tabManager.get(_uiState.value.activeTabIndex) ?: return@launch
        val newPath = "${_uiState.value.currentRemotePath.trimEnd('/')}/$newName"
        val result = manager.renameFile(file.fullPath, newPath)
        if (result.isSuccess) {
            _events.send(AppEvent.ShowToast("Renamed to $newName"))
            loadRemoteFiles(_uiState.value.currentRemotePath)
        } else {
            _events.send(AppEvent.ShowError("Rename failed"))
        }
    }

    fun createRemoteDirectory(name: String) = viewModelScope.launch {
        val manager = tabManager.get(_uiState.value.activeTabIndex) ?: return@launch
        val path = "${_uiState.value.currentRemotePath.trimEnd('/')}/$name"
        val result = manager.createDirectory(path)
        if (result.isSuccess) {
            loadRemoteFiles(_uiState.value.currentRemotePath)
        } else {
            _events.send(AppEvent.ShowError("Failed to create folder"))
        }
    }

    fun chmodRemoteFile(file: RemoteFile, octal: Int) = viewModelScope.launch {
        val manager = tabManager.get(_uiState.value.activeTabIndex) ?: return@launch
        manager.chmod(file.fullPath, octal)
        loadRemoteFiles(_uiState.value.currentRemotePath)
    }

    fun uploadFile(localPath: String) = viewModelScope.launch {
        val manager = tabManager.get(_uiState.value.activeTabIndex) ?: return@launch
        val fileName = File(localPath).name
        val remotePath = "${_uiState.value.currentRemotePath.trimEnd('/')}/$fileName"
        val jobId = System.currentTimeMillis().toString()
        val job = TransferJob(
            id = jobId, fileName = fileName,
            totalBytes = File(localPath).length(), type = TransferType.UPLOAD,
            status = TransferStatus.IN_PROGRESS,
        )
        addTransfer(job)
        val result = manager.uploadFile(localPath, remotePath) { transferred, total ->
            updateTransfer(jobId, transferred, total)
        }
        updateTransferStatus(jobId, if (result.isSuccess) TransferStatus.COMPLETED else TransferStatus.FAILED)
        if (result.isSuccess) {
            _events.send(AppEvent.ShowToast("Uploaded $fileName"))
            loadRemoteFiles(_uiState.value.currentRemotePath)
        } else {
            _events.send(AppEvent.ShowError("Upload failed"))
        }
    }

    fun downloadFile(remoteFile: RemoteFile) = viewModelScope.launch {
        val manager = tabManager.get(_uiState.value.activeTabIndex) ?: return@launch
        val localPath = "${_uiState.value.currentLocalPath.trimEnd('/')}/${remoteFile.name}"
        val jobId = System.currentTimeMillis().toString()
        val job = TransferJob(
            id = jobId, fileName = remoteFile.name, totalBytes = remoteFile.size,
            type = TransferType.DOWNLOAD, status = TransferStatus.IN_PROGRESS,
        )
        addTransfer(job)
        val result = manager.downloadFile(remoteFile.fullPath, localPath) { transferred, total ->
            updateTransfer(jobId, transferred, total)
        }
        updateTransferStatus(jobId, if (result.isSuccess) TransferStatus.COMPLETED else TransferStatus.FAILED)
        if (result.isSuccess) {
            _events.send(AppEvent.ShowToast("Downloaded ${remoteFile.name}"))
            loadLocalFiles(_uiState.value.currentLocalPath)
        } else {
            _events.send(AppEvent.ShowError("Download failed"))
        }
    }

    // ── Sort ──────────────────────────────────────────────────────────────────
    fun setSortBy(sortBy: SortBy, order: SortOrder = SortOrder.ASC) {
        _uiState.update {
            it.copy(
                sortBy = sortBy,
                sortOrder = order,
                remoteFiles = sortFiles(it.remoteFiles, sortBy, order),
            )
        }
    }

    private fun sortFiles(files: List<RemoteFile>, sortBy: SortBy, order: SortOrder): List<RemoteFile> {
        val sorted = when (sortBy) {
            SortBy.NAME -> files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            SortBy.SIZE -> files.sortedWith(compareBy({ !it.isDirectory }, { it.size }))
            SortBy.DATE -> files.sortedWith(compareBy({ !it.isDirectory }, { it.modifiedTime }))
        }
        return if (order == SortOrder.DESC) sorted.reversed() else sorted
    }

    // ── Transfer helpers ──────────────────────────────────────────────────────
    private fun addTransfer(job: TransferJob) =
        _uiState.update { it.copy(transfers = it.transfers + job) }

    private fun updateTransfer(id: String, transferred: Long, total: Long) =
        _uiState.update {
            it.copy(transfers = it.transfers.map { j ->
                if (j.id == id) j.copy(transferredBytes = transferred, totalBytes = total) else j
            })
        }

    private fun updateTransferStatus(id: String, status: TransferStatus) =
        _uiState.update {
            it.copy(transfers = it.transfers.map { j ->
                if (j.id == id) j.copy(status = status) else j
            })
        }

    fun getSSHManager(tabIndex: Int = _uiState.value.activeTabIndex): SSHManager? =
        tabManager.get(tabIndex)

    override fun onCleared() {
        super.onCleared()
        tabManager.closeAll()
    }
}
