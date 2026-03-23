package com.antago30.laboratory.viewmodel

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.ble.BleScanner
import com.antago30.laboratory.model.BleDevice
import com.antago30.laboratory.util.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsScreenViewModel(
    private val settingsRepo: SettingsRepository,
    private val connectionManager: BleConnectionManager
) : ViewModel() {
    private var appContext: Context? = null
    // LE Scanner
    private var bleScanner: BleScanner? = null
    private var scanJob: Job? = null
    private var scanStopTimerJob: Job? = null

    // Состояние для выбора BLE-устройства (настройки)
    var selectedDeviceName by mutableStateOf<String?>(null)
        private set
    var selectedDeviceAddress by mutableStateOf<String?>(null)
        private set
    var isScanning by mutableStateOf(false)
        private set
    var availableDevices by mutableStateOf<List<BleDevice>>(emptyList())
        private set

    // Инициализация: загружаем сохранённое устройство
    init {
        loadSavedDevice()
    }

    // Загрузка сохранённого устройства из SharedPreferences
    private fun loadSavedDevice() {
        val device = settingsRepo.getSelectedDevice()
        selectedDeviceName = device?.first
        selectedDeviceAddress = device?.second
    }

    // Запуск сканирования BLE-устройств
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startDeviceScan(durationMs: Long = 10_000) {
        val scanner = bleScanner ?: return

        scanJob?.cancel()
        scanStopTimerJob?.cancel()

        isScanning = true
        availableDevices = emptyList()

        scanner.startScan(durationMs = durationMs)

        // Запускаем задачу для получения результатов
        scanJob = viewModelScope.launch {
            scanner.scanResults.collect { devices ->
                availableDevices = devices
            }
        }

        // Запускаем таймер для остановки сканирования
        scanStopTimerJob = viewModelScope.launch {
            delay(durationMs) // Ждём указанное время
            // После задержки останавливаем сканирование
            stopDeviceScanInternal()
        }
    }

    // Остановка сканирования
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopDeviceScan() {
        stopDeviceScanInternal()
    }

    // Внутренний метод для остановки
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopDeviceScanInternal() {
        val scanner = bleScanner ?: return
        isScanning = false
        scanner.stopScan()
        scanJob?.cancel()
        scanStopTimerJob?.cancel()
    }

    // Выбор устройства из списка
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun selectDevice(device: BleDevice) {
        selectedDeviceName = device.name
        selectedDeviceAddress = device.address

        settingsRepo.saveSelectedDevice(
            deviceAddress = device.address,
            deviceName = device.name ?: "Unknown"
        )

        val adapter = BluetoothAdapter.getDefaultAdapter()
        val nativeDevice = adapter?.getRemoteDevice(device.address)

        nativeDevice?.let {
            // Проверка разрешений внутри connect()
            connectionManager.connect(it, autoConnect = false)
        }

        stopDeviceScan()
    }

    // Очистка выбранного устройства
    fun clearSelectedDevice() {
        selectedDeviceName = null
        selectedDeviceAddress = null
        settingsRepo.clearSelectedDevice()
    }

    fun checkBlePermissions(context: Context): Boolean {
        val connectGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        val scanGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED

        return connectGranted && scanGranted
    }

    fun setAppContext(context: Context) {
        appContext = context.applicationContext
        bleScanner = BleScanner(appContext)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onCleared() {
        super.onCleared()
        stopDeviceScanInternal()
    }
}