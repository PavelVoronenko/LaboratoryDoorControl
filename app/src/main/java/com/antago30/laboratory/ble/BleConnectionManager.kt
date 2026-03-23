package com.antago30.laboratory.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.antago30.laboratory.util.SettingsRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING
}

class BleConnectionManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val settingsRepo: SettingsRepository
) {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var currentDeviceAddress: String? = null
    private var servicesDiscovered = false

    companion object {
        val DOOR_SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val DOOR_COMMAND_CHARACTERISTIC_UUID: UUID = UUID.fromString("e3223119-9445-4e96-a4a1-85358c4046a2")
    }

    sealed class DoorCommandResult {
        object Success : DoorCommandResult()
        object NotConnected : DoorCommandResult()
        object CharacteristicNotFound : DoorCommandResult()
        object WriteFailed : DoorCommandResult()
        data class Error(val message: String) : DoorCommandResult()
    }

    // Flow для уведомлений о результате команды
    private val _doorCommandResult = MutableSharedFlow<DoorCommandResult>()
    val doorCommandResult: SharedFlow<DoorCommandResult> = _doorCommandResult.asSharedFlow()

    @SuppressLint("MissingPermission")
    fun sendDoorCommand(command: String = "OPENDOOR"): DoorCommandResult {
        if (_connectionState.value != ConnectionState.CONNECTED || bluetoothGatt == null) {
            return DoorCommandResult.NotConnected
        }

        val gatt = bluetoothGatt!!
        val service = gatt.getService(DOOR_SERVICE_UUID)
            ?: return DoorCommandResult.CharacteristicNotFound.also {
                android.util.Log.e("BLE_DEBUG", "❌ Service not found")
                coroutineScope.launch { _doorCommandResult.emit(it) }
            }

        val characteristic = service.getCharacteristic(DOOR_COMMAND_CHARACTERISTIC_UUID)
            ?: return DoorCommandResult.CharacteristicNotFound.also {
                android.util.Log.e("BLE_DEBUG", "❌ Characteristic $DOOR_COMMAND_CHARACTERISTIC_UUID not found")
                coroutineScope.launch { _doorCommandResult.emit(it) }
            }

        val writeProps = BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
        if (characteristic.properties.and(writeProps) == 0) {
            return DoorCommandResult.WriteFailed.also {
                android.util.Log.e("BLE_DEBUG", "❌ Characteristic does not support WRITE")
                coroutineScope.launch { _doorCommandResult.emit(it) }
            }
        }

        return try {
            val commandBytes = command.toByteArray(Charsets.UTF_8)

            android.util.Log.d("BLE_DEBUG", "📝 Writing to: ${characteristic.uuid}")
            android.util.Log.d("BLE_DEBUG", "📝 Command: '$command' (${commandBytes.size} bytes)")
            android.util.Log.d("BLE_DEBUG", "📝 Bytes: ${commandBytes.joinToString(" ") { String.format("%02X", it) }}")

            // ✅ Вызываем НОВЫЙ API, возвращающий int (код ошибки)
            val writeResult = gatt.writeCharacteristic(
                characteristic,
                commandBytes,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )

            // ✅ Проверяем результат: 0 означает успех (GATT_SUCCESS)
            if (writeResult == BluetoothGatt.GATT_SUCCESS) {
                android.util.Log.d("BLE_DEBUG", "✅ Write request sent successfully (Result: $writeResult)")
                DoorCommandResult.Success
            } else {
                android.util.Log.e("BLE_DEBUG", "❌ writeCharacteristic() failed with code: $writeResult")
                DoorCommandResult.WriteFailed
            }
        } catch (e: Exception) {
            android.util.Log.e("BLE_DEBUG", "❌ Exception: ${e.message}", e)
            DoorCommandResult.Error(e.message ?: "Unknown error")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            android.util.Log.d("BLE_DEBUG", "onConnectionStateChange: status=$status, newState=$newState")

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    android.util.Log.d("BLE_DEBUG", "✅ STATE_CONNECTED")
                    _connectionState.value = ConnectionState.CONNECTED
                    servicesDiscovered = false

                    android.util.Log.d("BLE_DEBUG", "📡 Discovering services (no MTU request, как в виджете)...")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    android.util.Log.d("BLE_DEBUG", "❌ STATE_DISCONNECTED")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    bluetoothGatt = null
                    servicesDiscovered = false
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    android.util.Log.d("BLE_DEBUG", "🔄 STATE_CONNECTING")
                    _connectionState.value = ConnectionState.CONNECTING
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    android.util.Log.d("BLE_DEBUG", "⏸ STATE_DISCONNECTING")
                    _connectionState.value = ConnectionState.DISCONNECTING
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            android.util.Log.d("BLE_DEBUG", "onServicesDiscovered: status=$status, alreadyDiscovered=$servicesDiscovered")

            if (!servicesDiscovered && status == BluetoothGatt.GATT_SUCCESS) {
                servicesDiscovered = true
                android.util.Log.d("BLE_DEBUG", "✅ Services discovered")

                gatt.services.forEach { service ->
                    android.util.Log.d("BLE_DEBUG", "Service: ${service.uuid}")
                    service.characteristics.forEach { char ->
                        android.util.Log.d("BLE_DEBUG", "  Characteristic: ${char.uuid}, props=${char.properties}")
                    }
                }

                android.util.Log.d("BLE_DEBUG", "✅ Ready to send commands (no notifications, как в виджете)")
            } else if (servicesDiscovered) {
                android.util.Log.d("BLE_DEBUG", "⚠️ Services already discovered, skipping")
            } else {
                android.util.Log.e("BLE_DEBUG", "❌ Service discovery failed: $status")
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            android.util.Log.d("BLE_DEBUG", "onReadRemoteRssi: rssi=$rssi, status=$status")
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            android.util.Log.d("BLE_DEBUG", "onCharacteristicWrite: status=$status, uuid=${characteristic.uuid}")
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    coroutineScope.launch {
                        _doorCommandResult.emit(DoorCommandResult.Success)
                    }
                    android.util.Log.d("BLE_DEBUG", "✅ Команда успешно записана!")
                }
                BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                    coroutineScope.launch {
                        _doorCommandResult.emit(DoorCommandResult.WriteFailed)
                    }
                    android.util.Log.d("BLE_DEBUG", "❌ Команда не записана: WRITE_NOT_PERMITTED")
                }
                BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> {
                    coroutineScope.launch {
                        _doorCommandResult.emit(DoorCommandResult.Error("Требуется аутентификация"))
                    }
                    android.util.Log.d("BLE_DEBUG", "❌ Требуется аутентификация")
                }
                else -> {
                    coroutineScope.launch {
                        _doorCommandResult.emit(DoorCommandResult.Error("GATT error: $status"))
                    }
                    android.util.Log.d("BLE_DEBUG", "❌ GATT error: $status")
                }
            }
        }
    }

    // 🔧 Метод подключения
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice, autoConnect: Boolean = false): ConnectResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connectPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            )
            if (connectPermission != PackageManager.PERMISSION_GRANTED) {
                return ConnectResult.PermissionDenied
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scanPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            )
            if (scanPermission != PackageManager.PERMISSION_GRANTED) {
                return ConnectResult.PermissionDenied
            }
        }

        return try {
            _connectionState.value = ConnectionState.CONNECTING
            currentDeviceAddress = device.address

            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(
                    context,
                    autoConnect,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
            } else {
                device.connectGatt(context, autoConnect, gattCallback)
            }
            ConnectResult.Connecting
        } catch (e: SecurityException) {
            _connectionState.value = ConnectionState.DISCONNECTED
            ConnectResult.PermissionDenied
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            ConnectResult.Error(e.message ?: "Unknown error")
        }
    }

    // Результаты операции подключения
    sealed class ConnectResult {
        object Connecting : ConnectResult()
        object PermissionDenied : ConnectResult()
        data class Error(val message: String) : ConnectResult()
    }

    fun attemptReconnect() {
        val savedAddress = settingsRepo.getSelectedDeviceAddress() // Получаем сохранённый адрес
        if (savedAddress != null) {
            val device = BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(savedAddress)
            if (device != null) {
                // 🔹 Проверяем, не подключено ли уже устройство
                if (connectionState.value != ConnectionState.CONNECTED) {
                    android.util.Log.d("BleConnectionManager", "🔄 Attempting reconnect to $savedAddress")
                    // 🔹 Пытаемся подключиться (autoConnect = true для быстрого восстановления)
                    connect(device, autoConnect = true)
                } else {
                    android.util.Log.d("BleConnectionManager", "✅ Already connected to $savedAddress")
                }
            } else {
                android.util.Log.w("BleConnectionManager", "Could not get remote device for address: $savedAddress")
            }
        } else {
            android.util.Log.d("BleConnectionManager", "No saved device address found for reconnection.")
        }
    }

    // Метод отключения
    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.let {
            _connectionState.value = ConnectionState.DISCONNECTING
            it.disconnect()
            it.close()
            bluetoothGatt = null
            currentDeviceAddress = null
            _connectionState.value = ConnectionState.DISCONNECTED
            servicesDiscovered = false
        }
    }

    fun getConnectedDeviceAddress(): String? = currentDeviceAddress
}