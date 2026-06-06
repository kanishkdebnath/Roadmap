package com.example.roadmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.theme.RoadmapTheme

@Composable
fun StepCheckbox(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(7.dp)
    Row(
        modifier = modifier
            .size(22.dp)
            .clip(shape)
            .then(
                if (checked) Modifier.background(MaterialTheme.colorScheme.primary)
                else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, shape)
            )
            .clickable { onToggle() }
            .semantics { stateDescription = if (checked) "Completed" else "Not completed" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (checked) {
            Icon(Icons.Rounded.Check, null,
                tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun DragGrip(modifier: Modifier = Modifier) {
    Icon(Icons.Rounded.DragIndicator, contentDescription = "Reorder",
        tint = RoadmapTheme.colors.faint, modifier = modifier.size(18.dp))
}

@Composable
fun AddInline(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = RoadmapTheme.colors.muted
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRoundRect(
                    color = color,
                    style = Stroke(width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                )
            }
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(Icons.Rounded.Add, null, tint = color, modifier = Modifier.size(15.dp).padding(start = 11.dp))
        Text(text, color = color, style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(end = 11.dp, top = 10.dp, bottom = 10.dp))
    }
}

@Preview @Composable private fun StepPartsPreview() {
    RoadmapTheme {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            DragGrip()
            StepCheckbox(true, {})
            StepCheckbox(false, {})
        }
    }
}

@Preview @Composable private fun AddInlinePreview() {
    RoadmapTheme {
        AddInline("Add step", {}, Modifier.padding(16.dp))
    }
}
