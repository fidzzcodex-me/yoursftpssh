package com.fidzzcodex.sshftp.data.repository

import com.fidzzcodex.sshftp.data.db.SessionDao
import com.fidzzcodex.sshftp.data.model.SSHSession
import com.fidzzcodex.sshftp.util.CryptoManager

class SessionRepository(private val dao: SessionDao) {

    suspend fun getAllSessions(): List<SSHSession> = dao.getAllSessions()

    suspend fun saveSession(
        name: String,
        host: String,
        port: Int,
        username: String,
        password: String,
    ): Long {
        val session = SSHSession(
            name              = name,
            host              = host,
            port              = port,
            username          = username,
            encryptedPassword = CryptoManager.encrypt(password),
        )
        return dao.insertSession(session)
    }

    suspend fun updateSession(session: SSHSession, newPassword: String? = null): Unit {
        val updated = if (newPassword != null) {
            session.copy(encryptedPassword = CryptoManager.encrypt(newPassword))
        } else session
        dao.updateSession(updated)
    }

    suspend fun deleteSession(session: SSHSession) = dao.deleteSession(session)

    suspend fun getSessionById(id: Long): SSHSession? = dao.getSessionById(id)

    fun decryptPassword(session: SSHSession): String =
        CryptoManager.decrypt(session.encryptedPassword)

    suspend fun markConnected(id: Long) =
        dao.updateLastConnected(id, System.currentTimeMillis())
}
