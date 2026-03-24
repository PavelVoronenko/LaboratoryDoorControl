package com.antago30.laboratory.ble.bleConnectionManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, SERVICES_DISCOVERING, READY, DISCONNECTING
}

class BleConnectionState {
    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    fun update(newState: ConnectionState) { _state.value = newState }
    val current: ConnectionState get() = _state.value
}