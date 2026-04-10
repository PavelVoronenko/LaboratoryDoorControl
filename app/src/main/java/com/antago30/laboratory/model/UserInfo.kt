package com.antago30.laboratory.model


data class UserInfo(
    val id: Int,
    val name: String,
    val macAddress: String,
    val location: String,
    val isSelected: Boolean = false,
    val uuid: String,
    val serviceData: String
)