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
    primary = SL_NeonViolet,
    onPrimary = Color.White,

    secondary = SL_NeonBlue,
    onSecondary = Color.Black,

    tertiary = SL_NeonCyan,
    onTertiary = Color.Black,

    background = SL_Black,
    surface = SL_Surface,

    onBackground = SL_Text,
    onSurface = SL_Text,
    surfaceVariant = SL_Deep,
    onSurfaceVariant = SL_TextDim,

    outline = SL_Stroke,
    error = SL_Red,
    onError = Color.White
)

private val LightScheme = lightColorScheme(
    primary = SL_NeonViolet,
    onPrimary = Color.White,

    secondary = SL_NeonBlue,
    onSecondary = Color.Black,

    tertiary = SL_NeonCyan,
    onTertiary = Color.Black,

    background = LightBg,
    surface = LightSurface,

    onBackground = Color(0xFF0F141A),
    onSurface = Color(0xFF0F141A),
    onSurfaceVariant = Color(0xFF1F2937)
)

@Composable
fun ProGuinTheme(
    darkTheme: Boolean = true,
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
