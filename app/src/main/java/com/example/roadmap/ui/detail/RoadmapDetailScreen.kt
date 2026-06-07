package com.example.roadmap.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.relation.MilestoneWithSteps
import com.example.roadmap.data.relation.RoadmapWithChildren
import com.example.roadmap.data.relation.StepWithLinks
import com.example.roadmap.domain.completedSteps
import com.example.roadmap.domain.isOverdue
import com.example.roadmap.domain.progress
import com.example.roadmap.domain.totalSteps
import com.example.roadmap.ui.components.AddInline
import com.example.roadmap.ui.components.DangerButton
import com.example.roadmap.ui.components.DragGrip
import com.example.roadmap.ui.components.DeadlineChip
import com.example.roadmap.ui.components.DeadlineState
import com.example.roadmap.ui.components.LinkChip
import com.example.roadmap.ui.components.MetaChip
import com.example.roadmap.ui.components.RingProgress
import com.example.roadmap.ui.components.RingSize
import com.example.roadmap.ui.components.SecondaryButton
import com.example.roadmap.ui.components.StepCheckbox
import com.example.roadmap.ui.format.formatDeadline
import com.example.roadmap.ui.theme.RoadmapTheme
import java.time.LocalDate

class DetailCallbacks(
    val onBack: () -> Unit,
    val onEditRoadmap: () -> Unit,
    val onArchive: () -> Unit,
    val onDeleteRoadmap: () -> Unit,
    val onAddMilestone: () -> Unit,
    val onEditMilestone: (MilestoneEntity) -> Unit,
    val onDeleteMilestone: (Long) -> Unit,
    val onReorderMilestones: (orderedIds: List<Long>) -> Unit,
    val onAddStep: (milestoneId: Long) -> Unit,
    val onEditStep: (StepWithLinks) -> Unit,
    val onToggleStep: (id: Long, completed: Boolean) -> Unit,
    val onOpenLink: (url: String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadmapDetailScreen(tree: RoadmapWithChildren, cb: DetailCallbacks, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val r = tree.roadmap
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(cb.onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { inner ->
        // Local snapshot the drag mutates live; re-seeded whenever Room re-emits.
        var milestones by remember(tree.milestones) { mutableStateOf(tree.milestones) }
        val listState = rememberLazyListState()
        // Reorder by KEY, not raw index: this LazyColumn also holds non-milestone
        // items (header, "Milestones · N" label, AddInline), so onMove's from/to
        // indices are absolute LazyColumn positions, not indices into `milestones`.
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            milestones = milestones.toMutableList().apply {
                val fromIndex = indexOfFirst { it.milestone.id == from.key }
                val toIndex = indexOfFirst { it.milestone.id == to.key }
                if (fromIndex != -1 && toIndex != -1) add(toIndex, removeAt(fromIndex))
            }
        }
        LazyColumn(
            Modifier.padding(inner).fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.title, style = MaterialTheme.typography.headlineMedium)
                                if (!r.description.isNullOrBlank()) {
                                    Text(
                                        r.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = RoadmapTheme.colors.muted,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            RingProgress(tree.progress(), size = RingSize.Large)
                        }
                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetaChip("${tree.completedSteps()} / ${tree.totalSteps()} steps")
                            val dl = formatDeadline(r.deadline)
                            if (dl != null) {
                                val overdue = tree.isOverdue(today)
                                DeadlineChip(
                                    if (overdue) "Overdue · $dl" else "Due $dl",
                                    if (overdue) DeadlineState.Overdue else DeadlineState.Normal,
                                )
                            }
                        }
                        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecondaryButton("Edit", cb.onEditRoadmap, Modifier.weight(1f))
                            SecondaryButton(
                                if (r.archived) "Unarchive" else "Archive",
                                cb.onArchive,
                                Modifier.weight(1f),
                            )
                            DangerButton("Delete", cb.onDeleteRoadmap)
                        }
                    }
                }
            }
            item {
                Text(
                    "Milestones · ${tree.milestones.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoadmapTheme.colors.faint,
                    modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 4.dp),
                )
            }
            items(milestones, key = { it.milestone.id }) { m ->
                ReorderableItem(reorderState, key = m.milestone.id) {
                    val handle = Modifier.draggableHandle(
                        onDragStopped = { cb.onReorderMilestones(milestones.map { it.milestone.id }) },
                    )
                    MilestoneCard(
                        m, today, cb,
                        dragHandle = handle,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                    )
                }
            }
            item {
                AddInline(
                    "Add milestone",
                    cb.onAddMilestone,
                    Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MilestoneCard(
    m: MilestoneWithSteps,
    today: LocalDate,
    cb: DetailCallbacks,
    dragHandle: Modifier,
    modifier: Modifier,
) {
    val done = m.milestone.completedAt != null
    Surface(
        modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 6.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DragGrip(dragHandle.padding(end = 6.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        m.milestone.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (done) RoadmapTheme.colors.muted else MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${m.completedSteps()} / ${m.totalSteps()} steps",
                            style = MaterialTheme.typography.bodySmall,
                            color = RoadmapTheme.colors.muted,
                        )
                        val dl = formatDeadline(m.milestone.deadline)
                        if (dl != null && m.isOverdue(today)) {
                            DeadlineChip("Overdue · $dl", DeadlineState.Overdue)
                        }
                    }
                }
                RingProgress(m.progress(), size = RingSize.Small)
                MilestoneMenu(
                    onEdit = { cb.onEditMilestone(m.milestone) },
                    onDelete = { cb.onDeleteMilestone(m.milestone.id) },
                )
            }
            Column(Modifier.padding(top = 4.dp)) {
                m.steps.forEach { s -> StepRow(s, cb) }
                AddInline(
                    "Add step",
                    { cb.onAddStep(m.milestone.id) },
                    Modifier.padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun MilestoneMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }) { Icon(Icons.Rounded.MoreVert, "Milestone actions") }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Edit") }, onClick = { open = false; onEdit() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { open = false; onDelete() })
        }
    }
}

