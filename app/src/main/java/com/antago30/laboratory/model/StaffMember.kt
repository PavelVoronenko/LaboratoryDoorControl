package com.antago30.laboratory.model

data class StaffMember(
    val id: String,
    val initials: String,
    val name: String,
    val isInside: Boolean,
    val lastUpdated: Long = System.currentTimeMillis() // Метка времени обновления статуса
)