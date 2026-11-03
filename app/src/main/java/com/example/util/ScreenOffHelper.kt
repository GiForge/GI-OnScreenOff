package com.example.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.data.ScreenAwakeManager
import com.example.receiver.LuceDeviceAdminReceiver
import com.example.service.LuceAccessibilityService
import com.example.ui.ScreenOffActivity

object ScreenOffHelper {

    fun isPermissionGranted(context: Context): Boolean {
        return LuceAccessibilityService.isRunning() || LuceDeviceAdminReceiver.isAdminActive(context)
    }

    fun turnScreenOff(context: Context): Boolean {
        // 1. Immediately deactivate keep-awake lock and give tactile haptic feedback
        ScreenAwakeManager.setActive(context, false)
        ScreenAwakeManager.performHaptic(context)

        // 2. Try modern Accessibility lock (Android 9+)
        if (LuceAccessibilityService.lockScreen()) {
            return true
        }

        // 3. Try Device Admin lock
        if (LuceDeviceAdminReceiver.lockNow(context)) {
            return true
        }

        // 4. If neither permission is active, launch setup activity
        val setupIntent = Intent(context, ScreenOffActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(setupIntent)
        return false
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback
        }
    }

    fun openDeviceAdminSettings(context: Context) {
        try {
            val intent = LuceDeviceAdminReceiver.createEnableIntent(context).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback
        }
    }
}
