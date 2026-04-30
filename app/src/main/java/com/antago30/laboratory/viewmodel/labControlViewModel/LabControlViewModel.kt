package com.antago30.laboratory.viewmodel.labControlViewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.model.FunctionItem
import com.antago30.laboratory.model.StaffMember
import com.antago30.laboratory.util.SettingsRepository
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.AdvertisingServiceUseCase
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.BleDataParsingUseCase
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.FunctionControlUseCase
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.StaffStatusUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class LabControlViewModel(
    private val connectionManager: BleConnectionManager,
    private val staffUseCase: StaffStatusUseCase,
    private val functionUseCase: FunctionControlUseCase,
    private val parsingUseCase: BleDataParsingUseCase,
    private val advertisingUseCase: AdvertisingServiceUseCase,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    companion object {
        private var hasDismissedBatteryWarning = false
    }

    // === UI State ===
    val isInterfaceEnabled: StateFlow<Boolean> = connectionManager.connectionStateFlow
        .map { it == ConnectionState.READY }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val staffList: StateFlow<List<StaffMember>> = staffUseCase.staffList
    val functions: StateFlow<List<FunctionItem>> = functionUseCase.functions

    private var lastKnownUserId: String? = null

    val isAdvertising: StateFlow<Boolean> = advertisingUseCase.isRunning

    // === UI State ===
    private val _controllerToastMessage = MutableStateFlow<String?>(null)
    val controllerToastMessage: StateFlow<String?> = _controllerToastMessage

    private val _showBatteryWarning = MutableStateFlow(false)
    val showBatteryWarning: StateFlow<Boolean> = _showBatteryWarning

    init {
        // Синхронизация состояния тумблера "Вещание" с фактическим состоянием сервиса
        advertisingUseCase.onServiceStateChanged = { isRunning ->
            functionUseCase.setFunctionEnabled("broadcast", isRunning)
            if (isRunning != settingsRepo.isAdvertisingEnabled()) {
                settingsRepo.saveAdvertisingEnabled(isRunning)
            }
        }

        // Наблюдение за списком пользователей от менеджера
        viewModelScope.launch {
            connectionManager.userListFlow.collect { userInfoList ->
                if (userInfoList.isNotEmpty()) {
                    Log.d("LabControlVM", "✅ Received user list update (${userInfoList.size} users)")
                    staffUseCase.syncStaffListFromController(userInfoList)
                }
            }
        }

        @Suppress("MissingPermission")
        viewModelScope.launch {
            connectionManager.connectionStateFlow.collect { state ->
                // Проверяем, находится ли приложение в фокусе
                val isAppVisible = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

                if (state == ConnectionState.READY && isAppVisible) {
                    connectionManager.requestMtu(512)
                    connectionManager.subscribeToSensorData()
                    connectionManager.subscribeToDebugData() // Подписываемся на отладку для проверки батарейки

                    // Небольшая задержка для завершения инициализации GATT
                    delay(50)
                    
                    // Запрашиваем список пользователей при подключении
                    fetchUserListFromController()
                }
            }
        }

        // Проверяем, не установлено ли соединение уже при создании ViewModel
        viewModelScope.launch {
            delay(50) // Небольшая задержка для инициализации
            if (connectionManager.connectionStateFlow.value == ConnectionState.READY) {
                Log.d("LabControlVM", "🔗 Соединение уже установлено, запрашиваем список пользователей")
                fetchUserListFromController()
            }
        }

        viewModelScope.launch {
            connectionManager.characteristicData.collect { data ->
                // Если приложение в фоне, игнорируем данные
                if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    return@collect
                }

                parsingUseCase.processData(data)
            }
        }

        // Обработка Terminal characteristic (логи от контроллера)
        viewModelScope.launch {
            connectionManager.terminalData.collect { data ->
                // Если приложение в фоне, игнорируем логи
                if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    return@collect
                }

                val response = try {
                    String(data.value.toByteArray(), StandardCharsets.UTF_8).trim()
                } catch (_: Exception) { "" }

                // Игнорируем историю логов и пустые сообщения
                if (response.isEmpty() || response.startsWith("LOG_HIST:")) return@collect

                // Показываем toast ТОЛЬКО для освещения
                if (response.contains("Освещение") || 
                    response.contains("JDE-33") ||
                    response.contains("Автоматическое")) {
                    
                    // Очищаем сообщение от меток времени [HH:mm:ss] и тегов [I], [W], [D]
                    val cleanMessage = response
                        .replace(Regex("""^\[\d{2}:\d{2}:\d{2}]\s*"""), "") // Удаляем время
                        .replace(Regex("""^\[[A-Z]]\s*"""), "")              // Удаляем тег типа
                        .trim()

                    _controllerToastMessage.value = cleanMessage
                }
            }
        }

        // Обработка Debug characteristic (для проверки батарейки)
        viewModelScope.launch {
            connectionManager.debugData.collect { data ->
                val response = String(data.value.toByteArray(), StandardCharsets.UTF_8).trim()
                if (response.contains("BAT:0")) {
                    if (!hasDismissedBatteryWarning) {
                        _showBatteryWarning.value = true
                    }
                } else if (response.contains("BAT:1")) {
                    _showBatteryWarning.value = false
                    hasDismissedBatteryWarning = false // Сбрасываем флаг, если батарейка теперь в норме
                }
            }
        }

        viewModelScope.launch {
            settingsRepo.currentUserIdFlow.collect { userId ->
                if (lastKnownUserId != null && userId != lastKnownUserId) {
                    // Пользователь изменился → перезапустить рекламу с новыми данными
                    if (advertisingUseCase.isRunning.value) {
                        advertisingUseCase.onUserChanged()
                    }
                }
                lastKnownUserId = userId
            }
        }
    }

    // === Запрос списка пользователей от контроллера ===
    @Suppress("MissingPermission")
    private fun fetchUserListFromController() {
        val connectionState = connectionManager.connectionStateFlow.value
        Log.d("LabControlVM", "📤 Отправляю LISTUSERS (состояние: $connectionState)")
        val result = connectionManager.sendCommand("LISTUSERS")
        Log.d("LabControlVM", "📥 Результат отправки LISTUSERS: $result")
    }

    fun setAppContext(context: Context) {
        advertisingUseCase.setContext(context)
    }

    // === Actions ===
    @Suppress("MissingPermission")
    fun toggleStaffStatus(id: String) {
        staffUseCase.toggleStaffStatus(id) { command ->
            connectionManager.sendCommand(command)
        }
    }

    @Suppress("MissingPermission")
    fun toggleFunction(id: String) {
        functionUseCase.toggleFunction(id) { command ->
            connectionManager.sendCommand(command)
        }
    }

    @Suppress("MissingPermission")
    fun onOpenDoorClicked() {
        if (connectionManager.connectionStateFlow.value != ConnectionState.READY) return
        connectionManager.sendCommand("OPENDOOR")
    }

    fun startBleAdvertising() {
        // Проверяем выбранного пользователя перед запуском
        if (getCurrentUser() == null) {
            showSystemMessage("Выберите текущего сотрудника")
            functionUseCase.setFunctionEnabled("broadcast", false)
            settingsRepo.saveAdvertisingEnabled(false)
            return
        }
        settingsRepo.saveAdvertisingEnabled(true)
        advertisingUseCase.start()
        // Обновляем тумблер на включено
        functionUseCase.setFunctionEnabled("broadcast", true)
    }

    fun stopBleAdvertising() {
        settingsRepo.saveAdvertisingEnabled(false)
        advertisingUseCase.stop()
        functionUseCase.setFunctionEnabled("broadcast", false)
    }

    fun syncServiceState() {
        advertisingUseCase.syncState()
    }

    fun onUserSelected() {
        if (advertisingUseCase.isRunning.value) {
            advertisingUseCase.onUserChanged()
        }
    }

    fun getCurrentUser(): StaffMember? {
        val staffListValue = staffList.value
        return settingsRepo.getCurrentUserInfo()?.let { userInfo ->
            staffListValue.find { it.name.equals(userInfo.name, ignoreCase = true) }
        }
    }

    fun showSystemMessage(message: String) {
        _controllerToastMessage.value = message
    }

    fun clearControllerToastMessage() {
        _controllerToastMessage.value = null
    }

    fun dismissBatteryWarning() {
        _showBatteryWarning.value = false
        hasDismissedBatteryWarning = true
    }
}
