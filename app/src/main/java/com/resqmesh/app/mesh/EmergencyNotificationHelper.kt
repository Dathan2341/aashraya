package com.resqmesh.app.mesh

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat

object EmergencyNotificationHelper {
    private const val CHANNEL_ID = "resqmesh_emergency_alerts"
    private const val CHANNEL_NAME = "Emergency Alerts"

    fun showEmergencyNotification(context: Context, message: MeshMessage) {
        if (!isEmergencyType(message.type)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (notificationManager == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency distress notifications from nearby devices"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = when (message.type) {
            MeshMessage.TYPE_TRAPPED -> "🚨 Trapped under debris"
            MeshMessage.TYPE_STARVING -> "🍞 Urgent supply request"
            MeshMessage.TYPE_MEDICAL -> "🩺 Urgent medical help needed"
            else -> "🚨 Distress signal received"
        }

        val activityIntent = Intent(context, EmergencyAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("title", title)
            putExtra("body", "From ${message.sender}: ${message.text}")
        }

        val alertPendingIntent = PendingIntent.getActivity(
            context,
            message.id.hashCode(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText("From ${message.sender}: ${message.text}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("From ${message.sender}: ${message.text}"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(alertPendingIntent, true)
            .setAutoCancel(true)
            .setContentIntent(alertPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setLights(0xFFEF4444.toInt(), 1000, 1000)
            .setOnlyAlertOnce(false)
            .build()

        notificationManager.notify(message.id.hashCode(), notification)
    }

    private fun isEmergencyType(type: String): Boolean {
        return type == MeshMessage.TYPE_TRAPPED ||
            type == MeshMessage.TYPE_STARVING ||
            type == MeshMessage.TYPE_MEDICAL ||
            type == MeshMessage.TYPE_SOS
    }
}
