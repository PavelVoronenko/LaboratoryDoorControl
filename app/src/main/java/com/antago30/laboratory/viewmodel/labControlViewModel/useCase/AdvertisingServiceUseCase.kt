package com.antago30.laboratory.viewmodel.labControlViewModel.useCase

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
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

    private var isStarting = false

    private var appContext: Context? = null

    fun setContext(context: Context) {
        appContext = context.applicationContext
        syncState()
    }

    fun start() {
        if (isStarting) return

        val ctx = appContext ?: return

        // Проверяем разрешение на уведомления (Android 13+)
        val hasNotificationPermission =
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        if (!hasNotificationPermission) {
            isStarting = false
            return
        }

        if (checkIfServiceIsRunning(ctx)) {
            _isRunning.value = true
            isStarting = false
            return
        }

        val currentUserInfo = settingsRepo.getCurrentUserInfo()
        
        // Проверяем, выбран ли текущий пользователь
        if (currentUserInfo == null) {
            isStarting = false
            return
        }

        val serviceUuidStr = currentUserInfo.uuid.takeIf { it.isNotBlank() }
            ?: "0000ff12-0000-1000-8000-00805f9b34fb"
        val adData = currentUserInfo.serviceData.takeIf { it.isNotBlank() }
            ?: "J7hs2Ak98g"

        isStarting = true
        try {
            val serviceUuid = UUID.fromString(serviceUuidStr)
            startWithParams(serviceUuid, adData)
        } catch (e: IllegalArgumentException) {
            isStarting = false
        }
    }

    fun stop() {
        val ctx = appContext ?: return
        val intent = Intent(ctx, BleAdvertisingService::class.java)
        val stopped = ctx.stopService(intent)

        _isRunning.value = false
        isStarting = false
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
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
            kotlinx.coroutines.delay(200)
            start()
        }
    }

    fun onUserChanged() {
        restartWithCurrentUser()
        isStarting = false
    }

    fun syncState() {
        val ctx = appContext ?: return
        _isRunning.value = checkIfServiceIsRunning(ctx)
    }

    private fun checkIfServiceIsRunning(context: Context): Boolean {
        return try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            manager.getRunningServices(Int.MAX_VALUE).any { service ->
                val className = service.service.className
                className == BleAdvertisingService::class.java.name ||
                        className.endsWith(".BleAdvertisingService")
            }
        } catch (e: Exception) {
            false
        }
    }
}