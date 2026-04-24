package com.antago30.laboratory.widget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.antago30.laboratory.R
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.util.SettingsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class LabWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        startBackgroundScan(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_OPEN_DOOR -> handleWidgetAction(context, "OPENDOOR")
            ACTION_TOGGLE_LIGHT -> handleWidgetAction(context, "TOGGLE_LIGHT")
            ACTION_BLE_SCAN_RESULT -> handleScanResults(context, intent)
        }
    }

    @Suppress("MissingPermission")
    private fun handleWidgetAction(context: Context, action: String) {
        val pendingResult = goAsync()
        widgetScope.launch {
            try {
                // Пытаемся использовать активный менеджер приложения или наш внутренний
                val appManager = BleConnectionManager.activeInstance
                val manager = appManager ?: getInternalManager(context)
                
                if (manager.connectionStateFlow.value == ConnectionState.READY) {
                    executeBleCommand(context, manager, action)
                    delay(300) // Даем время пакету уйти
                } else {
                    val address = SettingsRepository(context).getSelectedDeviceAddress()
                    if (address != null) {
                        val readySignal = CompletableDeferred<Unit>()
                        manager.callbackHandler.onReadyAction = { readySignal.complete(Unit) }
                        
                        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                        val adapter = bluetoothManager.adapter
                        if (adapter != null && adapter.isEnabled) {
                            manager.connect(adapter.getRemoteDevice(address))
                            
                            if (withTimeoutOrNull(2500) { readySignal.await() } != null) {
                                executeBleCommand(context, manager, action)
                                delay(300)
                            }
                        }
                    }
                }
                
                // Запускаем таймер отключения
                startInactivityTimer(manager)
                
            } catch (e: Exception) {
                Log.e("LabWidget", "Action error: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun executeBleCommand(context: Context, manager: BleConnectionManager, action: String) {
        val settingsRepo = SettingsRepository(context)
        when (action) {
            "OPENDOOR" -> manager.sendCommand("OPENDOOR")
            "TOGGLE_LIGHT" -> {
                val currentState = settingsRepo.getLightingState()
                val command = if (currentState) "LIGHTOFF" else "LIGHTON"
                manager.sendCommand(command)
                settingsRepo.saveLightingState(!currentState)
                triggerUpdate(context)
            }
        }
    }

    private fun handleScanResults(context: Context, intent: Intent) {
        val results: ArrayList<ScanResult>? =
            intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT, ScanResult::class.java)

        val settingsRepo = SettingsRepository(context)
        val targetAddress = settingsRepo.getSelectedDeviceAddress()

        results?.forEach { result ->
            // Проверяем, что данные пришли именно от нашего устройства
            if (targetAddress != null && !result.device.address.equals(targetAddress, ignoreCase = true)) {
                return@forEach
            }

            val scanRecord = result.scanRecord ?: return@forEach
            val serviceDataMap = scanRecord.serviceData
            
            var data: ByteArray? = null
            for ((uuid, bytes) in serviceDataMap) {
                if (uuid.toString().contains("ffe0", ignoreCase = true)) {
                    data = bytes
                    break
                }
            }
            
            if (data != null && data.size >= 2) {
                val isLightOn = data[0].toInt() == 1
                val isJdeConnected = data[1].toInt() == 1
                
                val settingsRepo = SettingsRepository(context)
                val oldLightState = settingsRepo.getLightingState()
                val oldJdeState = settingsRepo.getJdeConnectionState()
                
                if (oldLightState != isLightOn || oldJdeState != isJdeConnected) {
                    settingsRepo.saveLightingState(isLightOn)
                    settingsRepo.saveJdeConnectionState(isJdeConnected)
                    triggerUpdate(context)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBackgroundScan(context: Context) {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            val scanner = adapter?.bluetoothLeScanner ?: return
            
            val intent = Intent(context, LabWidgetProvider::class.java).apply {
                action = ACTION_BLE_SCAN_RESULT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 10, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            // Используем фильтр по имени
            val filter = ScanFilter.Builder()
                .setDeviceName("Laboratory")
                .build()
            
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER) // Low Power лучше для фона
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build()

            scanner.startScan(listOf(filter), settings, pendingIntent)
        } catch (e: Exception) {
            Log.e("LabWidgetProvider", "Scan start failed", e)
        }
    }

    companion object {
        const val ACTION_OPEN_DOOR = "com.antago30.laboratory.action.OPEN_DOOR"
        const val ACTION_TOGGLE_LIGHT = "com.antago30.laboratory.action.TOGGLE_LIGHT"
        const val ACTION_BLE_SCAN_RESULT = "com.antago30.laboratory.action.BLE_SCAN_RESULT"

        private val widgetScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        @SuppressLint("StaticFieldLeak")
        private var internalManager: BleConnectionManager? = null
        private var inactivityJob: Job? = null

        private fun getInternalManager(context: Context): BleConnectionManager {
            return internalManager ?: BleConnectionManager(
                context = context.applicationContext,
                coroutineScope = widgetScope,
                settingsRepo = SettingsRepository(context)
            ).also { internalManager = it }
        }

        @Suppress("MissingPermission")
        private fun startInactivityTimer(manager: BleConnectionManager) {
            inactivityJob?.cancel()
            inactivityJob = widgetScope.launch {
                delay(5000) // 5 секунд таймер разрыва соединения
                
                val isAppInForeground = try {
                    ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                } catch (e: Exception) { false }

                // Отключаем, если это внутренний менеджер ИЛИ если приложение в фоне
                if (manager === internalManager || !isAppInForeground) {
                    Log.d("LabWidget", "Inactivity timeout: disconnecting BLE")
                    manager.disconnect()
                    if (manager === internalManager) {
                        internalManager = null
                    }
                }
            }
        }

        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, LabWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, LabWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.lab_widget)
            val settingsRepo = SettingsRepository(context)
            val isLightOn = settingsRepo.getLightingState()
            val isJdeConnected = settingsRepo.getJdeConnectionState()

            // Дверь всегда в нейтральном серебристом стиле
            views.setInt(R.id.img_door, "setColorFilter", "#E0E0E0".toColorInt())

            // Лампочка меняет цвет в зависимости от состояния
            val lightColor = when {
                !isJdeConnected -> "#EF5350".toColorInt() // Мягкий красный
                isLightOn -> "#E0E0E0".toColorInt()      // Нейтрально-белый
                else -> "#757575".toColorInt()           // Глубокий серый (выключен)
            }
            views.setInt(R.id.img_light, "setColorFilter", lightColor)

            val openDoorIntent = Intent(context, LabWidgetProvider::class.java).apply { action = ACTION_OPEN_DOOR }
            views.setOnClickPendingIntent(R.id.btn_open_door, PendingIntent.getBroadcast(context, 0, openDoorIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            val lightIntent = Intent(context, LabWidgetProvider::class.java).apply { action = ACTION_TOGGLE_LIGHT }
            views.setOnClickPendingIntent(R.id.btn_toggle_light, PendingIntent.getBroadcast(context, 1, lightIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
