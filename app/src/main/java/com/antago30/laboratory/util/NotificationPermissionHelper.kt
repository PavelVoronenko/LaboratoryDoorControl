package com.antago30.laboratory.util

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/**
 * Утилита для однократного запроса разрешения на уведомления при старте приложения.
 */
object NotificationPermissionHelper {

    private const val PREFS_NAME = "notification_permission_prefs"
    private const val KEY_REQUESTED = "notification_permission_requested"

    fun isPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isAlreadyRequested(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REQUESTED, false)
    }

    fun markAsRequested(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_REQUESTED, true) }
    }

    /**
     * Запрашивает разрешение, если оно ещё не получено и не запрашивалось ранее.
     * @return true если разрешение уже есть или не требуется (Android < 13), false если запрос запущен
     */
    fun requestIfNeeded(activity: ComponentActivity, launcher: ActivityResultLauncher<String>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        if (isPermissionGranted(activity)) {
            return true
        }

        if (isAlreadyRequested(activity)) {
            return false
        }

        markAsRequested(activity)
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        return false
    }
}
