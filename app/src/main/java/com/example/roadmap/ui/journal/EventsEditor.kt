package com.example.roadmap.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roadmap.data.journal.EventDraft
import com.example.roadmap.ui.theme.RoadmapTheme

@Composable
fun EventsEditor(
    events: List<EventDraft>,
    onChange: (List<EventDraft>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        events.forEachIndexed { i, e ->
            EventRow(
                event = e,
                onEvent = { updated -> onChange(events.toMutableList().also { it[i] = updated }) },
                onRemove = { onChange(events.toMutableList().also { it.removeAt(i) }) },
            )
        }
        if (events.size < 20) {
            Text(
                "+ Add event",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickableText { onChange(events + EventDraft(text = "")) },
            )
        }
    }
}

@Composable
private fun EventRow(event: EventDraft, onEvent: (EventDraft) -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        IconButton(onClick = { onEvent(event.copy(important = !event.important)) }) {
            if (event.important) {
                Icon(Icons.Rounded.Star, "Unmark important", tint = RoadmapTheme.colors.amber)
            } else {
                Icon(Icons.Rounded.StarBorder, "Mark important", tint = RoadmapTheme.colors.faint)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                event.text,
                { onEvent(event.copy(text = it.take(500))) },
                placeholder = { Text("What happened?") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                event.time ?: "",
                { onEvent(event.copy(time = it.take(20).ifBlank { null })) },
                placeholder = { Text("Time (optional), e.g. 10am") },
                singleLine = true,
                modifier = Modifier.width(200.dp),
            )
        }
        IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, "Remove event") }
    }
}
