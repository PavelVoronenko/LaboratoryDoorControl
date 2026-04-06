package com.antago30.laboratory.viewmodel.manageUsersViewModel.kt

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.model.CharacteristicData
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.model.UserInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class ManageUsersViewModel(
    private val connectionManager: BleConnectionManager
) : ViewModel() {

    private val _users = MutableStateFlow<List<UserInfo>>(emptyList())
    val users: StateFlow<List<UserInfo>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            connectionManager.characteristicData.collect { data: CharacteristicData ->
                val response = String(data.value.toByteArray(), StandardCharsets.UTF_8).trim()

                Log.d("ManageUsersVM", "📥 Decoded: $response")

                if (response.startsWith("USERLIST:")) {
                    parseUserList(response)
                }
            }
        }
    }

    fun fetchUsers() {
        viewModelScope.launch {
            val isConnected = connectionManager.connectionStateFlow.value == ConnectionState.READY
            if (!isConnected) {
                Log.w("ManageUsersVM", "⚠️ Нет соединения с контроллером")
                _error.value = "Нет соединения с контроллером"
                return@launch
            }

            Log.d("ManageUsersVM", "📤 Отправляю LISTUSERS")
            _isLoading.value = true
            _error.value = null

            @Suppress("MissingPermission")
            connectionManager.sendCommand("LISTUSERS")

            // Даем время на ответ (ESP32 обрабатывает команду ~100-300мс)
            delay(1000)
            _isLoading.value = false

            // Если список остался пустым после ответа
            if (_users.value.isEmpty()) {
                Log.d("ManageUsersVM", "📭 Список пуст после запроса")
                _error.value = null // Не ошибка, просто нет пользователей
            }
        }
    }

    private fun parseUserList(raw: String) {
        try {
            Log.d("ManageUsersVM", "🔍 Парсинг: $raw")
            val data = raw.removePrefix("USERLIST:")
            if (data.isBlank()) {
                _users.value = emptyList()
                return
            }

            val parsed = data.split("|")
                .mapNotNull { entry ->
                    // 🔥 Очищаем запись от мусора перед парсингом
                    val cleanEntry = entry.replace(Regex("""\s*,"%\w+",%\w+\s*"""), "")

                    val parts = cleanEntry.split(",")

                    // 🔥 Требуем минимум 4 поля: id, name, mac, location
                    if (parts.size >= 4) {
                        val id = parts[0].toIntOrNull()
                        val name = parts[1].trim()
                        val mac = parts[2].trim()
                        val location = parts[3].trim()

                        // 🔥 Пропускаем записи с невалидным ID
                        if (id != null && name.isNotBlank() && mac.isNotBlank()) {
                            UserInfo(
                                id = id,
                                name = name,
                                macAddress = mac,
                                location = location
                            )
                        } else {
                            Log.w("ManageUsersVM", "⚠️ Пропущена запись: id=$id, name=$name")
                            null
                        }
                    } else {
                        Log.w("ManageUsersVM", "⚠️ Невалидная запись (частей: ${parts.size}): $cleanEntry")
                        null
                    }
                }
                // 🔥 Гарантируем уникальность ключей: если дубли — добавляем индекс
                .distinctBy { it.id }

            Log.d("ManageUsersVM", "✅ Загружено: ${parsed.size} пользователей")
            _users.value = parsed
        } catch (e: Exception) {
            Log.e("ManageUsersVM", "❌ Ошибка парсинга", e)
            _error.value = "Ошибка разбора данных"
        }
    }

    fun toggleSelection(id: Int) {
        _users.value = _users.value.map {
            if (it.id == id) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun deleteSelected() {
        val selected = _users.value.filter { it.isSelected }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            selected.forEach { user ->
                Log.d("ManageUsersVM", "🗑️ Удаляю: ${user.name} (ID:${user.id})")
                @Suppress("MissingPermission")
                connectionManager.sendCommand("DELUSER:${user.id}")
                delay(400) // Пауза для стабильной записи в NVS
            }
            delay(600)
            fetchUsers() // Обновляем список после удаления
        }
    }

    fun clearError() { _error.value = null }
}