package com.example.roadmap.ui.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roadmap.data.ThemeMode
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.relation.RoadmapCard
import com.example.roadmap.domain.isOverdue
import com.example.roadmap.domain.progressFraction
import com.example.roadmap.ui.components.DeadlineChip
import com.example.roadmap.ui.components.DeadlineState
import com.example.roadmap.ui.components.EmptyState
import com.example.roadmap.ui.components.GradientTile
import com.example.roadmap.ui.components.MetaChip
import com.example.roadmap.ui.components.PrimaryButton
import com.example.roadmap.ui.components.RingProgress
import com.example.roadmap.ui.components.RingSize
import com.example.roadmap.ui.components.RoadmapFab
import com.example.roadmap.ui.components.SecondaryButton
import com.example.roadmap.ui.components.SegmentedControl
import com.example.roadmap.ui.format.formatDeadline
import com.example.roadmap.ui.theme.RoadmapHue
import com.example.roadmap.ui.theme.RoadmapTheme
import com.example.roadmap.ui.imports.ImportDialog
import com.example.roadmap.ui.theme.hueForId
import java.time.LocalDate

@Composable
fun RoadmapListScreen(
    state: RoadmapListUiState,
    onScopeChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenRoadmap: (Long) -> Unit,
    onCreate: () -> Unit,
    onImport: () -> Unit,
    themeMode: ThemeMode = ThemeMode.System,
    onSetThemeMode: (ThemeMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = { RoadmapFab("New", onCreate) },
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Roadmaps", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${state.cards.size} ${if (state.archived) "archived" else "active"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = RoadmapTheme.colors.muted,
                    )
                }
                ThemeMenu(themeMode, onSetThemeMode)
                IconButton(onClick = onImport) {
                    Icon(Icons.Rounded.Download, contentDescription = "Import")
                }
            }

            SegmentedControl(
                options = listOf("Active", "Archive"),
                selectedIndex = if (state.archived) 1 else 0,
                onSelect = { onScopeChange(it == 1) },
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            )

            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                placeholder = { Text("Search roadmaps & milestones…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )

            when {
                state.cards.isEmpty() && state.query.isNotBlank() ->
                    EmptyState(
                        Icons.Rounded.Search,
                        "No matches",
                        "No roadmaps match “${state.query}”. Try another term or clear the search.",
                        RoadmapHue.Cyan,
                        Modifier.fillMaxSize(),
                    )

                state.cards.isEmpty() ->
                    EmptyState(
                        Icons.Outlined.Map,
                        if (state.archived) "Nothing archived" else "No roadmaps yet",
                        if (state.archived) "Roadmaps you archive will appear here."
                        else "Create your first roadmap, or import one from JSON.",
                        RoadmapHue.Emerald,
                        Modifier.fillMaxSize(),
                        actions = if (state.archived) null else ({
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PrimaryButton("Create roadmap", onCreate)
                                SecondaryButton("Import", onImport)
                            }
                        }),
                    )

                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 18.dp, end = 18.dp, top = 8.dp, bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    items(state.cards, key = { it.roadmap.id }) { card ->
                        RoadmapCardItem(card, onClick = { onOpenRoadmap(card.roadmap.id) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoadmapCardItem(card: RoadmapCard, onClick: () -> Unit) {
    val r = card.roadmap
    val complete = card.totalSteps > 0 && card.completedSteps == card.totalSteps
    val overdue = isOverdue(r.deadline, complete, LocalDate.now())
    val progress = progressFraction(card.completedSteps, card.totalSteps)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.Top,
        ) {
            GradientTile(r.title.firstOrNull() ?: '?', hueForId(r.id))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    r.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!r.description.isNullOrBlank()) {
                    Text(
                        r.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoadmapTheme.colors.muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Row(
                    Modifier.padding(top = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetaChip("${card.milestoneCount} milestones")
                    val deadlineText = formatDeadline(r.deadline)
                    if (deadlineText != null) {
                        DeadlineChip(
                            if (overdue) "Overdue · $deadlineText" else deadlineText,
                            if (overdue) DeadlineState.Overdue
                            else if (complete) DeadlineState.Done
                            else DeadlineState.Normal,
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            RingProgress(progress, size = RingSize.Medium)
        }
    }
}

// ---- previews ----

private fun sampleCard(
    id: Long,
    title: String,
    desc: String?,
    ms: Int,
    total: Int,
    done: Int,
    deadline: String?,
) = RoadmapCard(RoadmapEntity(id = id, title = title, description = desc, deadline = deadline), ms, total, done)

@Preview(name = "List Light")
@Composable
private fun ListPreviewLight() = RoadmapTheme(darkTheme = false) {
    RoadmapListScreen(
        RoadmapListUiState(
            cards = listOf(
                sampleCard(1, "Learn Kotlin & Compose", "From basics to a Material 3 app.", 6, 24, 15, "2026-09-30"),
                sampleCard(2, "Run a Half-Marathon", "12-week build-up.", 4, 12, 3, "2026-07-12"),
            ),
        ),
        {}, {}, {}, {}, {},
    )
}

@Preview(name = "List Dark")
@Composable
private fun ListPreviewDark() = RoadmapTheme(darkTheme = true) {
    RoadmapListScreen(RoadmapListUiState(cards = emptyList()), {}, {}, {}, {}, {})
}

// ---- Task 5: dialog + stateful route ----

@Composable
private fun ThemeMenu(themeMode: ThemeMode, onSetThemeMode: (ThemeMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                when (themeMode) {
                    ThemeMode.Light -> Icons.Rounded.LightMode
                    ThemeMode.Dark -> Icons.Rounded.DarkMode
                    ThemeMode.System -> Icons.Rounded.BrightnessAuto
                },
                contentDescription = "Theme",
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ThemeMode.entries.forEach { m ->
                DropdownMenuItem(
                    text = { Text(m.name) },
                    onClick = { onSetThemeMode(m); open = false },
                    leadingIcon = {
                        if (m == themeMode) Icon(Icons.Rounded.Check, contentDescription = null)
                    },
                )
            }
        }
    }
}

@Composable
fun NewRoadmapDialog(onDismiss: () -> Unit, onConfirm: (String, String?, String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                onConfirm(title.trim(), description.trim().ifBlank { null }, deadline.trim().ifBlank { null })
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("New roadmap") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    title, { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    description, { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    deadline, { deadline = it },
                    label = { Text("Deadline YYYY-MM-DD (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

/** Stateful host: owns the VM + dialog visibility; used by MainActivity. */
@Composable
fun RoadmapListRoute(
    viewModel: RoadmapListViewModel,
    onOpenRoadmap: (Long) -> Unit,
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    RoadmapListScreen(
        state = state,
        onScopeChange = viewModel::setArchived,
        onQueryChange = viewModel::setQuery,
        onOpenRoadmap = onOpenRoadmap,
        onCreate = { showNew = true },
        onImport = { showImport = true },
        themeMode = themeMode,
        onSetThemeMode = onSetThemeMode,
        modifier = modifier,
    )
    if (showNew) {
        NewRoadmapDialog(
            onDismiss = { showNew = false },
            onConfirm = { t, d, dl -> viewModel.createRoadmap(t, d, dl); showNew = false },
        )
    }
    if (showImport) {
        ImportDialog(
            onDismiss = { showImport = false },
            onImport = { draft -> viewModel.importRoadmap(draft); showImport = false },
        )
    }
}
