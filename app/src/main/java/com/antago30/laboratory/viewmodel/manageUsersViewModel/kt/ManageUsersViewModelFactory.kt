package com.antago30.laboratory.viewmodel.manageUsersViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.antago30.laboratory.ble.BleConnectionManager

class ManageUsersViewModelFactory(
    private val connectionManager: BleConnectionManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageUsersViewModel::class.java)) {
            return ManageUsersViewModel(connectionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}