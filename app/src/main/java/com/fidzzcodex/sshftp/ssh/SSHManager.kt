package com.fidzzcodex.sshftp.ssh

import com.fidzzcodex.sshftp.data.model.RemoteFile
import com.fidzzcodex.sshftp.data.model.SSHSession
import com.jcraft.jsch.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties
import java.util.Vector

data class SSHConnectionState(
    val isConnected: Boolean = false,
    val host: String = "",
    val username: String = "",
    val error: String? = null,
)

class SSHManager {
    private var jschSession: com.jcraft.jsch.Session? = null
    private var sftpChannel: ChannelSftp? = null
    private var shellChannel: ChannelShell? = null

    private val _connectionState = MutableStateFlow(SSHConnectionState())
    val connectionState: StateFlow<SSHConnectionState> = _connectionState

    // ── Connect ──────────────────────────────────────────────────────────────
    suspend fun connect(session: SSHSession, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val jsch = JSch()
                val js = jsch.getSession(session.username, session.host, session.port)
                js.setPassword(password)

                val config = Properties()
                config["StrictHostKeyChecking"] = "no"
                config["PreferredAuthentications"] = "password"
                config["ConnectTimeout"] = "10000"
                js.setConfig(config)

                js.connect(15_000)
                jschSession = js

                _connectionState.value = SSHConnectionState(
                    isConnected = true,
                    host = session.host,
                    username = session.username,
                )
                Result.success(Unit)
            } catch (e: JSchException) {
                _connectionState.value = SSHConnectionState(error = e.message)
                Result.failure(e)
            }
        }

    // ── Disconnect ───────────────────────────────────────────────────────────
    fun disconnect() {
        runCatching {
            sftpChannel?.disconnect()
            shellChannel?.disconnect()
            jschSession?.disconnect()
        }
        sftpChannel = null
        shellChannel = null
        jschSession = null
        _connectionState.value = SSHConnectionState(isConnected = false)
    }

    val isConnected get() = jschSession?.isConnected == true

    // ── SFTP: list directory ─────────────────────────────────────────────────
    suspend fun listDirectory(path: String): Result<List<RemoteFile>> =
        withContext(Dispatchers.IO) {
            try {
                val sftp = getSftp()
                @Suppress("UNCHECKED_CAST")
                val entries = sftp.ls(path) as Vector<ChannelSftp.LsEntry>
                val files = entries
                    .filter { it.filename != "." }
                    .map { entry ->
                        val attrs = entry.attrs
                        RemoteFile(
                            name           = entry.filename,
                            fullPath       = if (path.endsWith("/")) "$path${entry.filename}"
                                            else "$path/${entry.filename}",
                            isDirectory    = attrs.isDir,
                            size           = attrs.size,
                            permissions    = attrs.permissionsString,
                            permissionsOctal = attrs.permissions.toString(8).takeLast(4),
                            owner          = attrs.uidString,
                            group          = attrs.gidString,
                            modifiedTime   = attrs.mTime.toLong() * 1000L,
                        )
                    }
                    .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                Result.success(files)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ── SFTP: upload ─────────────────────────────────────────────────────────
    suspend fun uploadFile(
        localPath: String,
        remotePath: String,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp  = getSftp()
            val file  = File(localPath)
            val total = file.length()
            var transferred = 0L

            sftp.put(localPath, remotePath, object : SftpProgressMonitor {
                override fun init(op: Int, src: String?, dest: String?, max: Long) {}
                override fun count(count: Long): Boolean {
                    transferred += count
                    onProgress(transferred, total)
                    return true
                }
                override fun end() {}
            })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── SFTP: download ───────────────────────────────────────────────────────
    suspend fun downloadFile(
        remotePath: String,
        localPath: String,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = getSftp()
            val attrs = sftp.lstat(remotePath)
            val total = attrs.size
            var transferred = 0L

            sftp.get(remotePath, localPath, object : SftpProgressMonitor {
                override fun init(op: Int, src: String?, dest: String?, max: Long) {}
                override fun count(count: Long): Boolean {
                    transferred += count
                    onProgress(transferred, total)
                    return true
                }
                override fun end() {}
            })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── SFTP: operations ─────────────────────────────────────────────────────
    suspend fun deleteFile(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = getSftp()
            val attrs = sftp.lstat(remotePath)
            if (attrs.isDir) sftp.rmdir(remotePath) else sftp.rm(remotePath)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun renameFile(oldPath: String, newPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try { getSftp().rename(oldPath, newPath); Result.success(Unit) }
            catch (e: Exception) { Result.failure(e) }
        }

    suspend fun createDirectory(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try { getSftp().mkdir(path); Result.success(Unit) }
        catch (e: Exception) { Result.failure(e) }
    }

    suspend fun chmod(path: String, permissions: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try { getSftp().chmod(permissions, path); Result.success(Unit) }
            catch (e: Exception) { Result.failure(e) }
        }

    // ── Shell channel ────────────────────────────────────────────────────────
    fun openShell(): Pair<InputStream, OutputStream>? {
        return try {
            val channel = jschSession!!.openChannel("shell") as ChannelShell
            channel.setPtyType("xterm-256color")
            channel.setPtySize(220, 50, 1920, 1080)
            val inputStream  = channel.inputStream
            val outputStream = channel.outputStream
            channel.connect()
            shellChannel = channel
            Pair(inputStream, outputStream)
        } catch (e: Exception) { null }
    }

    fun resizeTerminal(cols: Int, rows: Int) {
        shellChannel?.setPtySize(cols, rows, cols * 8, rows * 16)
    }

    fun closeShell() {
        shellChannel?.disconnect()
        shellChannel = null
    }

    // ── Execute command ──────────────────────────────────────────────────────
    suspend fun executeCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val channel = jschSession!!.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            val stream = channel.inputStream
            channel.connect()
            val output = stream.bufferedReader().readText()
            channel.disconnect()
            Result.success(output)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Private helpers ──────────────────────────────────────────────────────
    private fun getSftp(): ChannelSftp {
        val existing = sftpChannel
        if (existing != null && existing.isConnected) return existing
        val channel = jschSession!!.openChannel("sftp") as ChannelSftp
        channel.connect()
        sftpChannel = channel
        return channel
    }
}

// Allow multiple simultaneous sessions (max 5 tabs)
class SSHTabManager {
    private val managers = mutableMapOf<Int, SSHManager>()

    fun getOrCreate(tabIndex: Int): SSHManager =
        managers.getOrPut(tabIndex) { SSHManager() }

    fun get(tabIndex: Int): SSHManager? = managers[tabIndex]

    fun close(tabIndex: Int) {
        managers[tabIndex]?.disconnect()
        managers.remove(tabIndex)
    }

    fun closeAll() {
        managers.values.forEach { it.disconnect() }
        managers.clear()
    }

    val activeTabs: List<Int> get() = managers.keys.toList()
}
