package com.antago30.laboratory

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.antago30.laboratory.ble.BleConnectionManager

class AppLifecycleObserver(
    private val connectionManager: BleConnectionManager
) : DefaultLifecycleObserver {

    override fun onPause(owner: LifecycleOwner) {
        connectionManager.disconnect()
    }

    override fun onResume(owner: LifecycleOwner) {
        connectionManager.attemptReconnect()
    }
}