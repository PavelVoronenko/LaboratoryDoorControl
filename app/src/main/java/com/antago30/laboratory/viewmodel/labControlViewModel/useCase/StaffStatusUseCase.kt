package com.antago30.laboratory.viewmodel.labControlViewModel.useCase

import android.util.Log
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.model.StaffMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StaffStatusUseCase(
    private val connectionManager: BleConnectionManager,
    initialStaffList: List<StaffMember>
) {
    private val staffLastChangeTime = mutableMapOf<String, Long>()
    private val _staffList = MutableStateFlow(initialStaffList)
    val staffList: StateFlow<List<StaffMember>> = _staffList.asStateFlow()

    // Карта сопоставления: имя от контроллера → id сотрудника
    private val controllerNameToStaffId = mapOf(
        "ВОЛОДЯ" to "volodia",
        "СЛАВА" to "slava",
        "ПАША" to "pasha",
        // Добавляй новые пары здесь
    )

    fun toggleStaffStatus(id: String, onCommandSend: (String) -> Unit) {
        val currentTime = System.currentTimeMillis()
        val currentList = _staffList.value
        val updatedList = currentList.map { staff ->
            if (staff.id == id) {
                val newIsInside = !staff.isInside
                val command = buildStaffCommand(staff.initials, newIsInside)
                if (connectionManager.connectionStateFlow.value == ConnectionState.READY) {
                    onCommandSend(command)
                }
                staffLastChangeTime[id] = currentTime
                staff.copy(isInside = newIsInside, lastUpdated = currentTime)
            } else staff
        }
        _staffList.value = updatedList
    }

    /**
     * Применяет обновления от контроллера.
     */
    fun applyControllerUpdate(
        controllerName: String,
        isInside: Boolean,
        currentTime: Long = System.currentTimeMillis()
    ) {

        // Ищем сотрудника по маппингу
        val staffId = controllerNameToStaffId[controllerName.uppercase()] ?: return

        val currentList = _staffList.value
        val updatedList = currentList.map { staff ->
            if (staff.id == staffId) {
                val lastChange = staffLastChangeTime[staff.id] ?: 0
                // Дебаунс: игнорируем обновления от контроллера, если недавно меняли локально
                if (currentTime - lastChange < 1000) {
                    Log.d("BLE_DEBUG", "⏭️ Ignoring controller update for ${staff.name} (local change pending)")
                    staff
                } else {
                    Log.d("BLE_DEBUG", "✅ Updating ${staff.name}: isInside = $isInside")
                    staff.copy(isInside = isInside, lastUpdated = currentTime)
                }
            } else staff
        }
        _staffList.value = updatedList
    }

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
}