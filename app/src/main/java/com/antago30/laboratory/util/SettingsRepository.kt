package com.antago30.laboratory.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.antago30.laboratory.model.StaffMember
import com.antago30.laboratory.model.UserInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lab_settings", Context.MODE_PRIVATE)

    private val gson = Gson()

    companion object Keys {
        private const val SELECTED_DEVICE_NAME = "selected_device_name"
        private const val SELECTED_DEVICE_ADDRESS = "selected_device_address"
        private const val STAFF_LIST_JSON = "staff_list_json"
        private const val CURRENT_USER_ID = "current_user_id"
        private const val CURRENT_USER_INFO_JSON = "current_user_info_json"
        private const val CACHED_USER_INFO_LIST_JSON = "cached_user_info_list_json"
    }

    private val _currentUserIdFlow = MutableStateFlow(getCurrentUserId())
    val currentUserIdFlow: StateFlow<String?> = _currentUserIdFlow.asStateFlow()

    fun saveSelectedDevice(deviceName: String, deviceAddress: String) {
        prefs.edit {
            putString(SELECTED_DEVICE_NAME, deviceName)
                .putString(SELECTED_DEVICE_ADDRESS, deviceAddress)
        }
    }

    fun getSelectedDevice(): Pair<String, String>? {
        val name = prefs.getString(SELECTED_DEVICE_NAME, null)
        val address = prefs.getString(SELECTED_DEVICE_ADDRESS, null)
        return if (name != null && address != null) name to address else null
    }

    fun getSelectedDeviceAddress(): String? {
        val address = prefs.getString(SELECTED_DEVICE_ADDRESS, null)
        return address
    }

    fun clearSelectedDevice() {
        prefs.edit {
            remove(SELECTED_DEVICE_NAME)
                .remove(SELECTED_DEVICE_ADDRESS)
        }
    }

    fun saveStaffList(staffList: List<StaffMember>) {
        val json = gson.toJson(staffList)
        prefs.edit {
            putString(STAFF_LIST_JSON, json)
        }
    }

    fun getStaffList(fallback: List<StaffMember>): List<StaffMember> {
        val json = prefs.getString(STAFF_LIST_JSON, null)
        return if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<StaffMember>>() {}.type
                val parsedList = gson.fromJson<List<StaffMember>>(json, type)

                // ❗ Пост-обработка: гарантируем валидный adData для каждого пользователя
                parsedList?.map { member ->
                    member.copy(
                        adData = member.adData.takeIf { it.isNotBlank() } ?: "J7hs2Ak98g"
                    )
                } ?: fallback

            } catch (e: Exception) {
                android.util.Log.e("SettingsRepository", "Failed to parse staff list", e)
                fallback
            }
        } else {
            fallback
        }
    }

    fun addStaffMember(newMember: StaffMember, fallback: List<StaffMember>): List<StaffMember> {
        val currentList = getStaffList(fallback)
        // Проверяем, нет ли сотрудника с таким id
        if (currentList.any { it.id == newMember.id }) {
            return currentList
        }
        val updatedList = currentList + newMember
        saveStaffList(updatedList)
        return updatedList
    }

    fun updateStaffMember(
        id: String,
        update: (StaffMember) -> StaffMember,
        fallback: List<StaffMember>
    ): List<StaffMember> {
        val currentList = getStaffList(fallback)
        val updatedList = currentList.map {
            if (it.id == id) update(it) else it
        }
        saveStaffList(updatedList)
        return updatedList
    }

    fun removeStaffMember(id: String, fallback: List<StaffMember>): List<StaffMember> {
        val currentList = getStaffList(fallback)
        val updatedList = currentList.filter { it.id != id }
        saveStaffList(updatedList)
        return updatedList
    }

    fun clearStaffList() {
        prefs.edit {
            remove(STAFF_LIST_JSON)
        }
    }

    // === Синхронизация списка сотрудников из UserInfo (от контроллера) ===

    // Вспомогательная функция для генерации инициалов из полного имени
    // "Павел Евгеньевич" -> "ПЕ", "ВладимирВикторович" -> "ВВ"
    private fun generateInitials(fullName: String): String {
        val trimmed = fullName.trim()
        
        // Пробуем разбить по пробелам
        var parts = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        
        // Если пробелов нет, пытаемся разбить по заглавным буквам (camelCase)
        if (parts.size < 2) {
            parts = buildList {
                val currentWord = StringBuilder()
                for (char in trimmed) {
                    if (char.isUpperCase() && currentWord.isNotEmpty()) {
                        add(currentWord.toString())
                        currentWord.clear()
                    }
                    currentWord.append(char)
                }
                if (currentWord.isNotEmpty()) add(currentWord.toString())
            }
        }
        
        return when {
            parts.size >= 2 -> {
                // Берём первые буквы первых двух слов
                parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
            }
            parts.size == 1 -> {
                // Одно слово - берём первые 2 буквы
                parts[0].take(2).uppercase()
            }
            else -> "??"
        }
    }

    fun syncStaffListFromUserInfo(
        userInfoList: List<UserInfo>,
        currentStaffList: List<StaffMember>
    ): List<StaffMember> {
        if (userInfoList.isEmpty()) {
            return currentStaffList
        }

        val updatedStaffList = userInfoList.map { userInfo ->
            // Проверяем, есть ли уже такой сотрудник в текущем списке по имени (MAC может быть placeholder)
            val existingStaff = currentStaffList.find {
                it.name.equals(userInfo.name, ignoreCase = true)
            }

            if (existingStaff != null) {
                android.util.Log.d(
                    "SettingsRepository",
                    "🔄 Найдено совпадение по имени: ${userInfo.name}, сохраняем isInside=${existingStaff.isInside}"
                )
                // Обновляем ID, UUID, ServiceData, MAC и инициалы; сохраняем статус isInside
                existingStaff.copy(
                    id = userInfo.id.toString(), // Всегда используем актуальный ID от контроллера
                    initials = generateInitials(userInfo.name),
                    serviceUUID = userInfo.uuid,
                    adData = userInfo.serviceData,
                    macAddress = userInfo.macAddress // Также обновляем MAC на актуальный
                )
            } else {
                // Создаём нового сотрудника
                StaffMember(
                    id = userInfo.id.toString(),
                    initials = generateInitials(userInfo.name),
                    name = userInfo.name,
                    isInside = false,
                    serviceUUID = userInfo.uuid,
                    adData = userInfo.serviceData,
                    macAddress = userInfo.macAddress
                )
            }
        }

        // Сохраняем обновлённый список
        saveStaffList(updatedStaffList)

        android.util.Log.d(
            "SettingsRepository",
            "✅ Синхронизировано: ${updatedStaffList.size} сотрудников из ${userInfoList.size} UserInfo"
        )

        return updatedStaffList
    }

    fun saveCurrentUserId(userId: String) {
        prefs.edit { putString(CURRENT_USER_ID, userId) }
        _currentUserIdFlow.value = userId
    }

    fun getCurrentUserId(): String? =
        prefs.getString(CURRENT_USER_ID, null)

    fun getCurrentUser(fallbackStaffList: List<StaffMember>): StaffMember? {
        val currentUserId = getCurrentUserId()
        return fallbackStaffList.find { it.id == currentUserId }
    }

    fun clearCurrentUserId() {
        prefs.edit {
            remove(CURRENT_USER_ID)
        }
    }

    // === Методы для сохранения выбранного UserInfo ===

    fun saveCurrentUserInfo(userInfo: UserInfo) {
        val json = gson.toJson(userInfo)
        prefs.edit {
            putString(CURRENT_USER_INFO_JSON, json)
        }
        _currentUserIdFlow.value = userInfo.id.toString()
    }

    fun getCurrentUserInfo(): UserInfo? {
        val json = prefs.getString(CURRENT_USER_INFO_JSON, null)
        return if (!json.isNullOrBlank()) {
            try {
                gson.fromJson(json, UserInfo::class.java)
            } catch (e: Exception) {
                android.util.Log.e("SettingsRepository", "Failed to parse current UserInfo", e)
                null
            }
        } else {
            null
        }
    }

    fun clearCurrentUserInfo() {
        prefs.edit {
            remove(CURRENT_USER_INFO_JSON)
        }
        _currentUserIdFlow.value = null
    }

    // Методы для кэширования UserInfo от контроллера
    fun saveCachedUserInfoList(list: List<UserInfo>) {
        val json = gson.toJson(list)
        prefs.edit {
            putString(CACHED_USER_INFO_LIST_JSON, json)
        }
    }

    fun getCachedUserInfoList(): List<UserInfo> {
        val json = prefs.getString(CACHED_USER_INFO_LIST_JSON, null)
        return if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<UserInfo>>() {}.type
                gson.fromJson<List<UserInfo>>(json, type) ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("SettingsRepository", "Failed to parse cached UserInfo list", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}