package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.ScreenAwakeManager
import com.example.widget.BulbWidgetProvider
import com.example.widget.PowerWidgetProvider

class LuceApp : Application() {

    companion object {
        const val CHANNEL_ID = "keep_awake_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ScreenAwakeManager.init(this)
        if (!ScreenAwakeManager.isAwake(this)) {
            ScreenAwakeManager.releaseWakeLock(this)
            com.example.service.KeepAwakeService.stop(this)
        }
        BulbWidgetProvider.updateAllWidgets(this)
        PowerWidgetProvider.updateAllWidgets(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
