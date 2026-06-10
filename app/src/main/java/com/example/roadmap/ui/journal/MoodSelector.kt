package com.example.roadmap.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadmap.data.journal.MoodTag
import com.example.roadmap.ui.theme.RoadmapTheme

private val Faces = listOf(1 to "😞", 2 to "😔", 3 to "😐", 4 to "🙂", 5 to "😄")
private val MoodLabels = mapOf(1 to "Rough", 2 to "Low", 3 to "Okay", 4 to "Good", 5 to "Great")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodSelector(
    scale: Int,
    tags: List<MoodTag>,
    onScale: (Int) -> Unit,
    onToggleTag: (MoodTag) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Faces.forEach { (s, emoji) ->
                val selected = s == scale
                Column(
                    Modifier
                        .weight(1f)
                        .aspectRatio(0.82f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) RoadmapTheme.colors.doneContainer else MaterialTheme.colorScheme.surfaceVariant)
                        .then(if (selected) Modifier.border(2.dp, RoadmapTheme.colors.done, RoundedCornerShape(11.dp)) else Modifier)
                        .clickable { onScale(s) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(emoji, fontSize = 20.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        MoodLabels.getValue(s),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = if (selected) RoadmapTheme.colors.done else RoadmapTheme.colors.faint,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MoodTag.entries.forEach { tag ->
                val selected = tag in tags
                val atCap = tags.size >= 3 && !selected
                TagChip(tag.name.lowercase(), selected = selected, enabled = !atCap) { onToggleTag(tag) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Up to 3 tags · ${tags.size} selected",
            style = MaterialTheme.typography.labelSmall,
            color = RoadmapTheme.colors.faint,
        )
    }
}

@Composable
private fun TagChip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(999.dp),
            )
            .alpha(if (enabled || selected) 1f else 0.4f)
            .clickable(enabled = enabled || selected) { onClick() }
            .padding(horizontal = 11.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else RoadmapTheme.colors.muted,
    )
}
