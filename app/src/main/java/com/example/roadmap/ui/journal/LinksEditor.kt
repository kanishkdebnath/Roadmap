package com.example.roadmap.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roadmap.data.LinkDraft
import com.example.roadmap.ui.theme.RoadmapTheme

@Composable
fun LinksEditor(
    links: List<LinkDraft>,
    onChange: (List<LinkDraft>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val anyInvalid = links.any { it.url.isNotBlank() && !it.url.startsWith("http://") && !it.url.startsWith("https://") }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        links.forEachIndexed { i, l ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        l.url,
                        { newUrl -> onChange(links.toMutableList().also { it[i] = l.copy(url = newUrl) }) },
                        placeholder = { Text("https://…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        l.label ?: "",
                        { newLabel -> onChange(links.toMutableList().also { it[i] = l.copy(label = newLabel.ifBlank { null }) }) },
                        placeholder = { Text("Label (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                IconButton(onClick = { onChange(links.toMutableList().also { it.removeAt(i) }) }) {
                    Icon(Icons.Rounded.Close, "Remove link")
                }
            }
        }
        if (anyInvalid) {
            Text(
                "Links must start with http:// or https://",
                style = MaterialTheme.typography.bodySmall,
                color = RoadmapTheme.colors.overdue,
            )
        }
        if (links.size < 10) {
            Text(
                "+ Add link",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp).clickableText { onChange(links + LinkDraft("")) },
            )
        }
    }
}
