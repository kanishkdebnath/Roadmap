package com.example.roadmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadmap.ui.theme.RoadmapTheme
import com.example.roadmap.ui.theme.TabularNums

enum class DeadlineState { Normal, Overdue, Done }

@Composable
private fun ChipBase(
    text: String, icon: ImageVector?, bg: Color, fg: Color,
    modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
    tabular: Boolean = false,
) {
    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(13.dp))
        Text(
            text, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium
                .merge(if (tabular) TabularNums else androidx.compose.ui.text.TextStyle.Default),
        )
    }
}

@Composable
fun MetaChip(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier) =
    ChipBase(text, icon, MaterialTheme.colorScheme.surfaceVariant, RoadmapTheme.colors.muted, modifier, tabular = true)

@Composable
fun DeadlineChip(text: String, state: DeadlineState, modifier: Modifier = Modifier) {
    val c = RoadmapTheme.colors
    val (bg, fg) = when (state) {
        DeadlineState.Normal -> MaterialTheme.colorScheme.surfaceVariant to c.muted
        DeadlineState.Overdue -> c.overdueContainer to c.overdue
        DeadlineState.Done -> c.doneContainer to c.done
    }
    ChipBase(text, Icons.Outlined.CalendarMonth, bg, fg, modifier)
}

@Composable
fun LinkChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) =
    ChipBase(label, Icons.Outlined.Link, MaterialTheme.colorScheme.surfaceVariant,
        RoadmapTheme.colors.muted, modifier, onClick)

@Preview @Composable private fun ChipsPreview() {
    RoadmapTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            MetaChip("6 milestones")
            DeadlineChip("Sep 30", DeadlineState.Normal)
            DeadlineChip("May 15", DeadlineState.Overdue)
            DeadlineChip("Done", DeadlineState.Done)
        }
    }
}
