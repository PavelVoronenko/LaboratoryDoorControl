package com.antago30.laboratory.viewmodel.addUserViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.antago30.laboratory.ble.BleConnectionManager

class AddUserViewModelFactory(
    private val connectionManager: BleConnectionManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddUserViewModel::class.java)) {
            return AddUserViewModel(connectionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}