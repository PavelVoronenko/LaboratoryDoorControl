package com.antago30.laboratory.ui.component.settingsScreen.terminalLog

import androidx.compose.ui.graphics.Color
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class LogType {
    INFO,    // Система / JDY-33
    DOOR,    // Открытие/закрытие двери
    USER,    // Действия пользователей
    WARNING, // Предупреждения
    DATE_HEADER // Заголовок даты
}

data class TerminalLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val type: LogType = LogType.INFO,
    val timestamp: LocalTime = LocalTime.now(),
    val userName: String? = null
) {
    fun getFormattedTime(): String {
        return "[${timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}]"
    }

    fun getTimeColor(): Color = Color(0xFF718096).copy(alpha = 0.5f)

    fun getMessageColor(): Color = when (type) {
        LogType.WARNING -> Color(0xFFF56565)
        LogType.USER -> Color(0xFF4FC3F7)
        else -> Color(0xFF70A0F1).copy(alpha = 0.75f)
    }
}
