package com.antago30.laboratory.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.antago30.laboratory.R

object NotificationHelper {
    const val NOTIFICATION_ID = 1001
    private const val CHANNEL_ID = "ble_advertising_channel"

    fun createNotificationChannel(context: Context) {
        try {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BLE Advertising",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background BLE advertising"
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        } catch (e: Exception) {

        }
    }

    fun createNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Broadcasting...")
            //.setContentText("Broadcasting...")
            .setSmallIcon(R.drawable.advertise)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}