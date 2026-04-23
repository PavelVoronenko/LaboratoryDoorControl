package com.antago30.laboratory.util

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Утилита для запроса разрешения на уведомления.
 */
object NotificationPermissionHelper {

    fun isPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Запрашивает разрешение, если оно ещё не получено.
     */
    fun requestIfNeeded(activity: ComponentActivity, launcher: ActivityResultLauncher<String>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        if (isPermissionGranted(activity)) {
            return true
        }

        // Больше не проверяем, запрашивалось ли ранее, чтобы дать возможность 
        // системному диалогу появиться снова, если это позволяет ОС.
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        return false
    }
}
