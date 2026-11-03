package com.example.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import com.example.data.ScreenAwakeManager
import com.example.util.ScreenOffHelper
import com.example.widget.BulbWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {

    val isActive: StateFlow<Boolean> = ScreenAwakeManager.isActive
    val durationMinutes: StateFlow<Int> = ScreenAwakeManager.durationMinutes
    val remainingSeconds: StateFlow<Long> = ScreenAwakeManager.remainingSeconds
    val batteryProtection: StateFlow<Boolean> = ScreenAwakeManager.batteryProtection
    val hapticEnabled: StateFlow<Boolean> = ScreenAwakeManager.hapticEnabled

    private val _isScreenOffGranted = MutableStateFlow(false)
    val isScreenOffGranted: StateFlow<Boolean> = _isScreenOffGranted.asStateFlow()

    private val _isLampMode = MutableStateFlow(false)
    val isLampMode: StateFlow<Boolean> = _isLampMode.asStateFlow()

    private val _lampBrightness = MutableStateFlow(0.85f)
    val lampBrightness: StateFlow<Float> = _lampBrightness.asStateFlow()

    private val _showPinSuccessDialog = MutableStateFlow(false)
    val showPinSuccessDialog: StateFlow<Boolean> = _showPinSuccessDialog.asStateFlow()

    fun checkPermissions(context: Context) {
        _isScreenOffGranted.value = ScreenOffHelper.isPermissionGranted(context)
    }

    fun toggle(context: Context) {
        ScreenAwakeManager.toggle(context)
    }

    fun setDuration(context: Context, minutes: Int) {
        ScreenAwakeManager.setDuration(context, minutes)
    }

    fun setBatteryProtection(context: Context, enabled: Boolean) {
        ScreenAwakeManager.setBatteryProtection(context, enabled)
    }

    fun setHapticEnabled(context: Context, enabled: Boolean) {
        ScreenAwakeManager.setHapticEnabled(context, enabled)
    }

    fun setLampMode(active: Boolean) {
        _isLampMode.value = active
    }

    fun setLampBrightness(brightness: Float) {
        _lampBrightness.value = brightness
    }

    fun dismissPinDialog() {
        _showPinSuccessDialog.value = false
    }

    fun testScreenOff(context: Context) {
        ScreenOffHelper.turnScreenOff(context)
    }

    fun isPinWidgetSupported(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            appWidgetManager.isRequestPinAppWidgetSupported
        } else {
            false
        }
    }

    fun requestPinWidget(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val provider = ComponentName(context, BulbWidgetProvider::class.java)
                val success = appWidgetManager.requestPinAppWidget(provider, null, null)
                if (success) {
                    _showPinSuccessDialog.value = true
                }
                return success
            }
        }
        return false
    }

    fun requestPinPowerWidget(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val provider = ComponentName(context, com.example.widget.PowerWidgetProvider::class.java)
                val success = appWidgetManager.requestPinAppWidget(provider, null, null)
                if (success) {
                    _showPinSuccessDialog.value = true
                }
                return success
            }
        }
        return false
    }
}
