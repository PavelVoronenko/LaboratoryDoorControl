package com.antago30.laboratory.ble.bleConnectionManager

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import androidx.annotation.RequiresPermission
import java.util.UUID

class BleCommandSender(
    private val getGatt: () -> BluetoothGatt?,
    private val serviceUuid: UUID,
    private val characteristicUuid: UUID
) {
    sealed class Result {
        object Success : Result()
        object NotConnected : Result()
        object CharacteristicNotFound : Result()
        object WriteFailed : Result()
        data class Error(val message: String) : Result()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendCommand(command: String = "TEST"): Result {
        return sendData(command.toByteArray(Charsets.UTF_8))
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendData(bytes: ByteArray, writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE): Result {
        val gatt = getGatt() ?: return Result.NotConnected
        val service = gatt.getService(serviceUuid) ?: return Result.CharacteristicNotFound
        val characteristic = service.getCharacteristic(characteristicUuid)
            ?: return Result.CharacteristicNotFound

        val writeProps = BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
        if (characteristic.properties.and(writeProps) == 0) {
            return Result.WriteFailed
        }

        return try {
            val writeResult = gatt.writeCharacteristic(
                characteristic,
                bytes,
                writeType
            )
            if (writeResult == BluetoothGatt.GATT_SUCCESS) Result.Success else Result.WriteFailed
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unknown error")
        }
    }
}