package com.antago30.laboratory.model

data class NewUserParams(
    val id: Int,
    val name: String,           // Имя + Отчество
    val uuid: String,           // Формат: 00000000-0000-1000-8000-00805f9b34fb
    val serviceData: String,    // Service data hex
    val macAddress: String,     // Формат: XX:XX:XX:XX:XX:XX
    val rssiThresholdEntry: Int = -70,
    val rssiThresholdExit: Int = -70
) {
    // Форматируем команду для ESP32: ADDUSER:id|name|uuid|serviceData|mac|entryThreshold|exitThreshold
    fun toEsp32Command(): String {
        return "ADDUSER:$id|$name|$uuid|$serviceData|$macAddress|$rssiThresholdEntry|$rssiThresholdExit"
    }

    // Валидация полей
    fun isValid(): Boolean {
        val cleanMac = macAddress.replace(":", "")
        return id > 0 &&
                name.isNotBlank() &&
                uuid.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) &&
                serviceData.isNotBlank() &&
                cleanMac.length == 12 &&
                cleanMac.matches(Regex("[0-9A-Fa-f]{12}"))
    }
}
