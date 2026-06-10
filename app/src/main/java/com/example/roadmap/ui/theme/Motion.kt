package com.example.roadmap.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Standard durations (ms) for the app's polish animations. */
object MotionDurations {
    const val FAST = 150
    const val MEDIUM = 300
}

/** Material-style standard easing. */
val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Pure: the system "Remove animations" a11y toggle sets ANIMATOR_DURATION_SCALE to 0. */
fun reduceMotion(animatorScale: Float): Boolean = animatorScale == 0f

/** True when the user has "Remove animations" enabled; read from system settings. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return reduceMotion(scale)
}
