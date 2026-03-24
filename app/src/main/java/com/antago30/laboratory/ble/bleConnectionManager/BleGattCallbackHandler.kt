package com.antago30.laboratory.ble.bleConnectionManager

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class BleGattCallbackHandler(
    private val connectionState: BleConnectionState,
    private val coroutineScope: CoroutineScope,
    private val onServicesReady: () -> Unit = {}
) : BluetoothGattCallback() {

    sealed class CommandResult {
        object Success : CommandResult()
        object WriteFailed : CommandResult()
        data class Error(val message: String) : CommandResult()
    }

    private val _commandResults = MutableSharedFlow<CommandResult>()
    val commandResults: SharedFlow<CommandResult> = _commandResults.asSharedFlow()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    connectionState.update(ConnectionState.CONNECTED)
                    gatt.discoverServices()
                    connectionState.update(ConnectionState.SERVICES_DISCOVERING)
                } else {
                    connectionState.update(ConnectionState.DISCONNECTED)
                }
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                connectionState.update(ConnectionState.DISCONNECTED)
            }
            BluetoothProfile.STATE_CONNECTING -> connectionState.update(ConnectionState.CONNECTING)
            BluetoothProfile.STATE_DISCONNECTING -> connectionState.update(ConnectionState.DISCONNECTING)
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            connectionState.update(ConnectionState.READY)
            onServicesReady()
        } else {
            connectionState.update(ConnectionState.CONNECTED)
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        val result = when (status) {
            BluetoothGatt.GATT_SUCCESS -> CommandResult.Success
            BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> CommandResult.WriteFailed
            else -> CommandResult.Error("GATT error: $status")
        }
        coroutineScope.launch { _commandResults.emit(result) }
    }
}