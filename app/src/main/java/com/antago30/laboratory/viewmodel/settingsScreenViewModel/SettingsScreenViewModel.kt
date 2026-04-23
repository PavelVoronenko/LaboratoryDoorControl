package com.antago30.laboratory.viewmodel.settingsScreenViewModel

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.ble.BleScanner
import com.antago30.laboratory.model.BleDevice
import com.antago30.laboratory.util.SettingsRepository
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.FunctionControlUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.antago30.laboratory.ui.component.settingsScreen.terminalLog.LogType
import com.antago30.laboratory.ui.component.settingsScreen.terminalLog.TerminalLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SettingsScreenViewModel(
    private val settingsRepo: SettingsRepository,
    private val connectionManager: BleConnectionManager,
    val functionUseCase: FunctionControlUseCase
) : ViewModel() {

    companion object {
        private val TIME_REGEX = Regex("""\[(\d{2}:\d{2}:\d{2})]""")
        private val TYPE_REGEX = Regex("""\[([IDWU])]""")
        private val QUOTE_REGEX = Regex("'([^']+)'")
        private val NAME_REGEX = Regex("^([А-ЯЁ][а-яё]+\\s[А-ЯЁ][а-яё]+|^[А-ЯЁ][а-яё]+)")
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
        private const val MAX_LOGS = 1000
    }

    private var appContext: Context? = null
    private var bleScanner: BleScanner? = null
    private var scanJob: Job? = null
    private var scanStopTimerJob: Job? = null

    private val _selectedDeviceAddress = MutableStateFlow(settingsRepo.getSelectedDeviceAddress())
    val selectedDeviceAddress: StateFlow<String?> = _selectedDeviceAddress.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<TerminalLogEntry>>(emptyList())
    val terminalLogs: StateFlow<List<TerminalLogEntry>> = _terminalLogs.asStateFlow()

    private val _isTerminalActive = MutableStateFlow(false)
    val isTerminalActive: StateFlow<Boolean> = _isTerminalActive.asStateFlow()

    var selectedDeviceName by mutableStateOf<String?>(null)
        private set

    var isScanning by mutableStateOf(false)
        private set
    var availableDevices by mutableStateOf<List<BleDevice>>(emptyList())
        private set

    private var isHistoryLoading = false
    private var historyTimeoutJob: Job? = null

    private var terminalObservationJob: Job? = null

    init {
        loadSavedDevice()
    }

    private fun parseLogEntry(raw: String): TerminalLogEntry {
        val timeMatch = TIME_REGEX.find(raw)
        val typeMatch = TYPE_REGEX.find(raw)

        val timeStr = timeMatch?.groupValues?.get(1)
        val typeChar = typeMatch?.groupValues?.get(1) ?: "I"

        val timestamp = try {
            if (timeStr != null) LocalTime.parse(timeStr, TIME_FORMATTER) else LocalTime.now()
        } catch (e: Exception) {
            LocalTime.now()
        }

        val type = when (typeChar) {
            "D" -> LogType.DOOR
            "W" -> LogType.WARNING
            "U" -> LogType.USER
            else -> LogType.INFO
        }

        var cleanMessage = raw
        timeMatch?.let { cleanMessage = cleanMessage.replace(it.value, "") }
        typeMatch?.let { cleanMessage = cleanMessage.replace(it.value, "") }
        cleanMessage = cleanMessage.trim()

        var userName: String? = null
        if (type == LogType.USER || cleanMessage.contains("обнаружен")) {
            val quoteMatch = QUOTE_REGEX.find(cleanMessage)
            if (quoteMatch != null) {
                userName = quoteMatch.groupValues[1]
            } else {
                val nameMatch = NAME_REGEX.find(cleanMessage.trim())
                if (nameMatch != null) {
                    userName = nameMatch.value
                }
            }
        }

        return TerminalLogEntry(
            message = cleanMessage,
            type = type,
            timestamp = timestamp,
            userName = userName
        )
    }

    fun startTerminalObservation() {
        if (terminalObservationJob?.isActive == true) return

        terminalObservationJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            connectionManager.terminalData.collect { characteristic ->
                val bytes = characteristic.value
                val rawMessage = String(bytes.toByteArray(), Charsets.UTF_8).trim()
                
                if (rawMessage.isEmpty()) return@collect

                // Визуальный эффект приёма данных (моргание)
                viewModelScope.launch {
                    _isTerminalActive.value = true
                    delay(200)
                    _isTerminalActive.value = false
                }

                if (rawMessage.startsWith("LOG_HIST:")) {
                    handleHistoryLog(rawMessage)
                } else {
                    val lines = rawMessage.split("\n").filter { it.isNotBlank() }
                    val newEntries = lines.map { parseLogEntry(it) }
                    
                    _terminalLogs.update { (newEntries + it).take(MAX_LOGS) }
                }
            }
        }
    }

    fun stopTerminalObservation() {
        terminalObservationJob?.cancel()
        terminalObservationJob = null
        _terminalLogs.value = emptyList()
    }

    private fun handleHistoryLog(raw: String) {
        if (raw == "LOG_HIST:END") {
            isHistoryLoading = false
            historyTimeoutJob?.cancel()
            return
        }

        try {
            val content = raw.substringAfter("|")
            if (content.isNotEmpty()) {
                val lines = if (raw.contains("BATCH")) {
                    content.split("\n").filter { it.isNotBlank() }
                } else {
                    listOf(content)
                }
                val newEntries = lines.map { parseLogEntry(it) }
                
                // ИСТОРИЯ добавляется В КОНЕЦ текущего списка логов.
                // Теперь контроллер шлет логи от новых к старым (Newest -> Oldest),
                // поэтому просто добавляем их в хвост для соблюдения хронологии.
                _terminalLogs.update { (it + newEntries).take(MAX_LOGS) }
            }
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error parsing history log: $raw", e)
        }
    }

    @Suppress("MissingPermission")
    fun requestLogHistory() {
        startTerminalObservation() // Убеждаемся, что наблюдение запущено
        _terminalLogs.value = emptyList()
        isHistoryLoading = true
        
        historyTimeoutJob?.cancel()
        historyTimeoutJob = viewModelScope.launch {
            delay(5000) // 5 секунд тайм-аут на загрузку
            isHistoryLoading = false
        }

        viewModelScope.launch {
            connectionManager.sendCommand("LOGLIST")
        }
    }

    @Suppress("MissingPermission")
    fun reconnectJde() {
        viewModelScope.launch {
            connectionManager.sendCommand("RECONNECT_JDE")
        }
    }

    @Suppress("MissingPermission")
    fun rebootController() {
        viewModelScope.launch {
            connectionManager.sendCommand("REBOOT")
        }
    }

    fun clearLogs() {
        _terminalLogs.value = emptyList()
    }

    private fun loadSavedDevice() {
        val device = settingsRepo.getSelectedDevice()
        selectedDeviceName = device?.first
        _selectedDeviceAddress.value = device?.second
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startDeviceScan(durationMs: Long = 10_000) {
        val scanner = bleScanner ?: return
        scanJob?.cancel()
        scanStopTimerJob?.cancel()
        isScanning = true
        availableDevices = emptyList()
        scanner.startScan(durationMs = durationMs)
        scanJob = viewModelScope.launch {
            scanner.scanResults.collect { devices ->
                availableDevices = devices
            }
        }
        scanStopTimerJob = viewModelScope.launch {
            delay(durationMs)
            stopDeviceScanInternal()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopDeviceScan() {
        stopDeviceScanInternal()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopDeviceScanInternal() {
        val scanner = bleScanner ?: return
        isScanning = false
        scanner.stopScan()
        scanJob?.cancel()
        scanStopTimerJob?.cancel()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun selectDevice(device: BleDevice) {
        selectedDeviceName = device.name
        _selectedDeviceAddress.value = device.address
        settingsRepo.saveSelectedDevice(deviceAddress = device.address, deviceName = device.name ?: "Unknown")
        val bluetoothManager = appContext?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val nativeDevice = bluetoothManager?.adapter?.getRemoteDevice(device.address)
        nativeDevice?.let {
            try {
                if (checkBlePermissionsForConnect(appContext!!)) {
                    connectionManager.connect(it, autoConnect = false)
                }
            } catch (e: SecurityException) {
                Log.e("SettingsScreenViewModel", "Security exception during connect: ${e.message}")
            }
        }
        stopDeviceScan()
    }

    private fun checkBlePermissionsForConnect(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    fun checkBlePermissions(context: Context): Boolean {
        val connectGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        val scanGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        return connectGranted && scanGranted
    }

    fun setAppContext(context: Context) {
        appContext = context.applicationContext
        bleScanner = BleScanner(appContext)
    }

    @Suppress("MissingPermission")
    override fun onCleared() {
        super.onCleared()
        stopDeviceScanInternal()
        historyTimeoutJob?.cancel()
        try {
            connectionManager.disconnect()
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error during disconnect onCleared", e)
        }
    }
}
