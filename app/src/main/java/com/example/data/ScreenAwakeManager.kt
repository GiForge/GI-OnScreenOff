package com.example.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.example.service.KeepAwakeService
import com.example.widget.BulbWidgetProvider
import com.example.widget.PowerWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScreenAwakeManager {
    private const val PREFS_NAME = "luce_screen_prefs"
    private const val KEY_IS_ACTIVE = "key_is_active"
    private const val KEY_DURATION_MINUTES = "key_duration_minutes"
    private const val KEY_BATTERY_PROTECT = "key_battery_protect"
    private const val KEY_HAPTIC = "key_haptic"

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _durationMinutes = MutableStateFlow(0) // 0 = Infinite
    val durationMinutes: StateFlow<Int> = _durationMinutes.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _batteryProtection = MutableStateFlow(true)
    val batteryProtection: StateFlow<Boolean> = _batteryProtection.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private var initialized = false
    private var globalWakeLock: PowerManager.WakeLock? = null

    fun init(context: Context) {
        if (initialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val active = prefs.getBoolean(KEY_IS_ACTIVE, false)
        _isActive.value = active
        _durationMinutes.value = prefs.getInt(KEY_DURATION_MINUTES, 0)
        _batteryProtection.value = prefs.getBoolean(KEY_BATTERY_PROTECT, true)
        _hapticEnabled.value = prefs.getBoolean(KEY_HAPTIC, true)
        initialized = true

        // If inactive on init, guarantee no wakeLock remains held
        if (!active) {
            releaseWakeLock(context)
        }
    }

    @Synchronized
    fun acquireWakeLock(context: Context) {
        try {
            if (globalWakeLock == null) {
                val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                @Suppress("DEPRECATION")
                globalWakeLock = powerManager?.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                    "GI-OnScreenOff:ScreenAwakeLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            if (globalWakeLock?.isHeld != true) {
                globalWakeLock?.acquire()
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    @Synchronized
    fun releaseWakeLock(context: Context) {
        try {
            if (globalWakeLock?.isHeld == true) {
                globalWakeLock?.release()
            }
        } catch (e: Exception) {
            // Ignored
        }
        globalWakeLock = null
    }

    fun isAwake(context: Context): Boolean {
        init(context)
        return _isActive.value
    }

    fun toggle(context: Context) {
        init(context)
        val newState = !_isActive.value
        setActive(context, newState, _durationMinutes.value)
    }

    fun setActive(context: Context, active: Boolean, durationMin: Int = -1) {
        init(context)
        val finalDuration = if (durationMin >= 0) durationMin else _durationMinutes.value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        prefs.edit()
            .putBoolean(KEY_IS_ACTIVE, active)
            .putInt(KEY_DURATION_MINUTES, finalDuration)
            .apply()

        _isActive.value = active
        _durationMinutes.value = finalDuration

        if (active) {
            acquireWakeLock(context)
            val serviceIntent = Intent(context, KeepAwakeService::class.java).apply {
                action = KeepAwakeService.ACTION_START
                putExtra(KeepAwakeService.EXTRA_DURATION_MINUTES, finalDuration)
            }
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                // Foreground service launch edge case
            }
        } else {
            // Instantly and synchronously release the WakeLock
            releaseWakeLock(context)
            _remainingSeconds.value = 0L
            // Stop service and remove notification completely
            KeepAwakeService.stop(context)
        }

        performHaptic(context)
        updateWidgets(context)
    }

    fun setDuration(context: Context, minutes: Int) {
        init(context)
        _durationMinutes.value = minutes
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DURATION_MINUTES, minutes).apply()

        if (_isActive.value) {
            setActive(context, true, minutes)
        }
    }

    fun setBatteryProtection(context: Context, enabled: Boolean) {
        init(context)
        _batteryProtection.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_BATTERY_PROTECT, enabled).apply()
    }

    fun setHapticEnabled(context: Context, enabled: Boolean) {
        init(context)
        _hapticEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HAPTIC, enabled).apply()
    }

    fun updateRemainingSeconds(seconds: Long) {
        _remainingSeconds.value = seconds
    }

    fun onServiceStopped(context: Context) {
        releaseWakeLock(context)
        _isActive.value = false
        _remainingSeconds.value = 0L
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_IS_ACTIVE, false).apply()
        updateWidgets(context)
    }

    fun updateWidgets(context: Context) {
        try {
            BulbWidgetProvider.updateAllWidgets(context)
            PowerWidgetProvider.updateAllWidgets(context)
        } catch (e: Exception) {
            // Widget update safeguard
        }
    }

    fun performHaptic(context: Context) {
        if (!_hapticEnabled.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(45)
                }
            }
        } catch (e: Exception) {
            // Safe ignore if vibration not supported
        }
    }
}
