package com.antago30.laboratory.ble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.antago30.laboratory.util.SettingsRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted, checking if advertising should start")
            
            val settingsRepo = SettingsRepository(context.applicationContext)
            if (!settingsRepo.isAdvertisingEnabled()) {
                Log.d("BootReceiver", "Advertising is disabled in settings, skipping")
                return
            }

            val currentUser = settingsRepo.getCurrentUserInfo()
            
            if (currentUser != null) {
                Log.d("BootReceiver", "Starting BleAdvertisingService for user: ${currentUser.name}")
                val serviceIntent = Intent(context, BleAdvertisingService::class.java).apply {
                    putExtra(BleAdvertisingService.EXTRA_SERVICE_UUID, currentUser.uuid)
                    putExtra(BleAdvertisingService.EXTRA_AD_DATA, currentUser.serviceData)
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
