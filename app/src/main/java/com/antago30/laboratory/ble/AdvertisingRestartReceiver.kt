package com.antago30.laboratory.ble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AdvertisingRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("RestartReceiver", "⏰ It's 6:00 AM! Triggering advertising restart...")
        
        val serviceIntent = Intent(context, BleAdvertisingService::class.java).apply {
            action = BleAdvertisingService.ACTION_RESTART
        }
        
        try {
            // Используем startForegroundService, так как мы запускаемся из фонового ресивера
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            Log.e("RestartReceiver", "Failed to restart service at 6 AM", e)
        }
    }
}
