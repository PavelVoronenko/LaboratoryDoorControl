package com.antago30.laboratory.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.antago30.laboratory.model.StaffMember
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lab_settings", Context.MODE_PRIVATE)

    private val gson = Gson()

    companion object Keys {
        private const val SELECTED_DEVICE_NAME = "selected_device_name"
        private const val SELECTED_DEVICE_ADDRESS = "selected_device_address"
        private const val STAFF_LIST_JSON = "staff_list_json"
        private const val CURRENT_USER_ID = "current_user_id"
    }

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
                gson.fromJson(json, type)
            } catch (e: Exception) {
                // При ошибке парсинга возвращаем fallback
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

    fun updateStaffMember(id: String, update: (StaffMember) -> StaffMember, fallback: List<StaffMember>): List<StaffMember> {
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

    fun saveCurrentUserId(userId: String) {
        prefs.edit {
            putString(CURRENT_USER_ID, userId)
        }
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
}