@Composable
private fun StepRow(s: StepWithLinks, cb: DetailCallbacks) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { cb.onEditStep(s) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        StepCheckbox(s.step.completed, { cb.onToggleStep(s.step.id, !s.step.completed) })
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                s.step.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (s.step.completed) RoadmapTheme.colors.faint else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (s.step.completed) TextDecoration.LineThrough else null,
                overflow = TextOverflow.Ellipsis,
            )
            if (s.links.isNotEmpty()) {
                Row(Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.links.forEach { l -> LinkChip(l.label ?: l.url, onClick = { cb.onOpenLink(l.url) }) }
                }
            }
        }
    }
}

// ---- preview helpers ----

internal fun sampleTree(): RoadmapWithChildren {
    val r = com.example.roadmap.data.entity.RoadmapEntity(
        id = 1,
        title = "Learn Kotlin & Compose",
        description = "From basics to a Material 3 app.",
        deadline = "2026-09-30",
    )
    fun step(id: Long, t: String, done: Boolean) = StepWithLinks(
        com.example.roadmap.data.entity.StepEntity(
            id = id, milestoneId = 1, title = t, completed = done, position = id.toInt(),
        ),
        if (id == 1L) listOf(
            com.example.roadmap.data.entity.LinkEntity(
                stepId = 1, url = "https://developer.android.com", position = 0,
            )
        ) else emptyList(),
    )
    val m1 = MilestoneWithSteps(
        com.example.roadmap.data.entity.MilestoneEntity(
            id = 1, roadmapId = 1, title = "Compose Basics", position = 0, completedAt = null,
        ),
        listOf(step(1, "Composables", true), step(2, "State", false)),
    )
    return RoadmapWithChildren(r, listOf(m1))
}

internal fun noopCallbacks() = DetailCallbacks(
    onBack = {},
    onEditRoadmap = {},
    onArchive = {},
    onDeleteRoadmap = {},
    onAddMilestone = {},
    onEditMilestone = {},
    onDeleteMilestone = {},
    onReorderMilestones = {},
    onAddStep = {},
    onEditStep = {},
    onToggleStep = { _, _ -> },
    onOpenLink = {},
)

// ---- preview ----

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun DetailPreview() {
    RoadmapTheme { RoadmapDetailScreen(sampleTree(), noopCallbacks()) }
}
