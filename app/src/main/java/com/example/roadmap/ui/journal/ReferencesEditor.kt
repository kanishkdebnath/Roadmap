package com.example.roadmap.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.roadmap.data.journal.JournalRepository
import com.example.roadmap.data.journal.RefType
import com.example.roadmap.data.journal.relation.RefTarget
import com.example.roadmap.data.journal.ReferenceDraft
import com.example.roadmap.ui.theme.RoadmapTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReferencesEditor(
    references: List<ReferenceDraft>,
    titleFor: (ReferenceDraft) -> String?,   // null => deleted target
    onRemove: (ReferenceDraft) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (references.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                references.forEach { ref ->
                    RefChip(
                        title = titleFor(ref),
                        isMilestone = ref.type == RefType.Milestone,
                        onRemove = { onRemove(ref) },
                    )
                }
            }
        }
        if (references.size < 10) {
            Text(
                "+ Add roadmap or milestone",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp).clickableText { onAddClick() },
            )
        }
    }
}

@Composable
private fun RefChip(title: String?, isMilestone: Boolean, onRemove: () -> Unit) {
    val deleted = title == null
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
            .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            if (isMilestone) Icons.Outlined.Flag else Icons.Outlined.Map,
            null,
            tint = RoadmapTheme.colors.muted,
        )
        Text(
            title ?: "(deleted)",
            style = MaterialTheme.typography.labelMedium,
            color = if (deleted) RoadmapTheme.colors.faint else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (deleted) TextDecoration.LineThrough else null,
        )
        Icon(
            Icons.Rounded.Close, "Remove reference",
            tint = RoadmapTheme.colors.faint,
            modifier = Modifier.clickable { onRemove() },
        )
    }
}

/** Searchable picker over the app's roadmaps + milestones. */
@Composable
fun ReferencePicker(
    repository: JournalRepository,
    onDismiss: () -> Unit,
    onPick: (RefTarget) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results by remember(query) { repository.searchReferenceTargets(query) }
        .collectAsState(initial = emptyList())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add reference") },
        confirmButton = {},
        dismissButton = { TextButton(onDismiss) { Text("Close") } },
        text = {
            Column {
                OutlinedTextField(
                    query, { query = it },
                    placeholder = { Text("Search roadmaps & milestones…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 320.dp).padding(top = 8.dp)) {
                    items(results) { t ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(t); onDismiss() }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                if (t.type == RefType.Milestone) Icons.Outlined.Flag else Icons.Outlined.Map,
                                null, tint = RoadmapTheme.colors.muted,
                            )
                            Column {
                                Text(t.title, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (t.type == RefType.Milestone) "Milestone" else "Roadmap",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RoadmapTheme.colors.faint,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
