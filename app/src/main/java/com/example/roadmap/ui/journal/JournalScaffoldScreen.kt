package com.example.roadmap.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadmap.domain.journalMonthGrid
import com.example.roadmap.ui.theme.RoadmapTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MonthLabel = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val Weekdays = listOf("S", "M", "T", "W", "T", "F", "S")

/**
 * Interim Journal landing: the current month's calendar chrome with no entries yet.
 * PR 3 replaces this with the live, mood-tinted, month-navigable heatmap.
 */
@Composable
fun JournalScaffoldScreen(modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    val month = remember { YearMonth.from(today) }
    val cells = remember(month) { journalMonthGrid(month) }

    Scaffold(modifier, containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(18.dp)) {
            Text("Journal", style = MaterialTheme.typography.headlineMedium)
            Text(
                month.format(MonthLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = RoadmapTheme.colors.muted,
            )
            Spacer(Modifier.height(16.dp))
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
                        DayCell(date, isToday = date == today, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate?, isToday: Boolean, modifier: Modifier) {
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        if (date != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .then(
                        if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = RoadmapTheme.colors.faint,
                )
            }
        }
    }
}

@Preview(name = "Journal scaffold (light)")
@Composable
private fun JournalScaffoldPreviewLight() = RoadmapTheme(darkTheme = false) { JournalScaffoldScreen() }

@Preview(name = "Journal scaffold (dark)")
@Composable
private fun JournalScaffoldPreviewDark() = RoadmapTheme(darkTheme = true) { JournalScaffoldScreen() }
