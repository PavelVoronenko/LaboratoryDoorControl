package com.antago30.laboratory.viewmodel.addUserViewModel.kt

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.ble.bleConnectionManager.BleCommandSender
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.model.NewUserParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddUserViewModel(
    private val connectionManager: BleConnectionManager
) : ViewModel() {

    // === UI State ===
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showError = MutableStateFlow<String?>(null)
    val showError: StateFlow<String?> = _showError.asStateFlow()

    private val _userAdded = MutableStateFlow(false)
    val userAdded: StateFlow<Boolean> = _userAdded.asStateFlow()

    // === Предустановленные значения для выпадающих меню ===
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

    // === Действия ===
    fun addUser(params: NewUserParams, context: Context) {
        if (!params.isValid()) {
            _showError.value = "Проверьте корректность введённых данных"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _showError.value = null

            // Проверяем подключение
            if (connectionManager.connectionStateFlow.value != ConnectionState.READY) {
                _showError.value = "Нет соединения с контроллером"
                _isLoading.value = false
                return@launch
            }

            // Отправляем команду
            val command = params.toEsp32Command()
            @Suppress("MissingPermission")
            val result = connectionManager.sendCommand(command)

            _isLoading.value = false

            // 🔥 Обрабатываем sealed class Result через when
            when (result) {
                is BleCommandSender.Result.Success -> {
                    _userAdded.value = true
                    // Сбрасываем флаг после прочтения
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(500)
                        _userAdded.value = false
                    }
                }
                is BleCommandSender.Result.NotConnected -> {
                    _showError.value = "Нет подключения к контроллеру"
                }
                is BleCommandSender.Result.CharacteristicNotFound -> {
                    _showError.value = "Характеристика не найдена на устройстве"
                }
                is BleCommandSender.Result.WriteFailed -> {
                    _showError.value = "Не удалось записать команду"
                }
                is BleCommandSender.Result.Error -> {
                    _showError.value = "Ошибка: ${result.message}"
                }
            }
        }
    }

    fun clearError() {
        _showError.value = null
    }

    fun resetAddedFlag() {
        _userAdded.value = false
    }
}