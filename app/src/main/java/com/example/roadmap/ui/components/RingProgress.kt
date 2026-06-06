package com.example.roadmap.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.theme.RoadmapTheme
import com.example.roadmap.ui.theme.TabularNums
import kotlin.math.roundToInt

// ---- pure math (JVM-testable) ----
private const val COMPLETE_THRESHOLD = 1f

fun progressSweep(progress: Float): Float = progress.coerceIn(0f, COMPLETE_THRESHOLD) * 360f
fun progressPercentLabel(progress: Float): String =
    (progress.coerceIn(0f, COMPLETE_THRESHOLD) * 100f).roundToInt().toString()
fun isRingComplete(progress: Float): Boolean = progress >= COMPLETE_THRESHOLD

enum class RingSize(val diameter: Dp, val stroke: Dp, val fontSize: Int) {
    Small(52.dp, 5.dp, 13), Medium(56.dp, 5.dp, 14), Large(104.dp, 9.dp, 27),
}

/** Uniform brand-colored conic-style progress ring. Checkmark at 100%. */
@Composable
fun RingProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: RingSize = RingSize.Medium,
) {
    val ringColor = MaterialTheme.colorScheme.primary
    val trackColor = RoadmapTheme.colors.ringTrack
    val complete = isRingComplete(progress)
    val pct = progressPercentLabel(progress)
    val sweep = progressSweep(progress)

    Box(
        modifier = modifier
            .size(size.diameter)
            .semantics {
                contentDescription = if (complete) "Complete" else "$pct percent complete"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size.diameter)) {
            val inset = size.stroke.toPx() / 2f
            val arcSize = Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
            val topLeft = Offset(inset, inset)
            // Track: full circle with butt caps so the seam at 0° stays flush.
            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(width = size.stroke.toPx()))
            // Progress: rounded caps; skip when empty so a zero-length arc can't render a dot.
            if (sweep > 0f) {
                drawArc(
                    ringColor, -90f, sweep, false, topLeft, arcSize,
                    style = Stroke(width = size.stroke.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        if (complete) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(size.diameter * 0.42f),
            )
        } else {
            Text(
                pct,
                style = MaterialTheme.typography.titleMedium
                    .merge(TabularNums)
                    .copy(fontSize = size.fontSizeSp(), fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun RingSize.fontSizeSp() =
    TextUnit(fontSize.toFloat(), TextUnitType.Sp)

@Preview(name = "Ring 62%")
@Composable
private fun RingPreview() {
    RoadmapTheme {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            RingProgress(0.62f, size = RingSize.Large)
        }
    }
}

@Preview(name = "Ring complete")
@Composable
private fun RingPreviewComplete() {
    RoadmapTheme {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            RingProgress(1f, size = RingSize.Large)
        }
    }
}

@Preview(name = "Ring empty")
@Composable
private fun RingPreviewEmpty() {
    RoadmapTheme {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            RingProgress(0f, size = RingSize.Large)
        }
    }
}
