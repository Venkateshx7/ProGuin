package com.venkatesh.proguin.ui.theme

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

private val DarkScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF001314),

    secondary = NeonPink,
    onSecondary = Color(0xFF1A0014),

    tertiary = NeonPink,

    background = DarkBg,
    surface = DarkSurface,

    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFE3E8F5)
)

private val LightScheme = lightColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF001314),

    secondary = NeonPink,
    onSecondary = Color(0xFF1A0014),

    tertiary = NeonPink,

    background = LightBg,
    surface = LightSurface,

    onBackground = Color(0xFF0F141A),
    onSurface = Color(0xFF0F141A),
    onSurfaceVariant = Color(0xFF1F2937)
)

@Composable
fun ProGuinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // keep FALSE for your premium neon palette
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) DarkScheme else LightScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
