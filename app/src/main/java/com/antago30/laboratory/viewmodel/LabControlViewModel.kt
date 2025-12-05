package com.antago30.laboratory.viewmodel

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.antago30.laboratory.ble.BleAdvertisingService
import com.antago30.laboratory.model.FunctionItem
import com.antago30.laboratory.model.StaffMember

class LabControlViewModel : ViewModel() {

    val staffList = mutableStateOf(
        listOf(
            StaffMember("1", "ВВ", "Владимир Викторович", true),
            StaffMember("2", "ВО", "Вячеслав Олегович", false),
            StaffMember("3", "ПЕ", "Павел Евгеньевич", true)
        )
    )

    val functions = mutableStateOf(
        listOf(
            FunctionItem("broadcast", "📡 Вещание рекламы", false),
            FunctionItem("cleaning", "🧹 Режим уборки", false),
            FunctionItem("lighting", "💡 Освещение", false)
        )
    )

    private var appContext: Context? = null

    private val _isServiceRunning = mutableStateOf(false)
    val isServiceRunning: Boolean get() = _isServiceRunning.value

    fun setAppContext(context: Context) {
        appContext = context.applicationContext
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
        functions.value = functions.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    fun toggleStaffStatus(id: String) {
        staffList.value = staffList.value.map {
            if (it.id == id) it.copy(isInside = !it.isInside) else it
        }
    }

    fun startBleAdvertising() {
        appContext?.let { ctx ->
            val intent = Intent(ctx, BleAdvertisingService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
            _isServiceRunning.value = true

            if (functions.value.find { it.id == "broadcast" }?.isEnabled == false) {
                toggleFunction("broadcast")
            }
        }
    }

    fun stopBleAdvertising() {
        appContext?.let { ctx ->
            val intent = Intent(ctx, BleAdvertisingService::class.java)
            ctx.stopService(intent)
            _isServiceRunning.value = false

            if (functions.value.find { it.id == "broadcast" }?.isEnabled == true) {
                toggleFunction("broadcast")
            }
        }
    }

    val isAdvertising: Boolean get() = _isServiceRunning.value

    fun onOpenDoorClicked() {

    }
}