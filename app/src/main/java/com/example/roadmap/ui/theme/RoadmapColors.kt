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
class RoadmapColors(
    val ringTrack: Color,
    val done: Color,
    val doneContainer: Color,
    val overdue: Color,
    val overdueContainer: Color,
    val muted: Color,
    val faint: Color,
    val amber: Color,
    val amberContainer: Color,
    val neutral: Color,
    val neutralContainer: Color,
    val sky: Color,
    val skyContainer: Color,
    val primaryStart: Color,
    val primaryEnd: Color,
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
    amber = LightTokens.amber,
    amberContainer = LightTokens.amberContainer,
    neutral = LightTokens.neutral,
    neutralContainer = LightTokens.neutralContainer,
    sky = LightTokens.sky,
    skyContainer = LightTokens.skyContainer,
    primaryStart = Brand,
    primaryEnd = Brand,
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
    amber = DarkTokens.amber,
    amberContainer = DarkTokens.amberContainer,
    neutral = DarkTokens.neutral,
    neutralContainer = DarkTokens.neutralContainer,
    sky = DarkTokens.sky,
    skyContainer = DarkTokens.skyContainer,
    primaryStart = BrandMint,
    primaryEnd = BrandCyan,
    isDark = true,
)

/** Fill for primary actions: solid brand in light, mint→cyan gradient in dark. */
val RoadmapColors.primaryBrush: Brush
    get() = if (primaryStart == primaryEnd) SolidColor(primaryStart)
            else Brush.linearGradient(listOf(primaryStart, primaryEnd))

/** Mood scale 1..5 → accent token (spec §7). 5 = sky (brand collides with mood-4 emerald). */
fun RoadmapColors.moodAccent(scale: Int): Color = when (scale) {
    1 -> overdue
    2 -> amber
    3 -> neutral
    4 -> done
    5 -> sky
    else -> neutral
}

/** Mood scale 1..5 → soft container tint used by the heatmap cells. */
fun RoadmapColors.moodTint(scale: Int): Color = when (scale) {
    1 -> overdueContainer
    2 -> amberContainer
    3 -> neutralContainer
    4 -> doneContainer
    5 -> skyContainer
    else -> neutralContainer
}

internal val LocalRoadmapColors = staticCompositionLocalOf { LightRoadmapColors }

/** Accessor: `RoadmapTheme.colors.overdue`. The `object` coexists with the `RoadmapTheme` composable. */
object RoadmapTheme {
    val colors: RoadmapColors
        @Composable @ReadOnlyComposable get() = LocalRoadmapColors.current
}
