package com.example.roadmap.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/** Bespoke tokens that Material 3's ColorScheme has no slot for. */
@Immutable
data class RoadmapColors(
    val ringTrack: Color,
    val done: Color,
    val doneContainer: Color,
    val overdue: Color,
    val overdueContainer: Color,
    val muted: Color,
    val faint: Color,
    /** Fill for primary actions: solid brand in light, mint→cyan gradient in dark. */
    val primaryBrush: Brush,
    val isDark: Boolean,
)

internal val LightRoadmapColors = RoadmapColors(
    ringTrack = LightTokens.ringTrack,
    done = LightTokens.done,
    doneContainer = LightTokens.doneContainer,
    overdue = LightTokens.overdue,
    overdueContainer = LightTokens.overdueContainer,
    muted = LightTokens.muted,
    faint = LightTokens.faint,
    primaryBrush = SolidColor(Brand),
    isDark = false,
)

internal val DarkRoadmapColors = RoadmapColors(
    ringTrack = DarkTokens.ringTrack,
    done = DarkTokens.done,
    doneContainer = DarkTokens.doneContainer,
    overdue = DarkTokens.overdue,
    overdueContainer = DarkTokens.overdueContainer,
    muted = DarkTokens.muted,
    faint = DarkTokens.faint,
    primaryBrush = Brush.linearGradient(listOf(BrandMint, BrandCyan)),
    isDark = true,
)

val LocalRoadmapColors = staticCompositionLocalOf { LightRoadmapColors }

/** Accessor: `RoadmapTheme.colors.overdue`. The `object` coexists with the `RoadmapTheme` composable. */
object RoadmapTheme {
    val colors: RoadmapColors
        @Composable @ReadOnlyComposable get() = LocalRoadmapColors.current
}
