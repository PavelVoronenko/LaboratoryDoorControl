package com.antago30.laboratory.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.antago30.laboratory.ble.bleConnectionManager.BleCommandSender
import com.antago30.laboratory.ble.bleConnectionManager.BleConnectionHandler
import com.antago30.laboratory.ble.bleConnectionManager.BleConnectionState
import com.antago30.laboratory.ble.bleConnectionManager.BleGattCallbackHandler
import com.antago30.laboratory.ble.bleConnectionManager.BleUserListHandler
import com.antago30.laboratory.model.CharacteristicData
import com.antago30.laboratory.model.ConnectResult
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.model.UserInfo
import com.antago30.laboratory.util.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BleConnectionManager(
    context: Context,
    private val coroutineScope: CoroutineScope,
    private val settingsRepo: SettingsRepository,
    isMainInstance: Boolean = false
) {
    private val context = context.applicationContext

    // UUIDs
    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val SEND_COMMAND_CHARACTERISTIC: UUID = UUID.fromString("e3223119-9445-4e96-a4a1-85358c4046a2")

        val SYSTEM_MESSAGE_CHARACTERISTIC: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val TERMINAL_CHARACTERISTIC: UUID = UUID.fromString("e3223119-9445-4e96-a4a1-85358c4046a2")
        val DEBUG_CHARACTERISTIC: UUID = UUID.fromString("d4ad22a3-f08a-4933-8889-8d754b2b2b2b")

        @SuppressLint("StaticFieldLeak")
        @Volatile
        var activeInstance: BleConnectionManager? = null
            private set
    }

    // Компоненты
    private val connectionState = BleConnectionState()
    val callbackHandler = BleGattCallbackHandler(
        connectionState = connectionState,
        coroutineScope = coroutineScope
    )
    private val connectionHandler = BleConnectionHandler(context, callbackHandler)
    
    private val userListHandler = BleUserListHandler(
        coroutineScope = coroutineScope,
        settingsRepo = settingsRepo,
        connectionStateFlow = connectionState.state,
        onSyncComplete = {
            @Suppress("MissingPermission")
            requestPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)
        }
    )

    private val commandSender = BleCommandSender(
        getGatt = { connectionHandler.getGatt() },
        serviceUuid = SERVICE_UUID,
        characteristicUuid = SEND_COMMAND_CHARACTERISTIC
    )

    // Публичные API
    val connectionStateFlow: StateFlow<ConnectionState> = connectionState.state
    val userListFlow: StateFlow<List<UserInfo>> = userListHandler.users
    val characteristicData: SharedFlow<CharacteristicData> = callbackHandler.characteristicUpdates
    val terminalData: SharedFlow<CharacteristicData> = callbackHandler.terminalUpdates
    val debugData: SharedFlow<CharacteristicData> = callbackHandler.debugUpdates

    init {
        // Если это главный экземпляр приложения, сохраняем его для виджета
        if (isMainInstance) {
            activeInstance = this
        }

        // Подписываем обработчик списка пользователей на обновления
        coroutineScope.launch {
            callbackHandler.characteristicUpdates.collect { data ->
                userListHandler.handleData(data)
            }
        }

        // Сброс флагов подписки при отключении
        coroutineScope.launch {
            connectionStateFlow.collect { state ->
                if (state == ConnectionState.DISCONNECTED) {
                    isSensorSubscribed = false
                    isDebugSubscribed = false
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice, autoConnect: Boolean = false): ConnectResult {
        return if (connectionHandler.connect(device, autoConnect)) {
            ConnectResult.Connecting

        } else {
            ConnectResult.Error("Failed to initiate connection")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        connectionHandler.disconnect()
        connectionState.update(ConnectionState.DISCONNECTED)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendCommand(command: String = "TEST"): BleCommandSender.Result {
        // Если запрашиваем список пользователей, повышаем приоритет для быстрой передачи
        if (command == "LISTUSERS") {
            requestPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
        }
        return commandSender.sendCommand(command)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun attemptReconnect() {
        Log.d("BleConnectionManager", "🔍 attemptReconnect() CALLED!")
        val savedAddress = settingsRepo.getSelectedDeviceAddress() ?: return

        Log.d("BleConnectionManager", "Found saved address: $savedAddress")

        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothManager?.adapter?.getRemoteDevice(savedAddress)
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(savedAddress)
        } ?: return

        Log.d("BleConnectionManager", "Got device object for $savedAddress")

        if (connectionState.current != ConnectionState.CONNECTED) {
            Log.d(
                "BleConnectionManager",
                "Current state is ${connectionState.current}, attempting to connect with autoConnect=true"
            )
            connect(device, autoConnect = true)
        } else {
            Log.d("BleConnectionManager", "$savedAddress , skipping reconnect.")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun subscribeToCharacteristic(uuid: UUID): Boolean {
        return toggleCharacteristicNotification(uuid, true)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun unsubscribeFromCharacteristic(uuid: UUID): Boolean {
        return toggleCharacteristicNotification(uuid, false)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun toggleCharacteristicNotification(uuid: UUID, enable: Boolean): Boolean {
        val gatt = connectionHandler.getGatt() ?: return false
        val service = gatt.getService(SERVICE_UUID) ?: return false
        val characteristic = service.getCharacteristic(uuid) ?: return false

        if (!gatt.setCharacteristicNotification(characteristic, enable)) {
            return false
        }

        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val descriptor = characteristic.getDescriptor(cccdUuid) ?: run {
            return false
        }

        val enableValue = if (enable) {
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            } else {
                return false
            }
        } else {
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }

        return gatt.writeDescriptor(descriptor, enableValue) == BluetoothStatusCodes.SUCCESS
    }

    private var isSensorSubscribed = false
    private var isDebugSubscribed = false

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun subscribeToSensorData(force: Boolean = false): Boolean {
        if (isSensorSubscribed && !force) {
            Log.d("BleConnMgr", "📡 Already subscribed to sensor data, skipping.")
            return true
        }
        Log.d("BleConnMgr", "📡 Subscribing to sensor data (Force=$force)...")
        val systemMessageLog = subscribeToCharacteristic(SYSTEM_MESSAGE_CHARACTERISTIC)
        kotlinx.coroutines.delay(300) // Увеличенная задержка
        val terminalLog = subscribeToCharacteristic(TERMINAL_CHARACTERISTIC)
        
        isSensorSubscribed = systemMessageLog && terminalLog
        Log.d("BleConnMgr", "📡 Subscriptions: System=$systemMessageLog, Terminal=$terminalLog")
        return isSensorSubscribed
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun subscribeToDebugData(): Boolean {
        if (isDebugSubscribed) return true
        Log.d("BleConnMgr", "📡 Subscribing to debug data...")
        isDebugSubscribed = subscribeToCharacteristic(DEBUG_CHARACTERISTIC)
        return isDebugSubscribed
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun unsubscribeFromDebugData(): Boolean {
        Log.d("BleConnMgr", "📡 Unsubscribing from debug data...")
        val result = unsubscribeFromCharacteristic(DEBUG_CHARACTERISTIC)
        if (result) isDebugSubscribed = false
        return result
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun requestMtu(size: Int = 200): Boolean {
        val gatt = connectionHandler.getGatt() ?: return false
        Log.d("BLE_DEBUG", "📡 Requesting MTU: $size")
        return gatt.requestMtu(size)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun requestPriority(priority: Int): Boolean {
        val gatt = connectionHandler.getGatt() ?: return false
        Log.d("BLE_DEBUG", "📶 Requesting Priority: $priority")
        return gatt.requestConnectionPriority(priority)
    }

    fun updateWidgets() {
        com.antago30.laboratory.widget.LabWidgetProvider.triggerUpdate(context)
    }

    fun getSettingsRepository() = settingsRepo
}