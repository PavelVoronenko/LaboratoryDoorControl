package com.antago30.laboratory.viewmodel.manageUsersViewModel.kt

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

class UserManagementViewModel(
    private val connectionManager: BleConnectionManager
) : ViewModel() {

    private val _users = MutableStateFlow(
        connectionManager.getSettingsRepository().getCachedUserInfoList()
    )
    val users: StateFlow<List<UserInfo>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showError = MutableStateFlow<String?>(null)
    val showError: StateFlow<String?> = _showError.asStateFlow()

    private val _isAddingUser = MutableStateFlow(false)
    val isAddingUser: StateFlow<Boolean> = _isAddingUser.asStateFlow()

    private val _currentUserId = MutableStateFlow(
        connectionManager.getSettingsRepository().getCurrentUserId()
    )
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    fun selectCurrentUser(userInfo: UserInfo) {
        _currentUserId.value = userInfo.id.toString()
        connectionManager.getSettingsRepository().saveCurrentUserId(userInfo.id.toString())
        connectionManager.getSettingsRepository().saveCurrentUserInfo(userInfo)
        Log.d("UserManagementVM", "✅ Выбран пользователь: ${userInfo.name} (ID: ${userInfo.id})")
    }

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
        // Подписываемся на централизованный поток пользователей
        viewModelScope.launch {
            connectionManager.userListFlow.collect { updatedUsers ->
                _users.value = updatedUsers
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
        }
    }

    // === Добавление пользователя ===
    fun addUser(params: NewUserParams) {
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
            _isLoading.value = false
        }
    }

    // === Метод для получения следующего доступного ID ===
    fun getNextAvailableId(): Int {
        val existingIds = users.value.map { it.id }.sorted()
        if (existingIds.isEmpty()) return 1
        for (i in existingIds.indices) {
            val expectedId = i + 1
            if (existingIds[i] != expectedId) return expectedId
        }
        return (existingIds.lastOrNull() ?: 0) + 1
    }

    // === Утилиты ===
    fun clearError() {
        _showError.value = null
    }

    fun setError(message: String) {
        _showError.value = message
    }

    // === Сброс буфера чанков ===
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