package com.antago30.laboratory.viewmodel

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antago30.laboratory.ble.BleAdvertisingService
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.ble.ConnectionState
import com.antago30.laboratory.model.FunctionItem
import com.antago30.laboratory.model.StaffMember
import com.antago30.laboratory.util.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LabControlViewModel(
    private val settingsRepo: SettingsRepository,
    private val connectionManager: BleConnectionManager,
) : ViewModel() {

    private val _uiEvents = MutableSharedFlow<UiEvent>()

    val isInterfaceEnabled: StateFlow<Boolean> =
        connectionManager.connectionState
            .map { state ->
                val enabled = state == ConnectionState.CONNECTED
                android.util.Log.d("BLE_DEBUG", "ViewModel: connectionState=$state → isInterfaceEnabled=$enabled")
                enabled
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        attemptAutoConnect()

        viewModelScope.launch {
            connectionManager.connectionState.collect { state ->
                android.util.Log.d("BLE_DEBUG", "🔁 ViewModel COLLECT: $state")
            }
        }
    }

    private fun attemptAutoConnect() {
        val savedAddress = settingsRepo.getSelectedDeviceAddress() ?: return
        val device = BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(savedAddress) ?: return

        when (val result = connectionManager.connect(device, autoConnect = false)) {
            is BleConnectionManager.ConnectResult.PermissionDenied -> {
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.RequestBlePermissions)
                }
            }
            is BleConnectionManager.ConnectResult.Error -> {
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.ShowError(result.message))
                }
            }
            is BleConnectionManager.ConnectResult.Connecting -> {
                // Всё ок, ждём ConnectionState.CONNECTED
            }
        }
    }

    // События для UI
    sealed class UiEvent {
        object RequestBlePermissions : UiEvent()
        data class ShowError(val message: String) : UiEvent()
    }

    val staffList = mutableStateOf(
        listOf(
            StaffMember("1", "ВВ", "Владимир Викторович", false),
            StaffMember("2", "ВО", "Вячеслав Олегович", true),
            StaffMember("3", "ПЕ", "Павел Евгеньевич", true),
        )
    )

    val functions = mutableStateOf(
        listOf(
            FunctionItem("broadcast", "📡 Вещание рекламы", false),
            FunctionItem("lighting", "💡 Освещение", false)
        )
    )

    private var appContext: Context? = null
    private val _isServiceRunning = mutableStateOf(false)

    fun setAppContext(context: Context) {
        appContext = context.applicationContext
        syncServiceState()
    }

    private fun checkIfServiceIsRunning(): Boolean {
        return appContext?.let { ctx ->
            val manager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            manager.getRunningServices(Int.MAX_VALUE).any {
                BleAdvertisingService::class.java.name == it.service.className
            }
        } ?: false
    }

    fun syncServiceState() {
        _isServiceRunning.value = checkIfServiceIsRunning()
        val broadcastEnabled = functions.value.find { it.id == "broadcast" }?.isEnabled == true
        if (_isServiceRunning.value && !broadcastEnabled) {
            toggleFunction("broadcast")
        } else if (!_isServiceRunning.value && broadcastEnabled) {
            toggleFunction("broadcast")
        }
    }

    fun toggleFunction(id: String) {
        functions.value = functions.value.map { func ->
            if (func.id == id) {
                val newEnabled = !func.isEnabled
                if (id == "broadcast") {
                    if (newEnabled) startBleAdvertising()
                    else stopBleAdvertising()
                }
                func.copy(isEnabled = newEnabled)
            } else func
        }
    }

    fun toggleStaffStatus(id: String) {
        staffList.value = staffList.value.map { staff ->
            if (staff.id == id) staff.copy(isInside = !staff.isInside) else staff
        }
    }

    fun startBleAdvertising() {
        appContext?.let { ctx ->
            val intent = Intent(ctx, BleAdvertisingService::class.java)
            ctx.startForegroundService(intent)
            _isServiceRunning.value = true
        }
    }

    fun stopBleAdvertising() {
        appContext?.let { ctx ->
            val intent = Intent(ctx, BleAdvertisingService::class.java)
            ctx.stopService(intent)
            _isServiceRunning.value = false
        }
    }

    val isAdvertising: Boolean get() = _isServiceRunning.value

    fun onOpenDoorClicked() {
        if (connectionManager.connectionState.value != ConnectionState.CONNECTED) return
        connectionManager.sendDoorCommand()
    }
}