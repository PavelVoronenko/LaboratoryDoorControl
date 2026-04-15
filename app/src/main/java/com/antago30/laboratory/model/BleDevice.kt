package com.antago30.laboratory.model

data class BleDevice(
    val name: String?,
    val address: String,
    val rssi: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = if (name.isNullOrBlank()) {
            "Unknown"
        } else {
            // Убираем мак-адрес из скобок, если он есть в названии
            name.replace(Regex("\\s*\\([0-9A-Fa-f:]{17}\\)"), "").trim()
        }
}