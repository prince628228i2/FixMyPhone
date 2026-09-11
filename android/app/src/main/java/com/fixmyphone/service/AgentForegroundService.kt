package com.fixmyphone.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fixmyphone.R

class AgentForegroundService : Service() {
    companion object {
        const val ACTION_START = "com.fixmyphone.START_ASSISTANT"
        const val ACTION_STOP = "com.fixmyphone.STOP_ASSISTANT"
        const val EXTRA_MODE = "mode"
        private const val CHANNEL_ID = "fixmyphone_assistant"
        private const val NOTIFICATION_ID = 7001
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Fix My Phone Assistant", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: "fix"
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Fix My Phone")
            .setContentText(if (mode == "24x7") "24×7 AI is ready for your voice" else "Phone Fix is listening")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        else startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
