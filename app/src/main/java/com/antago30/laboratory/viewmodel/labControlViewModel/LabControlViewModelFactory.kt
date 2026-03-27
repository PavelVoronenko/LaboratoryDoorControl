package com.antago30.laboratory.viewmodel.labControlViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.model.FunctionItem
import com.antago30.laboratory.model.StaffData
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.AdvertisingServiceUseCase
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.BleDataParsingUseCase
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.FunctionControlUseCase
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.StaffStatusUseCase

class LabControlViewModelFactory(
    private val connectionManager: BleConnectionManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LabControlViewModel::class.java)) {
            val staffUseCase = StaffStatusUseCase(
                connectionManager = connectionManager,
                initialStaffList = StaffData.DEFAULT_STAFF_LIST
            )
            val advertisingUseCase = AdvertisingServiceUseCase()

            val functionUseCase = FunctionControlUseCase(
                connectionManager = connectionManager,
                onAdvertisingToggle = { enabled ->
                    if (enabled) advertisingUseCase.start() else advertisingUseCase.stop()
                },
                initialFunctions = listOf(  // ← передаём начальные функции
                    FunctionItem("broadcast", "📡 Вещание рекламы", false, requiresConnection = false),
                    FunctionItem("lighting", "💡 Освещение", false, requiresConnection = true)
                )
            )

            val parsingUseCase = BleDataParsingUseCase(
                staffUseCase = staffUseCase,
                functionUseCase = functionUseCase
            )

            return LabControlViewModel(
                connectionManager = connectionManager,
                staffUseCase = staffUseCase,
                functionUseCase = functionUseCase,
                parsingUseCase = parsingUseCase,
                advertisingUseCase = advertisingUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}