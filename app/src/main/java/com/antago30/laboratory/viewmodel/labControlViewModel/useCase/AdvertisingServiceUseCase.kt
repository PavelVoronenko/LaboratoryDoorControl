package com.antago30.laboratory.viewmodel.labControlViewModel.useCase

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.antago30.laboratory.ble.BleAdvertisingService
import com.antago30.laboratory.util.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var appContext: Context? = null

    // Ресивер для получения статуса от сервиса
    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BleAdvertisingService.ACTION_STATE_CHANGED) {
                val isRunning = intent.getBooleanExtra(BleAdvertisingService.EXTRA_IS_RUNNING, false)
                _isRunning.value = isRunning
                onServiceStateChanged?.invoke(isRunning)
            }
        }
    }

    // Callback для уведомления о фактическом состоянии сервиса
    var onServiceStateChanged: ((Boolean) -> Unit)? = null

    fun setContext(context: Context) {
        val ctx = context.applicationContext
        if (appContext == null) {
            appContext = ctx
            // Регистрируем ресивер только один раз
            val filter = IntentFilter(BleAdvertisingService.ACTION_STATE_CHANGED)
            ctx.registerReceiver(serviceStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        }
        syncState()
    }

    fun start() {
        val ctx = appContext ?: run {
            Log.w("AdvertisingUseCase", "Context is null")
            return
        }

        // Добавляем небольшую задержку, чтобы Activity успела вернуться в фокус
        // после закрытия диалога разрешений. Это критично для Android 14.
        scope.launch {
            if (!hasNotificationPermission(ctx)) {
                delay(500)
            } else {
                delay(100)
            }
            
            if (hasNotificationPermission(ctx)) {
                launchAdvertising(ctx)
            } else {
                Log.w("AdvertisingUseCase", "Still no notification permission")
            }
        }
    }

    private fun hasNotificationPermission(ctx: Context): Boolean {
        return ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun launchAdvertising(ctx: Context) {
        val currentUserInfo = settingsRepo.getCurrentUserInfo()
        if (currentUserInfo == null) {
            Log.w("AdvertisingUseCase", "No current user selected")
            return
        }

        val serviceUuidStr = currentUserInfo.uuid.takeIf { it.isNotBlank() }
            ?: "0000ff12-0000-1000-8000-00805f9b34fb"
        val adData = currentUserInfo.serviceData.takeIf { it.isNotBlank() }
            ?: "J7hs2Ak98g"

        Log.d("AdvertisingUseCase", "Starting advertising: UUID=$serviceUuidStr, adData=$adData")

        try {
            val serviceUuid = UUID.fromString(serviceUuidStr)
            startWithParams(serviceUuid, adData)
        } catch (e: IllegalArgumentException) {
            Log.e("AdvertisingUseCase", "Invalid UUID format", e)
        }
    }

    fun stop() {
        val ctx = appContext ?: return
        val intent = Intent(ctx, BleAdvertisingService::class.java).apply {
            action = BleAdvertisingService.ACTION_STOP
        }
        ctx.startForegroundService(intent) // Посылаем команду STOP через старт
        _isRunning.value = false
    }

    private fun startWithParams(serviceUuid: UUID, adData: String) {
        val ctx = appContext ?: return
        val intent = Intent(ctx, BleAdvertisingService::class.java).apply {
            putExtra(BleAdvertisingService.EXTRA_SERVICE_UUID, serviceUuid.toString())
            putExtra(BleAdvertisingService.EXTRA_AD_DATA, adData)
        }
        try {
            ctx.startForegroundService(intent)
            _isRunning.value = true
            Log.d("AdvertisingUseCase", "Service started successfully")
        } catch (e: Exception) {
            Log.e("AdvertisingUseCase", "Failed to start service", e)
            _isRunning.value = false
        }
    }

    fun restartWithCurrentUser() {
        if (!_isRunning.value) return
        stop()
        scope.launch {
            delay(200)
            start()
        }
    }

    fun onUserChanged() {
        restartWithCurrentUser()
    }

    fun syncState() {
        val ctx = appContext ?: return
        val isRunning = checkIfServiceIsRunning(ctx)
        _isRunning.value = isRunning
        // НЕ вызываем здесь onServiceStateChanged, чтобы не сбивать тумблер при старте
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
            Log.e("AdvertisingUseCase", "Error checking service", e)
            false
        }
    }
}
