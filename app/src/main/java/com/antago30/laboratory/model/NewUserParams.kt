package com.antago30.laboratory.model

data class NewUserParams(
    val id: Int,
    val name: String,           // Имя + Отчество
    val uuid: String,           // Формат: 00000000-0000-1000-8000-00805f9b34fb
    val serviceData: String,    // Service data hex
    val macAddress: String,     // Формат: XX:XX:XX:XX:XX:XX
    val rssiThreshold: Int = -70 // По умолчанию
) {
    // Форматируем команду для ESP32: ADDUSER:id|name|uuid|serviceData|mac|threshold
    fun toEsp32Command(): String {
        return "ADDUSER:$id|$name|$uuid|$serviceData|$macAddress|$rssiThreshold"
    }

    // Валидация полей
    fun isValid(): Boolean {
        return id > 0 &&
                name.isNotBlank() &&
                uuid.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) &&
                serviceData.isNotBlank() &&
                macAddress.matches(Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}"))
    }
}