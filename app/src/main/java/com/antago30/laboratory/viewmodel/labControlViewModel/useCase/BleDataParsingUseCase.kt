package com.antago30.laboratory.viewmodel.labControlViewModel.useCase

import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.model.CharacteristicData
import kotlinx.coroutines.flow.MutableStateFlow

class BleDataParsingUseCase(
    private val staffUseCase: StaffStatusUseCase,
    private val functionUseCase: FunctionControlUseCase
) {
    private val _systemMessageData = MutableStateFlow("—")
    private val _terminalData = MutableStateFlow("—")

    fun processData(data: CharacteristicData) {

        when (data.uuid) {
            BleConnectionManager.SYSTEM_MESSAGE_CHARACTERISTIC.toString() -> {
                val message = String(data.value.toByteArray(), Charsets.UTF_8).trim()
                _systemMessageData.value = message
                parseSystemMessage(message)
            }
            BleConnectionManager.TERMINAL_CHARACTERISTIC.toString() -> {
                val message = String(data.value.toByteArray(), Charsets.UTF_8).trim()
                _terminalData.value = message
            }
            else -> {
            }
        }
    }

    private fun parseSystemMessage(message: String) {
        val currentTime = System.currentTimeMillis()
        val parts = message.split("|").filter { it.isNotBlank() }

        for (part in parts) {
            when {
                part.startsWith("LIGHTSTATUS:", ignoreCase = true) -> {
                    val lightStatus = part.substringAfter("LIGHTSTATUS:", "").trim()
                    val isLightOn = lightStatus == "1"
                    functionUseCase.syncLightingState(isLightOn, currentTime)
                }
                part.contains("-inside", ignoreCase = true) || part.contains("-outside", ignoreCase = true) -> {
                    val lastIndex = part.lastIndexOf('-')
                    if (lastIndex > 0) {
                        val name = part.substring(0, lastIndex).trim().uppercase()
                        val status = part.substring(lastIndex + 1).trim().lowercase()
                        val isInside = status == "inside"
                        staffUseCase.applyControllerUpdate(name, isInside, currentTime)
                    }
                }
            }
        }
    }
}