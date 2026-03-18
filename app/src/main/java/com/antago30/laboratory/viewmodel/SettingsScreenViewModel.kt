package com.antago30.laboratory.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antago30.laboratory.ble.BleScanner
import com.antago30.laboratory.model.BleDevice
import com.antago30.laboratory.util.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsScreenViewModel(
    private val settingsRepo: SettingsRepository,
) : ViewModel() {
    private var appContext: Context? = null
    // LE Scanner
    private var bleScanner: BleScanner? = null
    private var scanJob: Job? = null
    private var scanStopTimerJob: Job? = null // <-- Добавим отдельный Job для таймера

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
    fun startDeviceScan(durationMs: Long = 10_000) { // <-- Сделаем длительность параметром
        val scanner = bleScanner ?: return

        // Отменяем предыдущие задачи, если они были
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
            stopDeviceScanInternal() // Вызываем внутренний метод
        }
    }

    // Остановка сканирования (публичный метод)
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopDeviceScan() {
        stopDeviceScanInternal()
    }

    // Внутренний метод для остановки
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopDeviceScanInternal() {
        val scanner = bleScanner ?: return
        isScanning = false // <-- Устанавливаем isScanning в false
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
        stopDeviceScan() // <-- Останавливаем сканирование при выборе
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
        stopDeviceScanInternal() // <-- Используем внутренний метод
    }
}