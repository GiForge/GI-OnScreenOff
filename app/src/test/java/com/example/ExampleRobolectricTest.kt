package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ScreenAwakeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GI-OnScreenOff", appName)
    }

    @Test
    fun `test screen awake toggle and duration settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ScreenAwakeManager.init(context)

        // Ensure default state or toggle
        val initial = ScreenAwakeManager.isAwake(context)
        ScreenAwakeManager.setActive(context, true, 15)
        assertTrue(ScreenAwakeManager.isAwake(context))
        assertEquals(15, ScreenAwakeManager.durationMinutes.value)

        ScreenAwakeManager.setActive(context, false)
        assertFalse(ScreenAwakeManager.isAwake(context))
    }

    @Test
    fun `test screen off turns off awake manager`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ScreenAwakeManager.init(context)
        ScreenAwakeManager.setActive(context, true)
        assertTrue(ScreenAwakeManager.isAwake(context))

        com.example.util.ScreenOffHelper.turnScreenOff(context)
        assertFalse(ScreenAwakeManager.isAwake(context))
    }

    @Test
    fun `test widget providers update without exceptions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.widget.BulbWidgetProvider.updateAllWidgets(context)
        com.example.widget.PowerWidgetProvider.updateAllWidgets(context)
        assertTrue(true)
    }
}
