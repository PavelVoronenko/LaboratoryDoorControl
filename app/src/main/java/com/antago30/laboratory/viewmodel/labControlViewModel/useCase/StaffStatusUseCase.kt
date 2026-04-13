package com.antago30.laboratory.viewmodel.labControlViewModel.useCase

import android.util.Log
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.model.StaffMember
import com.antago30.laboratory.model.UserInfo
import com.antago30.laboratory.util.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StaffStatusUseCase(
    private val connectionManager: BleConnectionManager,
    private val settingsRepo: SettingsRepository,
    initialStaffList: List<StaffMember>
) {
    private val staffLastChangeTime = mutableMapOf<String, Long>()
    private val _staffList = MutableStateFlow(
        settingsRepo.getStaffList(fallback = initialStaffList)
    )
    val staffList: StateFlow<List<StaffMember>> = _staffList.asStateFlow()

    fun toggleStaffStatus(id: String, onCommandSend: (String) -> Unit) {
        val currentTime = System.currentTimeMillis()
        val currentList = _staffList.value
        val updatedList = currentList.map { staff ->
            if (staff.id == id) {
                val newIsInside = !staff.isInside
                val command = buildStaffCommand(staff.id, newIsInside)
                if (connectionManager.connectionStateFlow.value == ConnectionState.READY) {
                    onCommandSend(command)
                }
                staffLastChangeTime[id] = currentTime
                staff.copy(isInside = newIsInside, lastUpdated = currentTime)
            } else staff
        }
        _staffList.value = updatedList
        settingsRepo.saveStaffList(updatedList)
    }

    fun addStaffMember(newMember: StaffMember): Boolean {
        val currentList = _staffList.value
        if (currentList.any { it.id == newMember.id }) return false

        val updatedList = currentList + newMember
        _staffList.value = updatedList
        settingsRepo.saveStaffList(updatedList)
        return true
    }

    fun removeStaffMember(id: String): Boolean {
        val currentList = _staffList.value
        val updatedList = currentList.filter { it.id != id }
        if (updatedList.size == currentList.size) return false

        _staffList.value = updatedList
        settingsRepo.saveStaffList(updatedList)
        return true
    }

    /**
     * Синхронизирует список сотрудников из UserInfo, полученного от контроллера.
     * Вызывается при получении USERLIST.
     */
    fun syncStaffListFromController(userInfoList: List<UserInfo>) {
        val currentList = _staffList.value
        android.util.Log.d("StaffStatusUseCase", "📋 syncStaffListFromController: ${currentList.size} текущих, ${userInfoList.size} от контроллера")
        currentList.forEach { 
            android.util.Log.d("StaffStatusUseCase", "  Текущий: ${it.name} (MAC:${it.macAddress}) isInside=${it.isInside}")
        }

        val updatedList = settingsRepo.syncStaffListFromUserInfo(
            userInfoList = userInfoList,
            currentStaffList = currentList
        )

        android.util.Log.d("StaffStatusUseCase", "✅ После синхронизации:")
        updatedList.forEach {
            android.util.Log.d("StaffStatusUseCase", "  ${it.name} (ID:${it.id}) isInside=${it.isInside}")
        }

        _staffList.value = updatedList
        settingsRepo.saveStaffList(updatedList)
    }

    /**
     * Применяет обновления статуса от контроллера.
     * @param staffIdOrName ID сотрудника или имя (для обратной совместимости)
     * @param isInside true если внутри
     */
    fun applyControllerUpdate(
        staffIdOrName: String,
        isInside: Boolean,
        currentTime: Long = System.currentTimeMillis()
    ) {
        val currentList = _staffList.value

        // Сначала пытаемся найти по ID
        val staffId = if (currentList.any { it.id == staffIdOrName }) {
            staffIdOrName
        } else {
            // Не нашли по ID - пытаемся найти по имени (обратная совместимость)
            val matchedStaff = currentList.find { staff ->
                staff.name.contains(staffIdOrName, ignoreCase = true) ||
                    staffIdOrName.contains(staff.name, ignoreCase = true) ||
                        staff.initials.equals(staffIdOrName, ignoreCase = true)
            }
            matchedStaff?.id
        }

        if (staffId == null) {
            Log.w(
                "StaffStatusUseCase",
                "⚠️ Не удалось сопоставить: $staffIdOrName"
            )
            return
        }

        val updatedList = currentList.map { staff ->
            if (staff.id == staffId) {
                val lastChange = staffLastChangeTime[staff.id] ?: 0
                // Дебаунс: игнорируем обновления от контроллера, если недавно меняли локально
                if (currentTime - lastChange < 1000) {
                    Log.d("BLE_DEBUG", "⏭️ Ignoring controller update for ${staff.name} (local change pending)")
                    staff
                } else {
                    Log.d("BLE_DEBUG", "✅ Updating ${staff.name} (ID:${staff.id}): isInside = $isInside")
                    staff.copy(isInside = isInside, lastUpdated = currentTime)
                }
            } else staff
        }
        _staffList.value = updatedList
        settingsRepo.saveStaffList(updatedList)
    }

    private fun buildStaffCommand(staffId: String, isInside: Boolean): String {
        val state = if (isInside) "1" else "0"
        return "SETUSER:$staffId-$state"
    }
}