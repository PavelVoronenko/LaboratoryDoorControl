package com.antago30.laboratory.ble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import com.antago30.laboratory.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BleAdvertisingService : Service() {

    private lateinit var bleAdvertiser: BleAdvertiser
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        try {
            bleAdvertiser = BleAdvertiser(applicationContext)
            NotificationHelper.createNotificationChannel(applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("BleAdvertisingService", "onCreate failed", e)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationHelper.createNotification(this)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            }
            android.util.Log.d("BleAdvertisingService", "✅ startForeground() SUCCESS")
        } catch (e: Exception) {
            android.util.Log.e("BleAdvertisingService", "startForeground() failed", e)
            stopSelf()
            return START_NOT_STICKY
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("BleAdvertisingService", "BLUETOOTH_ADVERTISE permission NOT GRANTED")
            return START_NOT_STICKY
        }

        // Запускаем BLE в фоне ТОЛЬКО если разрешение есть
        serviceScope.launch {
            try {
                kotlinx.coroutines.delay(100)
                bleAdvertiser.startAdvertising("J7hs2Ak98g")
                android.util.Log.d("BleAdvertisingService", "✅ Advertising started in background")
            } catch (e: Exception) {
                android.util.Log.e("BleAdvertisingService", "Advertising failed", e)
            }
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        serviceScope.cancel()
        try {
            bleAdvertiser.stopAdvertising()
        } catch (e: Exception) {
            android.util.Log.e("BleAdvertisingService", "Error stopping advertising", e)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}