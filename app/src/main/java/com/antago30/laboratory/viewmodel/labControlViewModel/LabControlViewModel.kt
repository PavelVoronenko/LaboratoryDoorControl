package com.antago30.laboratory.viewmodel.labControlViewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.model.FunctionItem
import com.antago30.laboratory.model.StaffMember
import com.antago30.laboratory.model.UserInfo
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

    // === UI State ===
    val isInterfaceEnabled: StateFlow<Boolean> = connectionManager.connectionStateFlow
        .map { it == ConnectionState.READY }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val staffList: StateFlow<List<StaffMember>> = staffUseCase.staffList
    val functions: StateFlow<List<FunctionItem>> = functionUseCase.functions

    private var lastKnownUserId: String? = null

    val isAdvertising: StateFlow<Boolean> = advertisingUseCase.isRunning

    // Flow для показа toast уведомления от контроллера
    private val _controllerToastMessage = MutableStateFlow<String?>(null)
    val controllerToastMessage: StateFlow<String?> = _controllerToastMessage

    // === Буфер для сборки USERLIST чанков ===
    private val userListChunks = mutableMapOf<Int, String>()
    private var userListReceiving = false
    private var userListLastReceived = 0L
    private val chunckTimeoutMs = 2000L

    init {
        @Suppress("MissingPermission")
        viewModelScope.launch {
            connectionManager.connectionStateFlow.collect { state ->
                if (state == ConnectionState.READY) {
                    connectionManager.requestMtu(512)
                    connectionManager.subscribeToSensorData()

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
                val response = String(data.value.toByteArray(), StandardCharsets.UTF_8).trim()
                Log.d("LabControlVM", "📥 Received: '$response' (UUID: ${data.uuid})")

                // Проверяем, это USERLIST чанк?
                if (response.startsWith("USERLIST_PKT:") || response.startsWith("USERLIST:")) {
                    handleUserListChunk(response)
                } else {
                    parsingUseCase.processData(data)
                }
            }
        }

        // Обработка Terminal characteristic (логи от контроллера)
        viewModelScope.launch {
            connectionManager.terminalData.collect { data ->
                val response = String(data.value.toByteArray(), StandardCharsets.UTF_8).trim()
                // Показываем toast для сообщений об освещении и JDE-33
                if (response.contains("Освещение") || 
                    response.contains("JDE-33")) {
                    _controllerToastMessage.value = response
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

    // === Обработка чанков USERLIST ===
    private fun handleUserListChunk(packet: String) {
        try {
            if (packet.startsWith("USERLIST:")) {
                // Простой формат
                val data = packet.removePrefix("USERLIST:")
                parseAndSyncUserList(data)
                return
            }

            // Чанкнутый формат: USERLIST_PKT:0/2|...
            val headerEnd = packet.indexOf('|')
            if (headerEnd == -1) return

            val header = packet.substring(13, headerEnd)
            val headerParts = header.split('/')
            val chunkIndex = headerParts[0].toInt()
            val totalChunks = if (headerParts.size > 1) headerParts[1].toIntOrNull() ?: -1 else -1

            val dataStart = headerEnd + 1
            val hasEnd = packet.endsWith("|END")

            val dataEnd = if (hasEnd) packet.length - 4 else packet.length
            val chunkData = if (dataEnd <= dataStart) "" else packet.substring(dataStart, dataEnd)

            Log.d("LabControlVM", "📦 Chunk $chunkIndex/$totalChunks received (${chunkData.length} bytes)")

            if (chunkIndex == 0 && chunkData.isEmpty() && hasEnd) {
                Log.d("LabControlVM", "📭 Empty user list received")
                resetUserListBuffer()
                return
            }

            if (chunkIndex == 0) {
                userListChunks.clear()
                userListReceiving = true
                checkChunkTimeout()
            }

            userListChunks[chunkIndex] = chunkData
            userListLastReceived = System.currentTimeMillis()

            if (hasEnd) {
                Log.d("LabControlVM", "🏁 Received END marker, assembling ${userListChunks.size} chunks...")
                assembleAndParseUserList()
            }
        } catch (e: Exception) {
            Log.e("LabControlVM", "❌ Error parsing USERLIST chunk", e)
            resetUserListBuffer()
        }
    }

    private fun assembleAndParseUserList() {
        val sortedData = userListChunks.toSortedMap().values.joinToString("")
        Log.d("LabControlVM", "🔗 Full assembled data (${sortedData.length} bytes)")
        resetUserListBuffer()

        if (sortedData.isEmpty()) {
            Log.d("LabControlVM", "📭 Empty list after assembly")
            return
        }

        parseAndSyncUserList(sortedData)
    }

    private fun parseAndSyncUserList(data: String) {
        if (data.isBlank() || data == "|") {
            Log.d("LabControlVM", "📭 Empty user list data")
            return
        }

        val userInfoList = data.split("|")
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split(",")
                if (parts.size >= 6) {
                    val id = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val name = parts[1].trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val mac = parts[2].trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val location = parts[3].trim()
                    val uuid = parts[4].trim()
                    val serviceData = parts[5].trim()

                    UserInfo(
                        id = id,
                        name = name,
                        macAddress = mac,
                        location = location,
                        uuid = uuid,
                        serviceData = serviceData
                    )
                } else {
                    Log.w("LabControlVM", "⚠️ Invalid entry (${parts.size} parts): ${entry.take(50)}")
                    null
                }
            }
            .distinctBy { it.id }

        if (userInfoList.isNotEmpty()) {
            Log.d("LabControlVM", "✅ Parsing ${userInfoList.size} users, syncing to staff list...")
            staffUseCase.syncStaffListFromController(userInfoList)
        } else {
            Log.d("LabControlVM", "📭 No valid users parsed")
        }
    }

    private fun resetUserListBuffer() {
        userListChunks.clear()
        userListReceiving = false
    }

    private fun checkChunkTimeout() {
        viewModelScope.launch {
            delay(chunckTimeoutMs)
            if (userListReceiving && userListChunks.isNotEmpty()) {
                Log.w("LabControlVM", "⚠️ USERLIST chunk timeout! Assembling partial data...")
                assembleAndParseUserList()
            }
        }
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
            // Возвращаем тумблер в выключенное состояние
            functionUseCase.setFunctionEnabled("broadcast", false)
            return
        }
        advertisingUseCase.start()
    }

    fun stopBleAdvertising() {
        advertisingUseCase.stop()
    }

    fun syncServiceState() {
        advertisingUseCase.syncState()
    }

    fun onUserSelected() {
        if (advertisingUseCase.isRunning.value) {
            advertisingUseCase.onUserChanged()
        }
    }

    fun addNewStaffMember(newMember: StaffMember): Boolean {
        return staffUseCase.addStaffMember(newMember)
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
}