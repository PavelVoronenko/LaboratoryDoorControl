package com.antago30.laboratory.model

import com.google.gson.annotations.SerializedName

data class StaffMember(
    @SerializedName("id") val id: String,
    @SerializedName("initials") val initials: String,
    @SerializedName("name") val name: String,
    @SerializedName("isInside") val isInside: Boolean,
    @SerializedName("lastUpdated") val lastUpdated: Long = System.currentTimeMillis(),
    @SerializedName("serviceUUID") val serviceUUID: String,
    @SerializedName("adData") val adData: String = "J7hs2Ak98g",
    @SerializedName("macAddress") val macAddress: String
) {
    fun getFormattedMacAddress(): String {
        return macAddress.uppercase()
    }
}