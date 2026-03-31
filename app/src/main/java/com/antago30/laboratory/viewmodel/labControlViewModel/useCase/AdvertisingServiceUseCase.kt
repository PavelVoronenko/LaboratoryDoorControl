package com.antago30.laboratory.viewmodel.labControlViewModel.useCase

import android.content.Context
import android.content.Intent
import com.antago30.laboratory.ble.BleAdvertisingService
import com.antago30.laboratory.util.SettingsRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AdvertisingServiceUseCase(
    private val settingsRepo: SettingsRepository
) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private data class LastParams(val uuid: String, val adData: String)

    private var lastStartedParams: LastParams? = null
    private var isStarting = false

    private var appContext: Context? = null

    fun setContext(context: Context) {
        appContext = context.applicationContext
        syncState()
    }

    fun start() {
        if (isStarting) {
            return
        }

        val currentUser = settingsRepo.getCurrentUser(
            settingsRepo.getStaffList(emptyList())
        )

        val serviceUuidStr = currentUser?.serviceUUID?.takeIf { it.isNotBlank() }
            ?: "0000ff12-0000-1000-8000-00805f9b34fb"
        val adData = currentUser?.adData?.takeIf { it.isNotBlank() }
            ?: "J7hs2Ak98g"

        val newParams = LastParams(serviceUuidStr, adData)

        if (_isRunning.value && lastStartedParams == newParams) {
            return
        }

        isStarting = true


        try {
            val serviceUuid = UUID.fromString(serviceUuidStr)
            startWithParams(serviceUuid, adData)
        } catch (e: IllegalArgumentException) {
        }
    }

    private fun startWithParams(serviceUuid: UUID, adData: String) {
        val ctx = appContext ?: return
        val intent = Intent(ctx, BleAdvertisingService::class.java).apply {
            putExtra(BleAdvertisingService.EXTRA_SERVICE_UUID, serviceUuid.toString())
            putExtra(BleAdvertisingService.EXTRA_AD_DATA, adData)
        }
        ctx.startForegroundService(intent)
        _isRunning.value = true
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun restartWithCurrentUser() {
        if (!_isRunning.value) return
        stop()
        // Небольшая задержка для гарантированной остановки
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
            kotlinx.coroutines.delay(200)
            start()
        }
    }

    fun stop() {
        appContext?.let {
            appContext?.let { ctx ->
                ctx.stopService(Intent(ctx, BleAdvertisingService::class.java))
                _isRunning.value = false
            }
        }
        isStarting = false
    }

    fun onUserChanged() {
        restartWithCurrentUser()
        isStarting = false
    }

    fun syncState() {
        _isRunning.value = checkIfServiceIsRunning()
    }

    private fun checkIfServiceIsRunning(): Boolean {
        return appContext?.let { ctx ->
            val manager =
                ctx.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            manager.getRunningServices(Int.MAX_VALUE).any {
                BleAdvertisingService::class.java.name == it.service.className
            }
        } ?: false
    }
}