package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.LuceApp
import com.example.MainActivity
import com.example.R
import com.example.data.ScreenAwakeManager
import com.example.widget.BulbWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KeepAwakeService : Service() {

    companion object {
        const val ACTION_START = "com.example.luceschermo.ACTION_START"
        const val ACTION_STOP = "com.example.luceschermo.ACTION_STOP"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val NOTIFICATION_ID = 1001

        fun stop(context: Context) {
            try {
                ScreenAwakeManager.releaseWakeLock(context)
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.cancel(NOTIFICATION_ID)
                context.stopService(Intent(context, KeepAwakeService::class.java))
            } catch (e: Exception) {
                // Ignored
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var targetTimeMillis: Long = 0L

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    val batteryPct = (level * 100) / scale
                    if (batteryPct <= 15 && ScreenAwakeManager.batteryProtection.value) {
                        ScreenAwakeManager.setActive(this@KeepAwakeService, false)
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (e: Exception) {
            // Ignore if unable to register receiver
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP || !ScreenAwakeManager.isAwake(this)) {
            stopKeepAwake()
            return START_NOT_STICKY
        }

        // Ensure WakeLock is acquired
        ScreenAwakeManager.acquireWakeLock(this)

        val durationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 0) ?: 0
        startForegroundWithNotification(durationMinutes)

        if (durationMinutes > 0) {
            startTimer(durationMinutes)
        } else {
            timerJob?.cancel()
            ScreenAwakeManager.updateRemainingSeconds(0L)
            updateNotification("Sempre attivo (finché non lo spegni)")
        }

        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification(durationMinutes: Int) {
        val notification = buildNotification(
            if (durationMinutes > 0) "Attivo per $durationMinutes minuti" else "Schermo sempre attivo"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTimer(durationMinutes: Int) {
        timerJob?.cancel()
        val totalSeconds = durationMinutes * 60L
        targetTimeMillis = System.currentTimeMillis() + (totalSeconds * 1000L)

        timerJob = serviceScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val remainingMillis = targetTimeMillis - now
                if (remainingMillis <= 0) {
                    ScreenAwakeManager.updateRemainingSeconds(0L)
                    ScreenAwakeManager.setActive(this@KeepAwakeService, false)
                    break
                }
                val remainingSec = (remainingMillis + 999) / 1000
                ScreenAwakeManager.updateRemainingSeconds(remainingSec)

                val mins = remainingSec / 60
                val secs = remainingSec % 60
                val timeStr = String.format("%02d:%02d rimanenti", mins, secs)
                updateNotification("Tempo rimasto: $timeStr")

                delay(1000)
            }
        }
    }

    private fun buildNotification(subtitle: String): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingAppIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use Broadcast to BulbWidgetProvider ACTION_TOGGLE_AWAKE for reliable 1-click turn off
        val stopIntent = Intent(this, BulbWidgetProvider::class.java).apply {
            action = BulbWidgetProvider.ACTION_TOGGLE_AWAKE
        }
        val pendingStopIntent = PendingIntent.getBroadcast(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, LuceApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bulb_vector)
            .setContentTitle(getString(R.string.notification_title_active))
            .setContentText(subtitle)
            .setContentIntent(pendingAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_bulb_vector,
                getString(R.string.notification_action_turn_off),
                pendingStopIntent
            )
            .build()
    }

    private fun updateNotification(subtitle: String) {
        if (!ScreenAwakeManager.isAwake(this)) return
        val notification = buildNotification(subtitle)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun stopKeepAwake() {
        timerJob?.cancel()
        ScreenAwakeManager.releaseWakeLock(this)
        ScreenAwakeManager.onServiceStopped(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Ignored
        }
        ScreenAwakeManager.releaseWakeLock(this)
        ScreenAwakeManager.onServiceStopped(this)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.cancel(NOTIFICATION_ID)
    }
}
