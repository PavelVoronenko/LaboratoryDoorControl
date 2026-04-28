package com.antago30.laboratory.ble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import com.antago30.laboratory.util.NotificationHelper
import com.antago30.laboratory.util.SettingsRepository
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
    private lateinit var settingsRepo: SettingsRepository

    private var currentServiceUuid: UUID? = null
    private var currentAdData: String? = null

    private var isAdvertisingActive = false
        set(value) {
            if (field != value) {
                field = value
                sendStateBroadcast(value)
            }
        }

    private fun sendStateBroadcast(isRunning: Boolean) {
        val intent = Intent(ACTION_STATE_CHANGED).apply {
            putExtra(EXTRA_IS_RUNNING, isRunning)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) {
                    android.util.Log.d("BleAdvertisingService", "Bluetooth turned ON, restarting advertising")
                    restartAdvertising()
                }
            }
        }
    }

    companion object {
        const val EXTRA_SERVICE_UUID = "extra_service_uuid"
        const val EXTRA_AD_DATA = "extra_ad_data"
        const val ACTION_STATE_CHANGED = "com.antago30.laboratory.ble.ACTION_STATE_CHANGED"
        const val ACTION_STOP = "com.antago30.laboratory.ble.ACTION_STOP"
        const val EXTRA_IS_RUNNING = "is_running"
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository(applicationContext)
        registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        
        try {
            NotificationHelper.createNotificationChannel(applicationContext)
            
            // Запускаем Foreground сразу в onCreate, чтобы избежать ForegroundServiceDidNotStartInTimeException
            val notification = NotificationHelper.createNotification(this)
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
            android.util.Log.e("BleAdvertisingService", "Failed to start as foreground in onCreate", e)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            android.util.Log.d("BleAdvertisingService", "Stop action received")
            stopAdvertisingInternal()
            stopSelf()
            return START_NOT_STICKY
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        val serviceUuidStr = intent?.getStringExtra(EXTRA_SERVICE_UUID)
        val adData = intent?.getStringExtra(EXTRA_AD_DATA)

        if (serviceUuidStr != null) {
            // Прямой запуск (из приложения, виджета или BootReceiver)
            try {
                currentServiceUuid = UUID.fromString(serviceUuidStr)
                currentAdData = adData ?: "J7hs2Ak98g"

                android.util.Log.d("BleAdvertisingService", "Direct start with UUID: $currentServiceUuid")
                startAdvertisingInternal()
                startWatchdog()
            } catch (e: Exception) {
                android.util.Log.e("BleAdvertisingService", "Error starting advertising", e)
            }
        } else {
            // Восстановление после закрытия системой (Sticky/Redeliver restart)
            if (settingsRepo.isAdvertisingEnabled()) {
                val currentUser = settingsRepo.getCurrentUserInfo()
                if (currentUser != null) {
                    currentServiceUuid = UUID.fromString(currentUser.uuid)
                    currentAdData = currentUser.serviceData
                    android.util.Log.d("BleAdvertisingService", "Restoring parameters after system kill. Waiting for Watchdog.")
                    // Не запускаем рекламу сразу, чтобы не бесить пользователя при открытии приложения после Force Stop.
                    // Watchdog проверит состояние через небольшой промежуток времени.
                    startWatchdog()
                } else {
                    stopSelf()
                }
            } else {
                android.util.Log.d("BleAdvertisingService", "Service restarted but advertising is disabled in settings. Stopping.")
                stopSelf()
            }
        }

        return START_REDELIVER_INTENT
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertisingInternal() {
        val uuid = currentServiceUuid ?: return
        val data = currentAdData ?: return

        bleAdvertiser?.stopAdvertising()
        bleAdvertiser = BleAdvertiser(applicationContext, uuid, data).apply {
            onStateChanged = { active ->
                isAdvertisingActive = active
            }
        }
        bleAdvertiser?.startAdvertising()
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertisingInternal() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bleAdvertiser?.stopAdvertising()
        }
        isAdvertisingActive = false
    }

    private fun restartAdvertising() {
        if (currentServiceUuid != null) {
            startAdvertisingInternal()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = serviceScope.launch {
            // Первый запуск проверки делаем быстрее (через 10 секунд), если мы только что восстановились
            var checkInterval = 10_000L 
            
            while (isActive) {
                delay(checkInterval)
                
                // После первой проверки переходим на обычный интервал (5 минут)
                checkInterval = 5 * 60 * 1000L

                val shouldBeRunning = settingsRepo.isAdvertisingEnabled()
                if (!shouldBeRunning) {
                    android.util.Log.d("BleAdvertisingService", "Watchdog: Advertising disabled in settings, stopping service")
                    stopSelf()
                    break
                }

                val currentHour = System.currentTimeMillis() / (1000 * 60 * 60)
                val shouldForceRestart = (currentHour % 4 == 0L)

                if (!isAdvertisingActive || shouldForceRestart) {
                    android.util.Log.d("BleAdvertisingService", "Watchdog: restarting advertising (active=$isAdvertisingActive, force=$shouldForceRestart)")
                    restartAdvertising()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        try {
            unregisterReceiver(bluetoothStateReceiver)
        } catch (_: Exception) { }
        watchdogJob?.cancel()
        serviceScope.cancel()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bleAdvertiser?.stopAdvertising()
        }
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
