package com.antago30.laboratory.widget

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.util.NotificationHelper
import com.antago30.laboratory.util.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class LabWidgetService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var settingsRepo: SettingsRepository
    
    // Поля для удержания активного соединения
    private var widgetManager: BleConnectionManager? = null
    private var disconnectJob: Job? = null
    private var lastStartId: Int = -1

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository(applicationContext)
        NotificationHelper.createNotificationChannel(applicationContext)

        // Запускаем Foreground сразу в onCreate для надежности
        val notification = NotificationHelper.createNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(2002, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(2002, notification)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        lastStartId = startId

        // Отменяем таймер отключения, так как пришло новое действие
        disconnectJob?.cancel()

        serviceScope.launch {
            try {
                // Пытаемся взять менеджер приложения, если оно открыто
                val appManager = BleConnectionManager.activeInstance
                val managerToUse = if (appManager?.connectionStateFlow?.value == ConnectionState.READY) {
                    Log.d("LabWidgetService", "🚀 Using App's active connection")
                    appManager
                } else {
                    // Иначе используем/создаем менеджер виджета
                    getWidgetManager()
                }

                if (managerToUse.connectionStateFlow.value == ConnectionState.READY) {
                    Log.d("LabWidgetService", "⚡ Fast execute: $action")
                    executeCommand(managerToUse, action)
                } else {
                    Log.d("LabWidgetService", "🔌 Connecting for: $action")
                    val readySignal = CompletableDeferred<Unit>()
                    managerToUse.callbackHandler.onReadyAction = { readySignal.complete(Unit) }
                    
                    val address = settingsRepo.getSelectedDeviceAddress() ?: return@launch
                    val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
                    managerToUse.connect(bluetoothManager.adapter.getRemoteDevice(address))

                    if (withTimeoutOrNull(2000) { readySignal.await() } != null) {
                        executeCommand(managerToUse, action)
                        delay(100)
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) Log.e("LabWidgetService", "Error: ${e.message}")
            } finally {
                // Вместо немедленного отключения запускаем таймер простоя
                startDisconnectTimer()
            }
        }

        return START_NOT_STICKY
    }

    private fun getWidgetManager(): BleConnectionManager {
        return widgetManager ?: BleConnectionManager(
            context = applicationContext,
            coroutineScope = serviceScope,
            settingsRepo = settingsRepo
        ).also { widgetManager = it }
    }

    @Suppress("MissingPermission")
    private fun startDisconnectTimer() {
        disconnectJob?.cancel()
        disconnectJob = serviceScope.launch {
            Log.d("LabWidgetService", "⏲ Starting 15s inactivity timer...")
            delay(15000) // Удерживаем соединение 15 секунд
            
            Log.d("LabWidgetService", "🧹 Inactivity timeout. Disconnecting...")
            widgetManager?.disconnect()
            widgetManager = null
            
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(lastStartId)
        }
    }

    @Suppress("MissingPermission")
    private fun executeCommand(manager: BleConnectionManager, action: String) {
        when (action) {
            "OPENDOOR" -> manager.sendCommand("OPENDOOR")
            "TOGGLE_LIGHT" -> {
                val currentState = settingsRepo.getLightingState()
                val command = if (currentState) "LIGHTOFF" else "LIGHTON"
                manager.sendCommand(command)
                settingsRepo.saveLightingState(!currentState)
            }
        }
    }

    @Suppress("MissingPermission")
    override fun onDestroy() {
        serviceScope.cancel()
        widgetManager?.disconnect()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
