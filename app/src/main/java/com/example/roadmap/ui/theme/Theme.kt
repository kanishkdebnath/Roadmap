package com.example.roadmap.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LightTokens.primary,
    onPrimary = LightTokens.onPrimary,
    primaryContainer = Color(0xFFE8EFEA),
    onPrimaryContainer = Brand,
    background = LightTokens.background,
    onBackground = LightTokens.onBackground,
    surface = LightTokens.surface,
    onSurface = LightTokens.onBackground,
    surfaceVariant = LightTokens.surfaceVariant,
    onSurfaceVariant = LightTokens.onSurfaceVariant,
    outline = LightTokens.outline,
    outlineVariant = LightTokens.outlineVariant,
    error = LightTokens.overdue,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkTokens.primary,
    onPrimary = DarkTokens.onPrimary,
    primaryContainer = Color(0xFF10301F),
    onPrimaryContainer = BrandMint,
    background = DarkTokens.background,
    onBackground = DarkTokens.onBackground,
    surface = DarkTokens.surface,
    onSurface = DarkTokens.onBackground,
    surfaceVariant = DarkTokens.surfaceVariant,
    onSurfaceVariant = DarkTokens.onSurfaceVariant,
    outline = DarkTokens.outline,
    outlineVariant = DarkTokens.outlineVariant,
    error = DarkTokens.overdue,
    onError = DarkTokens.onPrimary,
)

@Composable
fun RoadmapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extended = if (darkTheme) DarkRoadmapColors else LightRoadmapColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalRoadmapColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = RoadmapShapes,
            content = content,
        )
    }
}
