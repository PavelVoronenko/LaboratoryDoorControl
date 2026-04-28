package com.antago30.laboratory.viewmodel.labControlViewModel.useCase

import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.model.FunctionItem
import com.antago30.laboratory.util.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FunctionControlUseCase(
    private val connectionManager: BleConnectionManager,
    private val onAdvertisingToggle: (Boolean) -> Unit,
    initialFunctions: List<FunctionItem>
) {
    private val settingsRepo: SettingsRepository = connectionManager.getSettingsRepository()
    private val _functions = MutableStateFlow(initialFunctions.map { 
        when (it.id) {
            "lighting" -> it.copy(isEnabled = settingsRepo.getLightingState())
            "broadcast" -> it.copy(isEnabled = settingsRepo.isAdvertisingEnabled())
            else -> it
        }
    })
    val functions: StateFlow<List<FunctionItem>> = _functions.asStateFlow()

    private val _isJdeConnected = MutableStateFlow(settingsRepo.getJdeConnectionState())
    val isJdeConnected: StateFlow<Boolean> = _isJdeConnected.asStateFlow()

    fun toggleFunction(
        id: String,
        sendBleCommand: (String) -> Unit
    ) {
        val currentList = _functions.value
        val updatedList = currentList.map { func ->
            if (func.id == id) {
                val newEnabled = !func.isEnabled
                when (id) {
                    "broadcast" -> {
                        onAdvertisingToggle(newEnabled)
                    }
                    "lighting" -> {
                        if (connectionManager.connectionStateFlow.value == ConnectionState.READY) {
                            val command = if (newEnabled) "LIGHTON" else "LIGHTOFF"
                            sendBleCommand(command)
                        }
                    }
                }
                func.copy(isEnabled = newEnabled)
            } else func
        }
        _functions.value = updatedList  // ← Обновляем StateFlow
    }

    fun syncLightingState(isLightOn: Boolean, currentTime: Long, debounceMs: Long = 1000) {
        settingsRepo.saveLightingState(isLightOn)
        val currentList = _functions.value
        val updatedList = currentList.map { func ->
            if (func.id == "lighting" && func.isEnabled != isLightOn) {
                func.copy(isEnabled = isLightOn)
            } else func
        }
        _functions.value = updatedList
        connectionManager.updateWidgets()
    }

    fun getFunctionState(id: String): Boolean =
        _functions.value.find { it.id == id }?.isEnabled ?: false

    fun setFunctionEnabled(id: String, enabled: Boolean) {
        val currentList = _functions.value
        val updatedList = currentList.map { func ->
            if (func.id == id && func.isEnabled != enabled) {
                func.copy(isEnabled = enabled)
            } else func
        }
        _functions.value = updatedList
    }

    fun setJdeConnected(connected: Boolean) {
        settingsRepo.saveJdeConnectionState(connected)
        _isJdeConnected.value = connected
        connectionManager.updateWidgets()
    }
}
