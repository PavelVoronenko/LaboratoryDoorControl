package com.antago30.laboratory.viewmodel.manageUsersViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.antago30.laboratory.ble.BleConnectionManager

class UserManagementViewModelFactory(
    private val connectionManager: BleConnectionManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserManagementViewModel::class.java)) {
            return UserManagementViewModel(connectionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}