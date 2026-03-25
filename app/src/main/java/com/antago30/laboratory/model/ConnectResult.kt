package com.antago30.laboratory.model

sealed class ConnectResult {
    object Connecting : ConnectResult()
    data class Error(val message: String) : ConnectResult()
}