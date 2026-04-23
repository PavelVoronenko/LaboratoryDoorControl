package com.antago30.laboratory.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
import com.antago30.laboratory.model.CharacteristicData
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.model.CommandResult
import com.antago30.laboratory.model.ConnectResult
import com.antago30.laboratory.util.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class BleConnectionManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val settingsRepo: SettingsRepository
) {
    init {
        // Сохраняем экземпляр для переиспользования (например, виджетом)
        activeInstance = this
    }

    // UUIDs
    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val SEND_COMMAND_CHARACTERISTIC: UUID = UUID.fromString("e3223119-9445-4e96-a4a1-85358c4046a2")

        val SYSTEM_MESSAGE_CHARACTERISTIC: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val TERMINAL_CHARACTERISTIC: UUID = UUID.fromString("e3223119-9445-4e96-a4a1-85358c4046a2")

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
    private val commandSender = BleCommandSender(
        getGatt = { connectionHandler.getGatt() },
        serviceUuid = SERVICE_UUID,
        characteristicUuid = SEND_COMMAND_CHARACTERISTIC
    )

    // Публичные API
    val connectionStateFlow: StateFlow<ConnectionState> = connectionState.state
    val doorCommandResult: SharedFlow<CommandResult> = callbackHandler.commandResults
    val characteristicData: SharedFlow<CharacteristicData> = callbackHandler.characteristicUpdates
    val terminalData: SharedFlow<CharacteristicData> = callbackHandler.terminalUpdates

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
        val gatt = connectionHandler.getGatt() ?: return false
        val service = gatt.getService(SERVICE_UUID) ?: return false
        val characteristic = service.getCharacteristic(uuid) ?: return false

        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            return false
        }

        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val descriptor = characteristic.getDescriptor(cccdUuid) ?: run {
            return false
        }

        // Определяем тип уведомления
        val enableValue =
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            } else {
                return false
            }

        return gatt.writeDescriptor(descriptor, enableValue) == BluetoothStatusCodes.SUCCESS
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun subscribeToSensorData(): Boolean {
        val systemMessageLog = subscribeToCharacteristic(SYSTEM_MESSAGE_CHARACTERISTIC)
        val terminalLog = subscribeToCharacteristic(TERMINAL_CHARACTERISTIC)
        return systemMessageLog && terminalLog
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun requestMtu(size: Int = 200): Boolean {
        val gatt = connectionHandler.getGatt() ?: return false
        Log.d("BLE_DEBUG", "📡 Requesting MTU: $size")
        return gatt.requestMtu(size)
    }

    fun getSettingsRepository() = settingsRepo
}