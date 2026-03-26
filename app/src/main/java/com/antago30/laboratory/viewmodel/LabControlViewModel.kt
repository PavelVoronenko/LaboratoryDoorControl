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
                Log.d(
                    "BLE_DEBUG",
                    "ViewModel: connectionState=$state → isInterfaceEnabled=$enabled"
                )
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
            StaffMember("1", "ВВ", "Володя", false),
            StaffMember("2", "ВО", "Слава", true),
            StaffMember("3", "ПЕ", "Паша", true),
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

    // Время последнего изменения для каждого сотрудника
    private val staffLastChangeTime = mutableMapOf<String, Long>()

    // Время последнего изменения для освещения
    private var lightLastChangeTime: Long = 0

    init {
        @Suppress("MissingPermission")
        viewModelScope.launch {
            connectionManager.connectionStateFlow.collect { state ->
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
                        _systemMessageData.value =
                            String(data.value.toByteArray(), Charsets.UTF_8).trim()
                        parseSystemMessage(_systemMessageData.value)
                    }

                    BleConnectionManager.TERMINAL_CHARACTERISTIC.toString() -> {
                        _terminalData.value =
                            String(data.value.toByteArray(), Charsets.UTF_8).trim()
                    }
                }
            }
        }
    }

    // Функция парсинга сообщения от контроллера
    private fun parseSystemMessage(message: String) {
        val currentTime = System.currentTimeMillis()
        val parts = message.split("|").filter { it.isNotBlank() }

        for (part in parts) {
            when {
                // Обработка статуса освещения
                part.startsWith("LIGHTSTATUS:") -> {
                    val lightStatus = part.substringAfter("LIGHTSTATUS:").trim()
                    val isLightOn = lightStatus == "1"

                    // Проверяем, не меняли ли мы статус недавно (менее 1 секунд назад)
                    if (currentTime - lightLastChangeTime < 1000) {
                        Log.d("BLE_DEBUG", "⏭️ Ignoring light status (local change pending)")
                        break // Пропускаем обновление
                    }

                    functions.value = functions.value.map { func ->
                        if (func.id == "lighting") {
                            func.copy(isEnabled = isLightOn)
                        } else func
                    }
                    Log.d("BLE_DEBUG", "💡 Light status updated from controller: $isLightOn")
                }

                // Обработка статуса сотрудников
                part.contains("-inside") || part.contains("-outside") -> {
                    val (name, status) = part.split("-").let {
                        it.first() to it.last()
                    }
                    val isInside = status == "inside"

                    // Ищем сотрудника и проверяем время
                    staffList.value = staffList.value.map { staff ->
                        if (staff.name.contains(name, ignoreCase = true) ||
                            staff.initials.equals(name, ignoreCase = true)
                        ) {

                            val lastChange = staffLastChangeTime[staff.id] ?: 0

                            // Если локальное изменение было недавно — игнорируем от контроллера
                            if (currentTime - lastChange < 1000) {
                                staff // Не обновляем
                            } else {
                                staff.copy(isInside = isInside, lastUpdated = currentTime)
                            }
                        } else staff
                    }
                }
            }
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
                        // Логика для рекламы
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
        val currentTime = System.currentTimeMillis()

        staffList.value = staffList.value.map { staff ->
            if (staff.id == id) {
                val newIsInside = !staff.isInside
                val command = buildStaffCommand(staff.initials, newIsInside)

                // Отправляем команду при подключении
                @Suppress("MissingPermission")
                if (connectionManager.connectionStateFlow.value == ConnectionState.READY) {
                    connectionManager.sendCommand(command)
                }

                // Запоминаем время изменения
                staffLastChangeTime[id] = currentTime
                staff.copy(isInside = newIsInside, lastUpdated = currentTime)
            } else staff
        }
    }

    // Функция для построения команды
    private fun buildStaffCommand(nickname: String, isInside: Boolean): String {
        val name = when (nickname.uppercase()) {
            "ПЕ", "ПАША" -> "PASHA"
            "ВО", "СЛАВА" -> "SLAVA"
            "ВВ", "ВОЛОДЯ" -> "VOLODIA"
            else -> return ""
        }

        val status = if (isInside) "INSIDE" else "OUTSIDE"
        return "${name}${status}"
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