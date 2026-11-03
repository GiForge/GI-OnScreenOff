package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color(0xFF1E1400),
    primaryContainer = Color(0xFF453000),
    onPrimaryContainer = AmberSecondary,
    secondary = AmberSecondary,
    onSecondary = Color(0xFF1E1400),
    background = DarkBackground,
    onBackground = Color(0xFFF0F6FC),
    surface = DarkSurface,
    onSurface = Color(0xFFF0F6FC),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC9D1D9),
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = AmberTertiary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFECB3),
    onPrimaryContainer = Color(0xFF422800),
    secondary = AmberPrimary,
    onSecondary = Color.Black,
    background = LightBackground,
    onBackground = Color(0xFF1F2328),
    surface = LightSurface,
    onSurface = Color(0xFF1F2328),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF57606A),
    outline = LightOutline
)

@Composable
fun LuceSchermoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep intentional warm amber brand styling
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // App is centered around glowing lightbulb, dark theme by default looks best
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
