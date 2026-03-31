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
import java.util.UUID

class BleAdvertisingService : Service() {

    private lateinit var bleAdvertiser: BleAdvertiser
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var currentServiceUuid: UUID? = null
    private var currentAdData: String? = null

    companion object {
        const val EXTRA_SERVICE_UUID = "extra_service_uuid"
        const val EXTRA_AD_DATA = "extra_ad_data"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            NotificationHelper.createNotificationChannel(applicationContext)
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return START_NOT_STICKY
        }

        val serviceUuidStr = intent?.getStringExtra(EXTRA_SERVICE_UUID)
        val adData = intent?.getStringExtra(EXTRA_AD_DATA) ?: "J7hs2Ak98g"

        // Запускаем BLE в фоне
        if (serviceUuidStr != null) {
            try {
                currentServiceUuid = UUID.fromString(serviceUuidStr)
                currentAdData = adData

                bleAdvertiser = BleAdvertiser(
                    applicationContext,
                    currentServiceUuid!!,
                    currentAdData!!
                )

                serviceScope.launch {
                    kotlinx.coroutines.delay(100)
                    bleAdvertiser.startAdvertising()
                }
            } catch (e: Exception) {
                return START_NOT_STICKY
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