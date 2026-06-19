package com.fidzzcodex.sshftp.data.db

import android.content.Context
import androidx.room.*
import com.fidzzcodex.sshftp.data.model.SSHSession
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Dao
interface SessionDao {
    @Query("SELECT * FROM ssh_sessions ORDER BY lastConnected DESC")
    suspend fun getAllSessions(): List<SSHSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SSHSession): Long

    @Update
    suspend fun updateSession(session: SSHSession)

    @Delete
    suspend fun deleteSession(session: SSHSession)

    @Query("SELECT * FROM ssh_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): SSHSession?

    @Query("UPDATE ssh_sessions SET lastConnected = :time WHERE id = :id")
    suspend fun updateLastConnected(id: Long, time: Long)
}

@Database(
    entities = [SSHSession::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportFactory(passphrase)
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sshftp_secure.db",
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
