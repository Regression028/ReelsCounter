package com.example.reelscounter.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = ReelsAccent,
    secondary = ShortsAccent,
    background = PaperLight,
    surface = SurfaceLight,
    onBackground = InkLight,
    onSurface = InkLight,
    outline = MutedLight
)

private val DarkColors = darkColorScheme(
    primary = ReelsAccent,
    secondary = ShortsAccent,
    background = PaperDark,
    surface = SurfaceDark,
    onBackground = InkDark,
    onSurface = InkDark,
    outline = MutedDark
)

/**
 * Deliberately does NOT support Android 12+ dynamic color. This app has
 * a specific warm-paper palette that's part of its identity — letting
 * the system's wallpaper-derived palette override it would defeat the
 * point of designing it in the first place.
 */
@Composable
fun ReelsCounterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val context = LocalContext.current

    if (context !is Activity) {
        // Preview/non-Activity context — just apply the color scheme.
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
        return
    }

    val currentView = LocalView.current
    SideEffect {
        val window = context.window
        window.statusBarColor = colorScheme.background.toArgb()
        WindowCompat.getInsetsController(window, currentView).isAppearanceLightStatusBars =
            !darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}