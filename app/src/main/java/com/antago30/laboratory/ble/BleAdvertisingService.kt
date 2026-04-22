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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class BleAdvertisingService : Service() {

    private var bleAdvertiser: BleAdvertiser? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var watchdogJob: Job? = null

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

                // Останавливаем старый, если был
                bleAdvertiser?.stopAdvertising()

                bleAdvertiser = BleAdvertiser(
                    applicationContext,
                    currentServiceUuid!!,
                    currentAdData!!
                )

                bleAdvertiser?.startAdvertising()
                
                // Запускаем Watchdog для периодического перезапуска
                startWatchdog()
            } catch (e: Exception) {
                android.util.Log.e("BleAdvertisingService", "Error starting advertising", e)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = serviceScope.launch {
            while (isActive) {
                // Ждем 4 часа
                delay(4 * 60 * 60 * 1000L)
                
                android.util.Log.d("BleAdvertisingService", "Watchdog: Periodic restart of advertising")
                
                bleAdvertiser?.let { adv ->
                    try {
                        adv.stopAdvertising()
                        delay(1000) // Пауза для сброса стека
                        adv.startAdvertising()
                    } catch (e: Exception) {
                        android.util.Log.e("BleAdvertisingService", "Watchdog error during restart", e)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        watchdogJob?.cancel()
        serviceScope.cancel()
        bleAdvertiser?.let { advertiser ->
            try {
                advertiser.stopAdvertising()
            } catch (e: Exception) {
                android.util.Log.e("BleAdvertisingService", "Error stopping advertising", e)
            }
        }
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
