package com.antago30.laboratory.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lab_settings", Context.MODE_PRIVATE)

    companion object Keys {
        private const val SELECTED_DEVICE_NAME = "selected_device_name"
        private const val SELECTED_DEVICE_ADDRESS = "selected_device_address"
    }

    fun saveSelectedDevice(deviceName: String, deviceAddress: String) {
        prefs.edit {
            putString(SELECTED_DEVICE_NAME, deviceName)
                .putString(SELECTED_DEVICE_ADDRESS, deviceAddress)
        }
    }

    fun getSelectedDevice(): Pair<String, String>? {
        val name = prefs.getString(SELECTED_DEVICE_NAME, null)
        val address = prefs.getString(SELECTED_DEVICE_ADDRESS, null)
        return if (name != null && address != null) name to address else null
    }

    fun clearSelectedDevice() {
        prefs.edit {
            remove(SELECTED_DEVICE_NAME)
                .remove(SELECTED_DEVICE_ADDRESS)
        }
    }
}