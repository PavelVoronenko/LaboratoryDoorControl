package com.antago30.laboratory.ble.bleConnectionManager

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.util.Log
import androidx.annotation.RequiresPermission
import com.antago30.laboratory.model.CharacteristicData
import com.antago30.laboratory.model.CommandResult
import com.antago30.laboratory.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class BleGattCallbackHandler(
    private val connectionState: BleConnectionState,
    private val coroutineScope: CoroutineScope,
    var onReadyAction: (() -> Unit)? = null
) : BluetoothGattCallback() {

    private val _commandResults = MutableSharedFlow<CommandResult>(extraBufferCapacity = 16)
    val commandResults: SharedFlow<CommandResult> = _commandResults.asSharedFlow()

    private val _characteristicUpdates = MutableSharedFlow<CharacteristicData>(extraBufferCapacity = 64)
    val characteristicUpdates: SharedFlow<CharacteristicData> = _characteristicUpdates.asSharedFlow()

    private val _terminalUpdates = MutableSharedFlow<CharacteristicData>(extraBufferCapacity = 128)
    val terminalUpdates: SharedFlow<CharacteristicData> = _terminalUpdates.asSharedFlow()

    private val _debugUpdates = MutableSharedFlow<CharacteristicData>(extraBufferCapacity = 32)
    val debugUpdates: SharedFlow<CharacteristicData> = _debugUpdates.asSharedFlow()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Ускоряем обмен данными сразу после подключения
                    gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)

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
            onReadyAction?.invoke()
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

    @Deprecated("Deprecated in Java")
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        @Suppress("DEPRECATION")
        val value = characteristic.value ?: return
        processCharacteristicChanged(characteristic.uuid.toString(), value)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        processCharacteristicChanged(characteristic.uuid.toString(), value)
    }

    private fun processCharacteristicChanged(uuidStr: String, value: ByteArray) {
        val data = CharacteristicData(
            uuid = uuidStr,
            value = value.toList()
        )
        // Log.d("BleGattCallback", "📥 Received notification from $uuidStr: ${value.size} bytes")
        coroutineScope.launch {
            if (uuidStr.equals("e3223119-9445-4e96-a4a1-85358c4046a2", ignoreCase = true)) {
                Log.d("BleGattCallback", "➡️ Emitting to terminalUpdates (MSG: ${String(value).take(15)}...)")
                _terminalUpdates.emit(data)
            } else if (uuidStr.equals("d4ad22a3-f08a-4933-8889-8d754b2b2b2b", ignoreCase = true)) {
                _debugUpdates.emit(data)
            } else {
                _characteristicUpdates.emit(data)
            }
        }
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d("BleGattCallback", "✅ Descriptor written successfully: ${descriptor.uuid} (Chr: ${descriptor.characteristic.uuid})")
        } else {
            Log.e("BleGattCallback", "❌ Descriptor write failed for ${descriptor.uuid}, status: $status")
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d("BLE_DEBUG", "✅ MTU changed to: $mtu")
            // Доступный размер данных = mtu - 3 (заголовок BLE)
            Log.d("BLE_DEBUG", "📦 Max payload: ${mtu - 3} bytes")
        } else {
            Log.e("BLE_DEBUG", "❌ MTU change failed: $status")
        }
    }
}