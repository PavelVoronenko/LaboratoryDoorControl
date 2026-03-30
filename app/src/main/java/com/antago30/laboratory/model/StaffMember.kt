package com.antago30.laboratory.model

import com.google.gson.annotations.SerializedName

data class StaffMember(
    @SerializedName("id") val id: String,
    @SerializedName("initials") val initials: String,
    @SerializedName("name") val name: String,
    @SerializedName("isInside") val isInside: Boolean,
    @SerializedName("lastUpdated") val lastUpdated: Long = System.currentTimeMillis(),
    @SerializedName("serviceUUID") val serviceUUID: String,
    @SerializedName("descriptorUUID") val descriptorUUID: String,
    @SerializedName("macAddress") val macAddress: String
) {
    fun isValid(): Boolean {
        return id.isNotBlank() &&
                initials.isNotBlank() &&
                name.isNotBlank() &&
                serviceUUID.isNotBlank() &&
                descriptorUUID.isNotBlank() &&
                macAddress.isNotBlank()
    }

    fun isValidUuidFormat(): Boolean {
        val uuidPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        return uuidPattern.matches(serviceUUID) && uuidPattern.matches(descriptorUUID)
    }

    fun isValidMacFormat(): Boolean {
        val macPattern = Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
        return macPattern.matches(macAddress)
    }

    fun getFormattedMacAddress(): String {
        return macAddress.uppercase()
    }

    fun getShortServiceUUID(): String {
        return if (serviceUUID.length > 8)
            "${serviceUUID.substring(0, 8)}..."
        else serviceUUID
    }
}