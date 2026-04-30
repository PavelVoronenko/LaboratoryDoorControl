package com.antago30.laboratory.util

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

object AlarmPermissionHelper {
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true // На старых версиях разрешение не требуется
    }

    fun openAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("AlarmPermissionHelper", "Failed to open settings", e)
            }
        }
    }
}
