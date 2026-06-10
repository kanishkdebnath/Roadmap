package com.example.roadmap.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadmap.domain.journalMonthGrid
import com.example.roadmap.ui.theme.RoadmapTheme
import com.example.roadmap.ui.theme.moodTint
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MonthLabel = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val Weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
private val LegendLabels = listOf(1 to "Rough", 2 to "Low", 3 to "Okay", 4 to "Good", 5 to "Great")

@Composable
fun MonthHeatmapScreen(
    state: JournalMonthUiState,
    today: LocalDate,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cells = remember(state.month) { journalMonthGrid(state.month) }
    Scaffold(modifier, containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text("Journal", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onPrevMonth) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month") }
                Text(
                    state.month.format(MonthLabel),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onNextMonth) { Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Weekdays.forEach { d ->
                    Text(
                        d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = RoadmapTheme.colors.faint,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { date ->
                        DayCell(
                            date = date,
                            mood = date?.let { state.moodByDate[it] },
                            isToday = date == today,
                            onOpenDay = onOpenDay,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(10.dp))
            Legend()
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    mood: Int?,
    isToday: Boolean,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier,
) {
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        if (date != null) {
            val bg = if (mood != null) RoadmapTheme.colors.moodTint(mood) else MaterialTheme.colorScheme.surface
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .then(
                        if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        else Modifier,
                    )
                    .clickable { onOpenDay(date) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mood != null) MaterialTheme.colorScheme.onSurface else RoadmapTheme.colors.faint,
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LegendLabels.forEach { (scale, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(RoadmapTheme.colors.moodTint(scale)),
                )
                Spacer(Modifier.size(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = RoadmapTheme.colors.muted)
            }
        }
    }
}

private fun sampleState() = JournalMonthUiState(
    month = YearMonth.of(2026, 6),
    moodByDate = mapOf(
        LocalDate.of(2026, 6, 2) to 3, LocalDate.of(2026, 6, 3) to 5,
        LocalDate.of(2026, 6, 4) to 4, LocalDate.of(2026, 6, 6) to 1,
        LocalDate.of(2026, 6, 8) to 2, LocalDate.of(2026, 6, 12) to 5,
    ),
)

@Preview(name = "Heatmap (light)")
@Composable
private fun HeatmapPreviewLight() = RoadmapTheme(darkTheme = false) {
    MonthHeatmapScreen(sampleState(), LocalDate.of(2026, 6, 9), {}, {}, {})
}

@Preview(name = "Heatmap (dark)")
@Composable
private fun HeatmapPreviewDark() = RoadmapTheme(darkTheme = true) {
    MonthHeatmapScreen(sampleState(), LocalDate.of(2026, 6, 9), {}, {}, {})
}
