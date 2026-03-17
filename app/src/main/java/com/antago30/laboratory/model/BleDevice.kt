package com.antago30.laboratory.model

data class BleDevice(
    val name: String?,
    val address: String,
    val rssi: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val displayName: String get() = name ?: "Unknown ($address)"
}