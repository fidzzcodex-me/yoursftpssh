package com.fidzzcodex.sshftp.ssh

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fidzzcodex.sshftp.MainActivity
import com.fidzzcodex.sshftp.R

class SSHService : Service() {

    companion object {
        const val CHANNEL_ID   = "ssh_ftp_channel"
        const val NOTIF_ID     = 1001
        const val ACTION_STOP  = "com.fidzzcodex.sshftp.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val host = intent?.getStringExtra("host") ?: "server"
        startForeground(NOTIF_ID, buildNotification(host))
        return START_STICKY
    }

    private fun buildNotification(host: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = Intent(this, SSHService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SSH Connected")
            .setContentText("Connected to $host")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", stopPending)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SSH Connection",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows active SSH connection status"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
