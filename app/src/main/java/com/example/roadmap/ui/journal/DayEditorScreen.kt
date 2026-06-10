package com.example.roadmap.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.roadmap.data.LinkDraft
import com.example.roadmap.data.journal.EventDraft
import com.example.roadmap.data.journal.MoodTag
import com.example.roadmap.data.journal.ReferenceDraft
import com.example.roadmap.ui.components.DangerButton
import com.example.roadmap.ui.theme.RoadmapTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayLabel = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditorScreen(
    state: DayEditorUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onScale: (Int) -> Unit,
    onToggleTag: (MoodTag) -> Unit,
    onSummary: (String) -> Unit,
    onEvents: (List<EventDraft>) -> Unit,
    onLinks: (List<LinkDraft>) -> Unit,
    onRemoveReference: (ReferenceDraft) -> Unit,
    onAddReference: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = state.draft
    Scaffold(
        modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(state.date.format(DayLabel)) },
                navigationIcon = {
                    IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = state.canSave) { Text("Save") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section("Mood", required = true) {
                MoodSelector(d.moodScale, d.moodTags, onScale, onToggleTag)
            }
            Section("Summary") {
                OutlinedTextField(
                    d.summary ?: "", { onSummary(it.take(500)) },
                    placeholder = { Text("One line about your day…") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${(d.summary ?: "").length} / 500",
                    style = MaterialTheme.typography.labelSmall,
                    color = RoadmapTheme.colors.faint,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Section("Events") { EventsEditor(d.events, onEvents) }
            Section("Links") { LinksEditor(d.links, onLinks) }
            Section("References") {
                ReferencesEditor(
                    references = d.references,
                    titleFor = { ref -> state.refTitles[RefKey(ref.roadmapId, ref.milestoneId)] },
                    onRemove = onRemoveReference,
                    onAddClick = onAddReference,
                )
            }
            DangerButton("Delete entry", onDelete, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Section(title: String, required: Boolean = false, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            buildString { append(title.uppercase()); if (required) append("  •  REQUIRED") },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (required) RoadmapTheme.colors.overdue else RoadmapTheme.colors.faint,
        )
        content()
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Day editor")
@Composable
private fun DayEditorPreview() = com.example.roadmap.ui.theme.RoadmapTheme {
    DayEditorScreen(
        state = DayEditorUiState(
            date = LocalDate.of(2026, 6, 9),
            draft = com.example.roadmap.data.journal.JournalDraft(
                date = LocalDate.of(2026, 6, 9),
                moodScale = 4,
                moodTags = listOf(MoodTag.Focused, MoodTag.Grateful),
                summary = "Shipped the theme toggle.",
                events = listOf(EventDraft("Standup", important = true, time = "10am"), EventDraft("Fixed a bug")),
                links = listOf(LinkDraft("https://developer.android.com", "Docs")),
                references = listOf(ReferenceDraft(com.example.roadmap.data.journal.RefType.Roadmap, 1)),
            ),
            refTitles = mapOf(RefKey(1, null) to "Learn Kotlin"),
            loaded = true,
            canSave = true,
        ),
        {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    )
}
