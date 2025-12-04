package com.antago30.laboratory.viewmodel

import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.antago30.laboratory.model.FunctionItem
import com.antago30.laboratory.model.StaffMember

class LabControlViewModel : ViewModel() {
    val staffList = mutableStateOf(
        listOf(
            StaffMember("1", "ВВ", "Владимир Викторович", true),
            StaffMember("2", "ВО", "Вячеслав Олегович", false),
            StaffMember("3", "ПЕ", "Павел Евгеньевич", true)
        )
    )

    val functions = mutableStateOf(
        listOf(
            FunctionItem("broadcast", "📡 Вещание рекламы", false),
            FunctionItem("cleaning", "🧹 Режим уборки", false),
            FunctionItem("lighting", "💡 Освещение", false)
        )
    )

    val isBroadcasting: Boolean
        get() = functions.value.find { it.id == "broadcast" }?.isEnabled == true

    fun toggleStaffStatus(id: String) {
        staffList.value = staffList.value.map {
            if (it.id == id) it.copy(isInside = !it.isInside) else it
        }
    }

    fun toggleFunction(id: String) {
        functions.value = functions.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    fun onOpenDoorClicked() {

    }
}