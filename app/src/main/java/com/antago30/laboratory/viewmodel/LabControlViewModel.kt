package com.antago30.laboratory.viewmodel

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antago30.laboratory.ble.BleAdvertisingService
import com.antago30.laboratory.ble.BleDevice
import com.antago30.laboratory.ble.BleScanner
import com.antago30.laboratory.model.FunctionItem
import com.antago30.laboratory.model.StaffMember
import com.antago30.laboratory.util.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LabControlViewModel(
    private val context: Context,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    // 🔹 BLE Scanner
    private val bleScanner = BleScanner(context)
    private var scanJob: Job? = null

    // 🔹 Состояние для выбора BLE-устройства (настройки)
    var selectedDeviceName by mutableStateOf<String?>(null)
        private set
    var selectedDeviceAddress by mutableStateOf<String?>(null)
        private set
    var isScanning by mutableStateOf(false)
        private set
    var availableDevices by mutableStateOf<List<BleDevice>>(emptyList())
        private set

    //Инициализация: загружаем сохранённое устройство
    init {
        loadSavedDevice()
    }

    //Загрузка сохранённого устройства из SharedPreferences
    private fun loadSavedDevice() {
        val device = settingsRepo.getSelectedDevice()
        selectedDeviceName = device?.first
        selectedDeviceAddress = device?.second
    }

    //апуск сканирования BLE-устройств
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startDeviceScan() {
        isScanning = true
        availableDevices = emptyList()

        bleScanner.startScan(durationMs = 15_000)

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            bleScanner.scanResults.collect { devices ->
                availableDevices = devices
            }
        }
    }

    // 🔹 Остановка сканирования
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopDeviceScan() {
        isScanning = false
        bleScanner.stopScan()
        scanJob?.cancel()
    }

    // 🔹 Выбор устройства из списка
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun selectDevice(device: BleDevice) {
        selectedDeviceName = device.name
        selectedDeviceAddress = device.address

        settingsRepo.saveSelectedDevice(
            deviceAddress = device.address,
            deviceName = device.name ?: "Unknown"
        )
        stopDeviceScan()
    }

    // 🔹 Очистка выбранного устройства
    fun clearSelectedDevice() {
        selectedDeviceName = null
        selectedDeviceAddress = null
        settingsRepo.clearSelectedDevice()
    }

    // 🔹 Проверка разрешений Bluetooth (для Android 12+)
    fun checkBlePermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connect = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            val scan = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            connect && scan
        } else {
            // Для Android 11 и ниже
            val bluetooth = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
            val location = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            bluetooth && location
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🔹 Существующая логика приложения
    // ─────────────────────────────────────────────────────────────

    val staffList = mutableStateOf(
        listOf(
            StaffMember("1", "ВВ", "Владимир Викторович", true),
            StaffMember("2", "ВО", "Вячеслав Олегович", false),
            StaffMember("3", "ПЕ", "Павел Евгеньевич", true)
        )
    )

    val functions = mutableStateOf(
        listOf(
            FunctionItem("broadcast", "📡 Вещание рекламы", false),
            FunctionItem("cleaning", "🧹 Режим уборки", false),
            FunctionItem("lighting", "💡 Освещение", false)
        )
    )

    private var appContext: Context? = null
    private val _isServiceRunning = mutableStateOf(false)
    val isServiceRunning: Boolean get() = _isServiceRunning.value

    fun setAppContext(context: Context) {
        appContext = context.applicationContext
        syncServiceState()
    }

    private fun checkIfServiceIsRunning(): Boolean {
        return appContext?.let { ctx ->
            val manager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            manager.getRunningServices(Int.MAX_VALUE).any {
                BleAdvertisingService::class.java.name == it.service.className
            }
        } ?: false
    }

    fun syncServiceState() {
        _isServiceRunning.value = checkIfServiceIsRunning()
        val broadcastEnabled = functions.value.find { it.id == "broadcast" }?.isEnabled == true
        if (_isServiceRunning.value && !broadcastEnabled) {
            toggleFunction("broadcast")
        } else if (!_isServiceRunning.value && broadcastEnabled) {
            toggleFunction("broadcast")
        }
    }

    fun toggleFunction(id: String) {
        functions.value = functions.value.map { func ->
            if (func.id == id) {
                val newEnabled = !func.isEnabled
                if (id == "broadcast") {
                    if (newEnabled) startBleAdvertising()
                    else stopBleAdvertising()
                }
                func.copy(isEnabled = newEnabled)
            } else func
        }
    }

    fun toggleStaffStatus(id: String) {
        staffList.value = staffList.value.map { staff ->
            if (staff.id == id) staff.copy(isInside = !staff.isInside) else staff
        }
    }

    fun startBleAdvertising() {
        appContext?.let { ctx ->
            val intent = Intent(ctx, BleAdvertisingService::class.java)
            ctx.startForegroundService(intent)
            _isServiceRunning.value = true
        }
    }

    fun stopBleAdvertising() {
        appContext?.let { ctx ->
            val intent = Intent(ctx, BleAdvertisingService::class.java)
            ctx.stopService(intent)
            _isServiceRunning.value = false
        }
    }

    val isAdvertising: Boolean get() = _isServiceRunning.value

    fun onOpenDoorClicked() {
        // TODO: Реализовать логику открытия двери
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onCleared() {
        super.onCleared()
        stopDeviceScan()
    }
}