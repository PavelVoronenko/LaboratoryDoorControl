package com.antago30.laboratory.ble.bleConnectionManager

import com.antago30.laboratory.model.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleConnectionState {
    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    fun update(newState: ConnectionState) { _state.value = newState }
    val current: ConnectionState get() = _state.value
}