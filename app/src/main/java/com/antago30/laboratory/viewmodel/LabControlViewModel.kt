package com.antago30.laboratory.viewmodel

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antago30.laboratory.ble.BleAdvertisingService
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.model.FunctionItem
import com.antago30.laboratory.model.StaffMember
import com.antago30.laboratory.util.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LabControlViewModel(
    private val settingsRepo: SettingsRepository,
    private val connectionManager: BleConnectionManager,
) : ViewModel() {

    private val _uiEvents = MutableSharedFlow<UiEvent>()

    val isInterfaceEnabled: StateFlow<Boolean> =
        connectionManager.connectionStateFlow
            .map { state ->
                val enabled = state == ConnectionState.READY
                Log.d("BLE_DEBUG", "ViewModel: connectionState=$state → isInterfaceEnabled=$enabled")
                enabled
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // События для UI
    sealed class UiEvent {
        object RequestBlePermissions : UiEvent()
        data class ShowError(val message: String) : UiEvent()
    }

    val staffList = mutableStateOf(
        listOf(
            StaffMember("1", "ВВ", "Владимир Викторович", false),
            StaffMember("2", "ВО", "Вячеслав Олегович", true),
            StaffMember("3", "ПЕ", "Павел Евгеньевич", true),
        )
    )

    val functions = mutableStateOf(
        listOf(
            FunctionItem("broadcast", "📡 Вещание рекламы", false),
            FunctionItem("lighting", "💡 Освещение", false)
        )
    )

    private var appContext: Context? = null
    private val _isServiceRunning = mutableStateOf(false)

    private val _systemMessageData = MutableStateFlow("—")
    private val _terminalData = MutableStateFlow("—")
    val systemMessageData: StateFlow<String> = _systemMessageData.asStateFlow()
    val terminalData: StateFlow<String> = _terminalData.asStateFlow()

    init {
        @Suppress("MissingPermission")
        viewModelScope.launch {
            connectionManager.connectionStateFlow.collect { state ->
                Log.d("BLE_DEBUG", "🔁 ViewModel COLLECT: $state")

                if (state == ConnectionState.READY) {
                    connectionManager.requestMtu(200)
                    connectionManager.subscribeToSensorData()
                }
            }
        }

        // Слушаем входящие данные
        viewModelScope.launch {
            connectionManager.characteristicData.collect { data ->
                when (data.uuid) {
                    BleConnectionManager.SYSTEM_MESSAGE_CHARACTERISTIC.toString() -> {
                        _systemMessageData.value = formatSensorData(data.value)
                    }
                    BleConnectionManager.TERMINAL_CHARACTERISTIC.toString() -> {
                        _terminalData.value = formatSensorData(data.value)
                    }
                }
            }
        }
    }

    private fun formatSensorData(bytes: List<Byte>): String {
        return try {
            val string = String(bytes.toByteArray(), Charsets.UTF_8).trim()
            string.ifEmpty { "—" }
        } catch (e: Exception) {
            bytes.joinToString(" ") { String.format("%02X", it) }
        }
    }

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

                when (id) {
                    "broadcast" -> {
                        // Логика для рекламы (без изменений)
                        if (newEnabled) startBleAdvertising()
                        else stopBleAdvertising()
                    }
                    "lighting" -> {
                        // Логика для освещения
                        @Suppress("MissingPermission")
                        if (connectionManager.connectionStateFlow.value == ConnectionState.READY) {
                            val command = if (newEnabled) "LIGHTON" else "LIGHTOFF"
                            connectionManager.sendCommand(command)
                        }
                    }
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun onOpenDoorClicked() {
        if (connectionManager.connectionStateFlow.value != ConnectionState.READY) return
        connectionManager.sendCommand("OPENDOOR")
    }
}