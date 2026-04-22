package com.antago30.laboratory.viewmodel.labControlViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.model.FunctionItem
import com.antago30.laboratory.util.SettingsRepository
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.AdvertisingServiceUseCase
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.BleDataParsingUseCase
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.FunctionControlUseCase
import com.antago30.laboratory.viewmodel.labControlViewModel.useCase.StaffStatusUseCase

class LabControlViewModelFactory(
    private val connectionManager: BleConnectionManager,
    private val settingsRepo: SettingsRepository
) : ViewModelProvider.Factory {

    lateinit var functionUseCase: FunctionControlUseCase
        private set

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LabControlViewModel::class.java)) {
            val initialStaffList = settingsRepo.getStaffList(
                fallback = emptyList()
            )

            val staffUseCase = StaffStatusUseCase(
                connectionManager = connectionManager,
                settingsRepo = settingsRepo,
                initialStaffList = initialStaffList
            )
            val advertisingUseCase = AdvertisingServiceUseCase(settingsRepo)

            val fUseCase = FunctionControlUseCase(
                connectionManager = connectionManager,
                onAdvertisingToggle = { enabled ->
                    if (enabled) advertisingUseCase.start() else advertisingUseCase.stop()
                },
                initialFunctions = listOf(
                    FunctionItem("broadcast", "📡 Вещание рекламы", false, requiresConnection = false),
                    FunctionItem("lighting", "💡 Освещение", false, requiresConnection = true)
                )
            )
            this.functionUseCase = fUseCase

            val parsingUseCase = BleDataParsingUseCase(
                staffUseCase = staffUseCase,
                functionUseCase = fUseCase
            )

            return LabControlViewModel(
                connectionManager = connectionManager,
                staffUseCase = staffUseCase,
                functionUseCase = fUseCase,
                parsingUseCase = parsingUseCase,
                advertisingUseCase = advertisingUseCase,
                settingsRepo = settingsRepo
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
