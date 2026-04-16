package com.antago30.laboratory.ui.component.settingsScreen.terminalLog

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class TerminalLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val timestamp: LocalTime = LocalTime.now()
) {
    fun getFormattedTime(): String {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }
}