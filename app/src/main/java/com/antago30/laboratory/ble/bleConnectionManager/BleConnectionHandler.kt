package com.antago30.laboratory.ble.bleConnectionManager

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

class BleConnectionHandler(
    private val context: Context,
    private val callback: BleGattCallbackHandler,
) {
    private var gatt: BluetoothGatt? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice, autoConnect: Boolean = false): Boolean {
        return try {
            gatt = device.connectGatt(context, autoConnect, callback, BluetoothDevice.TRANSPORT_LE)
            gatt != null
        } catch (e: SecurityException) {
            android.util.Log.e("BleConnectionHandler", "❌ connect: Permission denied", e)
            false
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect(): DisconnectResult {
        return try {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                return DisconnectResult.PermissionDenied
            }

            gatt?.let {
                android.util.Log.d("BleConnectionHandler", "🧹 Closing GATT connection...")
                it.disconnect()
                it.close()
            }

            gatt = null
            DisconnectResult.Success
        } catch (e: SecurityException) {
            android.util.Log.e("BleConnectionHandler", "❌ disconnect: SecurityException", e)
            DisconnectResult.PermissionDenied
        } catch (e: Exception) {
            android.util.Log.e("BleConnectionHandler", "❌ disconnect: Unexpected error", e)
            DisconnectResult.Error(e.message ?: "Unknown error")
        }
    }

    fun getGatt(): BluetoothGatt? = gatt

    // Результат операции отключения
    sealed class DisconnectResult {
        object Success : DisconnectResult()
        object PermissionDenied : DisconnectResult()
        data class Error(val message: String) : DisconnectResult()
    }
}