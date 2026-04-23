package com.antago30.laboratory.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.antago30.laboratory.R

class LabWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_OPEN_DOOR -> {
                startActionService(context, "OPENDOOR")
            }
            ACTION_TOGGLE_LIGHT -> {
                startActionService(context, "TOGGLE_LIGHT")
            }
        }
    }

    private fun startActionService(context: Context, action: String) {
        val intent = Intent(context, LabWidgetService::class.java).apply {
            this.action = action
        }
        context.startForegroundService(intent)
    }

    companion object {
        const val ACTION_OPEN_DOOR = "com.antago30.laboratory.action.OPEN_DOOR"
        const val ACTION_TOGGLE_LIGHT = "com.antago30.laboratory.action.TOGGLE_LIGHT"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int) {
            
            val views = RemoteViews(context.packageName, R.layout.lab_widget)

            // Настраиваем Intent для открытия двери
            val openDoorIntent = Intent(context, LabWidgetProvider::class.java).apply {
                action = ACTION_OPEN_DOOR
            }
            val openDoorPendingIntent = PendingIntent.getBroadcast(
                context, 0, openDoorIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_open_door, openDoorPendingIntent)

            // Настраиваем Intent для света
            val lightIntent = Intent(context, LabWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_LIGHT
            }
            val lightPendingIntent = PendingIntent.getBroadcast(
                context, 1, lightIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_toggle_light, lightPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
