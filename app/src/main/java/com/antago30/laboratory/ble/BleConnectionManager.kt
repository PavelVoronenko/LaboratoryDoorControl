package com.antago30.laboratory.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.antago30.laboratory.ble.bleConnectionManager.BleCommandSender
import com.antago30.laboratory.ble.bleConnectionManager.BleConnectionHandler
import com.antago30.laboratory.ble.bleConnectionManager.BleConnectionState
import com.antago30.laboratory.ble.bleConnectionManager.BleGattCallbackHandler
import com.antago30.laboratory.ble.bleConnectionManager.ConnectionState
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
    // UUIDs
    companion object {
        val DOOR_SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val DOOR_COMMAND_CHARACTERISTIC_UUID: UUID = UUID.fromString("e3223119-9445-4e96-a4a1-85358c4046a2")
    }

    // Компоненты
    private val connectionState = BleConnectionState()
    private val callbackHandler = BleGattCallbackHandler(
        connectionState = connectionState,
        coroutineScope = coroutineScope
    )
    private val connectionHandler = BleConnectionHandler(context, callbackHandler)
    private val commandSender = BleCommandSender(
        getGatt = { connectionHandler.getGatt() },
        serviceUuid = DOOR_SERVICE_UUID,
        characteristicUuid = DOOR_COMMAND_CHARACTERISTIC_UUID
    )

    // Публичные API
    val connectionStateFlow: StateFlow<ConnectionState> = connectionState.state
    val doorCommandResult: SharedFlow<BleGattCallbackHandler.CommandResult> = callbackHandler.commandResults

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
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothManager?.adapter?.getRemoteDevice(savedAddress)
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(savedAddress)
        } ?: return

        Log.d("BleConnectionManager", "Got device object for $savedAddress")

        if (connectionState.current != ConnectionState.CONNECTED) {
            Log.d("BleConnectionManager", "Current state is ${connectionState.current}, attempting to connect with autoConnect=true")
            connect(device, autoConnect = true)
        } else {
            Log.d("BleConnectionManager", "$savedAddress , skipping reconnect.")
        }
    }

    sealed class ConnectResult {
        object Connecting : ConnectResult()
        data class Error(val message: String) : ConnectResult()
    }
}