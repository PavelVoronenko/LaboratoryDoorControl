package com.antago30.laboratory.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.antago30.laboratory.model.BleDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.bluetooth.BluetoothManager

class BleScanner(private val context: Context?) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothManager?.adapter
    }
    private val _scanResults = MutableStateFlow<List<BleDevice>>(emptyList())
    val scanResults: StateFlow<List<BleDevice>> = _scanResults.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val newDevice = BleDevice(
                name = device.name,
                address = device.address,
                rssi = result.rssi
            )
            // Обновляем список, избегая дубликатов по адресу
            val current = _scanResults.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.address == newDevice.address }
            if (existingIndex >= 0) {
                //current[existingIndex] = newDevice // Обновляем RSSI
            } else {
                current.add(newDevice)
            }
            _scanResults.value = current.sortedByDescending { it.rssi }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan(durationMs: Long = 10_000) {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) return

        _scanResults.value = emptyList() // Очистка перед сканированием

        val scanner = bluetoothAdapter!!.bluetoothLeScanner ?: return
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        val filters = listOf<ScanFilter>()

        scanner.startScan(filters, settings, scanCallback)

        // Авто-остановка через durationMs
        Handler(Looper.getMainLooper()).postDelayed({
            stopScan()
        }, durationMs)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            // Игнорируем ошибки при остановке
        }
    }
}