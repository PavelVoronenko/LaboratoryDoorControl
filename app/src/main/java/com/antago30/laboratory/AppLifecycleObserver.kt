package com.antago30.laboratory

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.antago30.laboratory.ble.BleConnectionManager

class AppLifecycleObserver(
    private val connectionManager: BleConnectionManager
) : DefaultLifecycleObserver {

    @Suppress("MissingPermission")
    override fun onStop(owner: LifecycleOwner) {
        Log.d("AppLifecycleObserver", "onStop: App is in background, disconnecting...")
        connectionManager.disconnect()
    }

    @Suppress("MissingPermission")
    override fun onStart(owner: LifecycleOwner) {
        Log.d("AppLifecycleObserver", "onStart: App is in foreground, reconnecting...")
        connectionManager.attemptReconnect()
    }
}
