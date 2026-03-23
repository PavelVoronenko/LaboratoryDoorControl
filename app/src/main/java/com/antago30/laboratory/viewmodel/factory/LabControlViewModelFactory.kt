package com.antago30.laboratory.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.util.SettingsRepository
import com.antago30.laboratory.viewmodel.LabControlViewModel

class LabControlViewModelFactory(
    private val settingsRepo: SettingsRepository,
    private val connectionManager: BleConnectionManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LabControlViewModel::class.java)) {
            return LabControlViewModel(
                settingsRepo = settingsRepo,
                connectionManager = connectionManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}