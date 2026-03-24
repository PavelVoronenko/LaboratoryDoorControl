package com.antago30.laboratory

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.antago30.laboratory.ble.BleConnectionManager

class AppLifecycleObserver(
    private val connectionManager: BleConnectionManager
) : DefaultLifecycleObserver {

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onPause(owner: LifecycleOwner) {
        Log.d("AppLifecycleObserver", "onPause: Disconnecting...")
        connectionManager.disconnect()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onResume(owner: LifecycleOwner) {
        Log.d("AppLifecycleObserver", "🔍 AppLifecycleObserver: onResume CALLED!")
        connectionManager.attemptReconnect()
    }
}