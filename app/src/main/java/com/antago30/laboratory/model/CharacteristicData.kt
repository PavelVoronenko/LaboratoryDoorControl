package com.antago30.laboratory.model

data class CharacteristicData(
    val uuid: String,
    val value: List<Byte>,
    val timestamp: Long = System.currentTimeMillis()
)