package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R
import com.example.data.ScreenAwakeManager
import com.example.util.ScreenOffHelper

class BulbWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE_AWAKE = "com.example.luceschermo.ACTION_TOGGLE_AWAKE"
        const val ACTION_SCREEN_OFF = "com.example.luceschermo.ACTION_SCREEN_OFF"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, BulbWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            } catch (e: Exception) {
                // Safeguard
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val isActive = ScreenAwakeManager.isAwake(context)
            val views = RemoteViews(context.packageName, R.layout.widget_bulb)

            // Button 1: Lightbulb Always-On toggle
            if (isActive) {
                views.setImageViewResource(R.id.widget_bulb_image, R.drawable.img_bulb_on)
                views.setInt(R.id.bulb_toggle_area, "setBackgroundResource", R.drawable.widget_btn_gold_bg)
                views.setTextViewText(R.id.widget_status_text, "SEMPRE ATTIVO")
                views.setTextColor(R.id.widget_status_text, 0xFFFFC107.toInt())
                views.setTextViewText(R.id.widget_sub_text, "💡 Tocca per timeout")
            } else {
                views.setImageViewResource(R.id.widget_bulb_image, R.drawable.img_bulb_off)
                views.setInt(R.id.bulb_toggle_area, "setBackgroundResource", R.drawable.widget_card_bg_off)
                views.setTextViewText(R.id.widget_status_text, "TIMEOUT NORMALE")
                views.setTextColor(R.id.widget_status_text, 0xFFB0B8C4.toInt())
                views.setTextViewText(R.id.widget_sub_text, "Tocca per attivare")
            }

            val toggleIntent = Intent(context, BulbWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_AWAKE
            }
            val pendingToggleIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 1,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.bulb_toggle_area, pendingToggleIntent)
            views.setOnClickPendingIntent(R.id.widget_bulb_image, pendingToggleIntent)

            // Button 2: Immediate Screen Off (Power Button)
            views.setImageViewResource(R.id.widget_power_icon, R.drawable.img_power_off_btn)
            views.setInt(R.id.btn_screen_off_area, "setBackgroundResource", R.drawable.widget_btn_red_bg)
            views.setTextViewText(R.id.widget_power_text, "SPEGNI")
            views.setTextColor(R.id.widget_power_text, 0xFFFFFFFF.toInt())
            views.setTextViewText(R.id.widget_power_sub_text, "Schermo")

            val screenOffIntent = Intent(context, BulbWidgetProvider::class.java).apply {
                action = ACTION_SCREEN_OFF
            }
            val pendingScreenOffIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 2,
                screenOffIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_screen_off_area, pendingScreenOffIntent)
            views.setOnClickPendingIntent(R.id.widget_power_icon, pendingScreenOffIntent)

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
        when (intent.action) {
            ACTION_TOGGLE_AWAKE -> {
                ScreenAwakeManager.toggle(context)
                updateAllWidgets(context)
            }
            ACTION_SCREEN_OFF -> {
                ScreenOffHelper.turnScreenOff(context)
                updateAllWidgets(context)
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                updateAllWidgets(context)
            }
        }
    }
}
