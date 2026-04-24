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

class LabStyledWidgetProvider : AppWidgetProvider() {

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
                val appManager = BleConnectionManager.activeInstance
                val manager = appManager ?: getInternalManager(context)
                
                if (manager.connectionStateFlow.value == ConnectionState.READY) {
                    executeBleCommand(context, manager, action)
                    delay(300)
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
                startInactivityTimer(manager)
            } catch (e: Exception) {
                Log.e("LabStyledWidget", "Action error: ${e.message}")
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
            
            val intent = Intent(context, LabStyledWidgetProvider::class.java).apply {
                action = ACTION_BLE_SCAN_RESULT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 20, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            val filter = ScanFilter.Builder()
                .setDeviceName("Laboratory")
                .build()
            
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build()

            scanner.startScan(listOf(filter), settings, pendingIntent)
        } catch (e: Exception) {
            Log.e("LabStyledWidget", "Scan start failed", e)
        }
    }

    companion object {
        const val ACTION_OPEN_DOOR = "com.antago30.laboratory.action.OPEN_DOOR_STYLED"
        const val ACTION_TOGGLE_LIGHT = "com.antago30.laboratory.action.TOGGLE_LIGHT_STYLED"
        const val ACTION_BLE_SCAN_RESULT = "com.antago30.laboratory.action.BLE_SCAN_RESULT_STYLED"

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
                delay(5000)
                val isAppInForeground = try {
                    ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                } catch (_: Exception) { false }

                if (manager === internalManager || !isAppInForeground) {
                    manager.disconnect()
                    if (manager === internalManager) {
                        internalManager = null
                    }
                }
            }
        }

        fun triggerUpdate(context: Context) {
            // Централизованное обновление всех виджетов через основной провайдер
            LabWidgetProvider.triggerUpdate(context)
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.lab_widget_styled)
            val settingsRepo = SettingsRepository(context)
            val isLightOn = settingsRepo.getLightingState()
            val isJdeConnected = settingsRepo.getJdeConnectionState()

            // В стилизованном виджете используем Primary цвет приложения для активных элементов
            val primaryColor = "#4FC3F7".toColorInt()
            val mutedColor = "#A0AEC0".toColorInt() // TextMuted
            val errorColor = "#F56565".toColorInt() // Outdoor (Red)

            views.setInt(R.id.img_door, "setColorFilter", primaryColor)

            val lightColor = when {
                !isJdeConnected -> errorColor
                isLightOn -> primaryColor
                else -> mutedColor
            }
            views.setInt(R.id.img_light, "setColorFilter", lightColor)

            val openDoorIntent = Intent(context, LabStyledWidgetProvider::class.java).apply { action = ACTION_OPEN_DOOR }
            views.setOnClickPendingIntent(R.id.btn_open_door, PendingIntent.getBroadcast(context, 0, openDoorIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            val lightIntent = Intent(context, LabStyledWidgetProvider::class.java).apply { action = ACTION_TOGGLE_LIGHT }
            views.setOnClickPendingIntent(R.id.btn_toggle_light, PendingIntent.getBroadcast(context, 1, lightIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
