package com.example.roadmap.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal val LightColorScheme = lightColorScheme(
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
    // Selected nav indicator / secondary accents (kept in the brand family, not M3 baseline).
    secondary = Brand,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EFEA),
    onSecondaryContainer = Brand,
    // surfaceContainer ramp — read by NavigationBar/Rail + DropdownMenu; mapped to the green surfaces.
    surfaceContainerLowest = LightTokens.surface,
    surfaceContainerLow = LightTokens.background,
    surfaceContainer = LightTokens.surfaceVariant,
    surfaceContainerHigh = Color(0xFFECF1EE),
    surfaceContainerHighest = LightTokens.outlineVariant,
    error = LightTokens.overdue,
    onError = Color.White,
)

internal val DarkColorScheme = darkColorScheme(
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
    // Selected nav indicator / secondary accents (kept in the brand family, not M3 baseline).
    secondary = BrandMint,
    onSecondary = DarkTokens.onPrimary,
    secondaryContainer = Color(0xFF10301F),
    onSecondaryContainer = BrandMint,
    // surfaceContainer ramp — read by NavigationBar/Rail + DropdownMenu; mapped to the green surfaces.
    surfaceContainerLowest = DarkTokens.background,
    surfaceContainerLow = DarkTokens.surface,
    surfaceContainer = DarkTokens.surfaceVariant,
    surfaceContainerHigh = Color(0xFF16301F),
    surfaceContainerHighest = DarkTokens.outlineVariant,
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
            val activity = view.context.findActivity() ?: return@SideEffect
            WindowCompat.getInsetsController(activity.window, view)
                .isAppearanceLightStatusBars = !darkTheme
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
