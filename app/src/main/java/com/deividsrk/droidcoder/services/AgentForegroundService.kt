package com.deividsrk.droidcoder.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.deividsrk.droidcoder.MainActivity
import com.deividsrk.droidcoder.R

class AgentForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "agent_running_channel"
        private const val ACTION_START = "ACTION_START"
        private const val ACTION_UPDATE = "ACTION_UPDATE"
        private const val ACTION_STOP = "ACTION_STOP"
        
        private const val EXTRA_STATUS = "EXTRA_STATUS"

        fun start(context: Context, status: String) {
            val intent = Intent(context, AgentForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_STATUS, status)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun update(context: Context, status: String) {
            val intent = Intent(context, AgentForegroundService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_STATUS, status)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AgentForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val status = intent?.getStringExtra(EXTRA_STATUS) ?: "IA em execução..."

        when (action) {
            ACTION_START -> {
                showNotification(status)
            }
            ACTION_UPDATE -> {
                showNotification(status)
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun showNotification(statusText: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DroidCoder2 - IA em Execução")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Execução do Agente de IA",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mostra o progresso de execução e ferramentas usadas pelo DroidCoder2"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
