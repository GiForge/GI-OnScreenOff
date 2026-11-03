package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R
import com.example.util.ScreenOffHelper

class PowerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_SCREEN_OFF_SINGLE = "com.example.luceschermo.ACTION_SCREEN_OFF_SINGLE"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PowerWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_power_single)
            views.setImageViewResource(R.id.power_single_icon, R.drawable.img_power_off_btn)

            val screenOffIntent = Intent(context, PowerWidgetProvider::class.java).apply {
                action = ACTION_SCREEN_OFF_SINGLE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 100 + 3,
                screenOffIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.power_single_widget_container, pendingIntent)
            views.setOnClickPendingIntent(R.id.power_single_icon, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

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
        if (intent.action == ACTION_SCREEN_OFF_SINGLE) {
            ScreenOffHelper.turnScreenOff(context)
        } else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            updateAllWidgets(context)
        }
    }
}
