package com.antago30.laboratory.viewmodel.manageUsersViewModel.kt

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.ble.bleConnectionManager.BleCommandSender
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.model.NewUserParams
import com.antago30.laboratory.model.UserInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class UserManagementViewModel(
    private val connectionManager: BleConnectionManager
) : ViewModel() {

    private val _users = MutableStateFlow<List<UserInfo>>(emptyList())
    val users: StateFlow<List<UserInfo>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showError = MutableStateFlow<String?>(null)
    val showError: StateFlow<String?> = _showError.asStateFlow()

    private val _isAddingUser = MutableStateFlow(false)
    val isAddingUser: StateFlow<Boolean> = _isAddingUser.asStateFlow()

    // === Буфер для сборки чанков USERLIST ===
    private val userListChunks = mutableMapOf<Int, String>() // key: index, value: data
    private var userListTotalChunks = 0
    private var userListReceiving = false
    private var userListLastReceived = 0L
    private val CHUNK_TIMEOUT_MS = 2000L  // Тайм-аут сборки: 2 секунды

    // === Предустановленные значения (из AddUserViewModel) ===
    val uuidOptions = listOf(
        "0000ff12-0000-1000-8000-00805f9b34fb",
        "0000ff42-0000-1000-8000-00805f9b34fb",
        "0000ffef-0000-1000-8000-00805f9b34fb",
        "0000ff01-0000-1000-8000-00805f9b34fb",
        "0000ff02-0000-1000-8000-00805f9b34fb",
        "0000ff03-0000-1000-8000-00805f9b34fb",
        "0000ff04-0000-1000-8000-00805f9b34fb",
        "0000ff05-0000-1000-8000-00805f9b34fb",
        "0000ff06-0000-1000-8000-00805f9b34fb",
        "0000ff07-0000-1000-8000-00805f9b34fb"
    )

    val serviceDataOptions = listOf(
        "J7hs2Ak98g", "N2dHkU87ds", "K2sh7Ysg21",
        "Abc123Xy", "Def456Zw", "Ghi789Uv",
        "Jkl012St", "Mno345Qr", "Pqr678Op", "Stu901Mn"
    )

    init {
        val cached = connectionManager.getSettingsRepository().getCachedUserInfoList()
        if (cached.isNotEmpty()) {
            Log.d("UserManagementVM", "📂 Восстановлено ${cached.size} пользователей из кэша")
            _users.value = cached
        }

        // Слушаем входящие данные для обновления списка
        viewModelScope.launch {
            connectionManager.characteristicData.collect { data ->
                val response = String(data.value.toByteArray(), StandardCharsets.UTF_8).trim()
                Log.d("UserManagementVM", "📥 Decoded: ${response.take(100)}...")

                // 🔍 Проверяем, чанк ли это
                if (response.startsWith("USERLIST_PKT:")) {
                    handleUserListChunk(response)
                } else if (response.startsWith("USERLIST:")) {
                    // Старый формат (для совместимости)
                    parseUserList(response)
                }
            }
        }
    }

    // === Загрузка списка пользователей ===
    fun fetchUsers() {
        viewModelScope.launch {
            val isConnected = connectionManager.connectionStateFlow.value == ConnectionState.READY
            if (!isConnected) {
                _showError.value = "Нет соединения с контроллером"
                return@launch
            }

            Log.d("UserManagementVM", "📤 Отправляю LISTUSERS")
            _isLoading.value = true
            _showError.value = null

            @Suppress("MissingPermission")
            connectionManager.sendCommand("LISTUSERS")

            _isLoading.value = false

            if (_users.value.isEmpty()) {
                Log.d("UserManagementVM", "📭 Список пуст после запроса")
            }
        }
    }

    // === Парсинг ответа USERLIST ===
    private fun parseUserList(raw: String) {
        try {
            val data = raw.removePrefix("USERLIST:")
            if (data.isBlank() || data == "|") {
                Log.d("UserManagementVM", "📭 Empty user list received")

                // Сравниваем с кэшем
                val cached = connectionManager.getSettingsRepository().getCachedUserInfoList()
                if (cached.isNotEmpty()) {
                    connectionManager.getSettingsRepository().saveCachedUserInfoList(emptyList())
                }

                _users.value = emptyList()
                return
            }

            val parsed = data.split("|")
                .filter { it.isNotBlank() }  // Убираем пустые записи
                .mapNotNull { entry ->
                    val parts = entry.split(",")

                    // ✅ Проверяем наличие всех 6 полей
                    if (parts.size >= 6) {
                        val id = parts[0].toIntOrNull()
                        val name = parts[1].trim()
                        val mac = parts[2].trim()
                        val location = parts[3].trim()
                        val uuid = parts[4].trim()
                        val serviceData = parts[5].trim()

                        if (id != null && name.isNotBlank() && mac.isNotBlank()) {
                            UserInfo(
                                id = id,
                                name = name,
                                macAddress = mac,
                                location = location,
                                uuid = uuid,
                                serviceData = serviceData
                            )
                        } else null
                    } else {
                        Log.w(
                            "UserManagementVM",
                            "⚠️ Invalid entry (${parts.size} parts): ${entry.take(50)}"
                        )
                        null
                    }
                }
                .distinctBy { it.id }

            val cached = connectionManager.getSettingsRepository().getCachedUserInfoList()
            if (parsed != cached) {
                Log.d(
                    "UserManagementVM",
                    "🔄 Список отличается от кэша, сохраняем (${parsed.size} пользователей)"
                )
                connectionManager.getSettingsRepository().saveCachedUserInfoList(parsed)
            } else {
                Log.d("UserManagementVM", "✅ Список совпадает с кэшем")
            }

            Log.d("UserManagementVM", "✅ Загружено: ${parsed.size} пользователей")
            _users.value = parsed
        } catch (e: Exception) {
            Log.e("UserManagementVM", "❌ Ошибка парсинга", e)
            _showError.value = "Ошибка разбора данных: ${e.message}"
        }
    }

    // === Добавление пользователя ===
    fun addUser(params: NewUserParams, context: Context) {
        val isDuplicate = users.value.any { it.id == params.id }
        if (isDuplicate) {
            setError("Пользователь с таким ID уже существует")
            return
        }

        if (!params.isValid()) {
            _showError.value = "Проверьте корректность введённых данных"
            return
        }

        viewModelScope.launch {
            _isAddingUser.value = true
            _showError.value = null

            if (connectionManager.connectionStateFlow.value != ConnectionState.READY) {
                _showError.value = "Нет соединения с контроллером"
                _isAddingUser.value = false
                return@launch
            }

            val command = params.toEsp32Command()

            @Suppress("MissingPermission")
            val result = connectionManager.sendCommand(command)
            Log.d("UserManagementVM", "📥 $command")

            _isAddingUser.value = false

            when (result) {
                is BleCommandSender.Result.Success -> {
                    // После успешного добавления — обновляем список
                    delay(300)
                    fetchUsers()
                }

                is BleCommandSender.Result.NotConnected -> _showError.value =
                    "Нет подключения к контроллеру"

                is BleCommandSender.Result.CharacteristicNotFound -> _showError.value =
                    "Характеристика не найдена"

                is BleCommandSender.Result.WriteFailed -> _showError.value =
                    "Не удалось записать команду"

                is BleCommandSender.Result.Error -> _showError.value = "Ошибка: ${result.message}"
            }
        }
    }

    // === Удаление пользователя ===
    fun deleteUser(userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("UserManagementVM", "🗑️ Удаляю пользователя ID:$userId")

            @Suppress("MissingPermission")
            connectionManager.sendCommand("DELUSER:$userId")

            delay(300) // Даём время на обработку
            fetchUsers() // Обновляем список
        }
    }

    // === Метод для получения следующего доступного ID ===
    fun getNextAvailableId(): Int {
        val existingIds = users.value.map { it.id }.sorted()

        // Если список пуст — начинаем с 1
        if (existingIds.isEmpty()) return 1

        // Ищем первый пропуск в последовательности
        for (i in existingIds.indices) {

            val expectedId = i + 1
            if (existingIds[i] != expectedId) {
                return expectedId
            }
        }

        return (existingIds.lastOrNull() ?: 0) + 1
    }

    // === Вызови этот метод при открытии формы добавления ===
    fun startAddingUser() {
        _isAddingUser.value = true
        // Можно дополнительно сбросить форму здесь
    }

    // === Утилиты ===
    fun clearError() {
        _showError.value = null
    }

    fun refresh() {
        fetchUsers()
    }

    fun setError(message: String) {
        _showError.value = message
    }

    // === Обработка чанка USERLIST ===
    private fun handleUserListChunk(packet: String) {
        try {
            val headerEnd = packet.indexOf('|')
            if (headerEnd == -1) return

            // Парсим заголовок: "0/1" или "0"
            val header = packet.substring(13, headerEnd)
            val headerParts = header.split('/')
            val chunkIndex = headerParts[0].toInt()
            val totalChunks = if (headerParts.size > 1) {
                headerParts[1].toInt()
            } else {
                -1
            }

            val dataStart = headerEnd + 1
            val hasEnd = packet.endsWith("|END")

            // ✅ Безопасный расчёт dataEnd
            val dataEnd = if (hasEnd) {
                packet.length - 4  // Убираем "|END"
            } else {
                packet.length
            }

            // ✅ Fix: если dataEnd <= dataStart — данные пустые
            val chunkData = if (dataEnd <= dataStart) {
                ""
            } else {
                packet.substring(dataStart, dataEnd)
            }

            Log.d(
                "UserManagementVM",
                "📦 Chunk $chunkIndex/$totalChunks received (${chunkData.length} bytes)"
            )

            // ✅ Проверка: если первый чанок пустой и есть |END — это пустой список
            if (chunkIndex == 0 && chunkData.isEmpty() && hasEnd) {
                Log.d("UserManagementVM", "📭 Empty user list received")
                _users.value = emptyList()
                resetUserListBuffer()
                return
            }

            userListChunks[chunkIndex] = chunkData
            userListLastReceived = System.currentTimeMillis()

            if (totalChunks > 0) {
                if (chunkIndex == 0) {
                    userListReceiving = true
                    userListTotalChunks = totalChunks
                    checkChunkTimeout()
                }
                if (userListChunks.size == userListTotalChunks) {
                    assembleAndParseUserList()
                }
            } else if (hasEnd) {
                assembleAndParseUserList()
            }

        } catch (e: Exception) {
            Log.e("UserManagementVM", "❌ Error parsing chunk", e)
            resetUserListBuffer()
            _showError.value = "Ошибка получения данных"
        }
    }

    // === Сборка и парсинг полного списка ===
    private fun assembleAndParseUserList() {
        Log.d("UserManagementVM", "🔗 Assembling ${userListChunks.size} chunks...")

        val sortedData = userListChunks.toSortedMap().values.joinToString("")
        Log.d("UserManagementVM", "📋 Full assembled data (${sortedData.length} bytes): $sortedData")

        resetUserListBuffer()

        // ✅ Если данные пустые — сразу очищаем список
        if (sortedData.isEmpty()) {
            Log.d("UserManagementVM", "📭 Empty list after assembly")
            _users.value = emptyList()
            return
        }

        parseUserList("USERLIST:$sortedData")
    }

    // === Сброс буфера чанков ===
    private fun resetUserListBuffer() {
        userListChunks.clear()
        userListTotalChunks = 0
        userListReceiving = false
    }

    // === Проверка тайм-аута сборки ===
    private fun checkChunkTimeout() {
        viewModelScope.launch {
            delay(CHUNK_TIMEOUT_MS)
            if (userListReceiving && userListChunks.size < userListTotalChunks) {
                Log.w(
                    "UserManagementVM",
                    "⚠️ Chunk timeout! Received ${userListChunks.size}/${userListTotalChunks}"
                )
                resetUserListBuffer()
                _showError.value = "Ошибка получения списка: таймаут"
            }
        }
    }

    fun getNextAvailableUuid(): String {
        val usedUuids = users.value.map { it.uuid }.toSet()

        // Ищем первый не использованный из списка опций
        return uuidOptions.firstOrNull { it !in usedUuids }
            ?: uuidOptions.first() // Если все заняты — возвращаем первый (можно показать ошибку)
    }

    // ✅ Найти следующий свободный Service Data
    fun getNextAvailableServiceData(): String {
        val usedServiceData = users.value.map { it.serviceData }.toSet()

        return serviceDataOptions.firstOrNull { it !in usedServiceData }
            ?: serviceDataOptions.first()
    }

    // ✅ Проверка: все ли опции исчерпаны?
    fun areUuidOptionsExhausted(): Boolean {
        val usedUuids = users.value.map { it.uuid }.toSet()
        return uuidOptions.all { it in usedUuids }
    }

    fun areServiceDataOptionsExhausted(): Boolean {
        val usedServiceData = users.value.map { it.serviceData }.toSet()
        return serviceDataOptions.all { it in usedServiceData }
    }
}