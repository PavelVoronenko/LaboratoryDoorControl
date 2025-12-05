package com.antago30.laboratory.ble

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.antago30.laboratory.util.NotificationHelper

class BleAdvertisingService : Service() {

    private lateinit var bleAdvertiser: BleAdvertiser

    override fun onCreate() {
        super.onCreate()
        bleAdvertiser = BleAdvertiser(this)
        NotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.createNotification(this)
        )
        bleAdvertiser.startAdvertising("J7hs2Ak98g")
        return START_STICKY
    }

    override fun onDestroy() {
        bleAdvertiser.stopAdvertising()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}