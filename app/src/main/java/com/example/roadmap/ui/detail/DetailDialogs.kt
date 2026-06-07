package com.example.roadmap.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roadmap.data.LinkDraft
import com.example.roadmap.data.relation.StepWithLinks

/** New roadmap / milestone, and Edit roadmap / milestone all share this title+description+deadline form. */
@Composable
fun EditEntityDialog(
    dialogTitle: String,
    initialTitle: String = "",
    initialDescription: String? = null,
    initialDeadline: String? = null,
    showDescription: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String?, deadline: String?) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription ?: "") }
    var deadline by remember { mutableStateOf(initialDeadline ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onConfirm(title.trim(), description.trim().ifBlank { null }, deadline.trim().ifBlank { null })
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    title, { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showDescription) {
                    OutlinedTextField(
                        description, { description = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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

/** Edit a step's title + its links; "Delete step" lives here too. */
@Composable
fun EditStepDialog(
    step: StepWithLinks,
    onDismiss: () -> Unit,
    onSave: (title: String, links: List<LinkDraft>) -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember { mutableStateOf(step.step.title) }
    val links = remember {
        androidx.compose.runtime.mutableStateListOf<Pair<String, String>>().apply {
            addAll(step.links.map { it.url to (it.label ?: "") })
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit step") },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && links.all {
                    it.first.isBlank() || it.first.startsWith("http://") || it.first.startsWith("https://")
                },
                onClick = {
                    val drafts = links
                        .filter { it.first.isNotBlank() }
                        .map { LinkDraft(it.first.trim(), it.second.trim().ifBlank { null }) }
                    onSave(title.trim(), drafts)
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    title, { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Links", style = MaterialTheme.typography.labelMedium)
                links.forEachIndexed { i, (url, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                url, { links[i] = it to label },
                                label = { Text("https://…") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                label, { links[i] = url to it },
                                label = { Text("Label (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        IconButton({ links.removeAt(i) }) { Icon(Icons.Rounded.Close, "Remove link") }
                    }
                }
                TextButton({ links.add("" to "") }) {
                    Icon(Icons.Rounded.Add, null)
                    Text("Add link")
                }
                Text(
                    "Links must start with http:// or https://",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton({ onConfirm() }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}
