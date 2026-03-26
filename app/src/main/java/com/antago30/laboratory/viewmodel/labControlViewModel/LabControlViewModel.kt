package com.antago30.laboratory.viewmodel.labControlViewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.model.ConnectionState
import com.antago30.laboratory.model.FunctionItem
import com.antago30.laboratory.model.StaffMember
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.AdvertisingServiceUseCase
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.BleDataParsingUseCase
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.FunctionControlUseCase
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.StaffStatusUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LabControlViewModel(
    private val connectionManager: BleConnectionManager,
    private val staffUseCase: StaffStatusUseCase,
    private val functionUseCase: FunctionControlUseCase,
    private val parsingUseCase: BleDataParsingUseCase,
    private val advertisingUseCase: AdvertisingServiceUseCase
) : ViewModel() {

    // === UI State ===
    val isInterfaceEnabled: StateFlow<Boolean> = connectionManager.connectionStateFlow
        .map { it == ConnectionState.READY }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val staffList: StateFlow<List<StaffMember>> = staffUseCase.staffList
    val functions: StateFlow<List<FunctionItem>> = functionUseCase.functions

    // Данные от контроллера
    val systemMessageData = parsingUseCase.systemMessageData
    val terminalData = parsingUseCase.terminalData
    val isAdvertising: StateFlow<Boolean> = advertisingUseCase.isRunning

    init {
        // Подписка на состояние соединения
        @Suppress("MissingPermission")
        viewModelScope.launch {
            connectionManager.connectionStateFlow.collect { state ->
                if (state == ConnectionState.READY) {
                    connectionManager.requestMtu(200)
                    connectionManager.subscribeToSensorData()
                }
            }
        }

        viewModelScope.launch {
            connectionManager.characteristicData.collect { data ->
                parsingUseCase.processData(data)
            }
        }
    }

    fun setAppContext(context: Context) {
        advertisingUseCase.setContext(context)
    }

    // === Actions ===
    @Suppress("MissingPermission")
    fun toggleStaffStatus(id: String) {
        staffUseCase.toggleStaffStatus(id) { command ->
            connectionManager.sendCommand(command)
        }
    }

    @Suppress("MissingPermission")
    fun toggleFunction(id: String) {
        functionUseCase.toggleFunction(id) { command ->
            connectionManager.sendCommand(command)
        }
    }

    @Suppress("MissingPermission")
    fun onOpenDoorClicked() {
        if (connectionManager.connectionStateFlow.value != ConnectionState.READY) return
        connectionManager.sendCommand("OPENDOOR")
    }

    fun startBleAdvertising() {
        advertisingUseCase.start()
    }

    fun stopBleAdvertising() {
        advertisingUseCase.stop()
    }

    fun syncServiceState() {
        advertisingUseCase.syncState()
    }
}