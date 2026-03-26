package com.antago30.laboratory.viewmodel.labControlViewModel.useCase

import android.content.Context
import android.content.Intent
import com.antago30.laboratory.ble.BleAdvertisingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdvertisingServiceUseCase {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var appContext: Context? = null

    fun setContext(context: Context) {
        appContext = context.applicationContext
        syncState()
    }

    fun start() {
        appContext?.let { ctx ->
            val intent = Intent(ctx, BleAdvertisingService::class.java)
            ctx.startForegroundService(intent)
            _isRunning.value = true
        }
    }

    fun stop() {
        appContext?.let { ctx ->
            val intent = Intent(ctx, BleAdvertisingService::class.java)
            ctx.stopService(intent)
            _isRunning.value = false
        }
    }

    fun syncState() {
        _isRunning.value = checkIfServiceIsRunning()
    }

    private fun checkIfServiceIsRunning(): Boolean {
        return appContext?.let { ctx ->
            val manager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            manager.getRunningServices(Int.MAX_VALUE).any {
                BleAdvertisingService::class.java.name == it.service.className
            }
        } ?: false
    }

    fun isBroadcastEnabled(functions: () -> List<com.antago30.laboratory.model.FunctionItem>): Boolean {
        return functions().find { it.id == "broadcast" }?.isEnabled == true
    }
}