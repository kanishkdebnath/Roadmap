package com.example.roadmap.ui.imports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roadmap.data.ImportPreview
import com.example.roadmap.data.RoadmapDraft
import com.example.roadmap.data.buildImportPrompt
import com.example.roadmap.data.previewImport
import com.example.roadmap.ui.components.PrimaryButton
import com.example.roadmap.ui.components.SecondaryButton
import com.example.roadmap.ui.theme.RoadmapHue
import com.example.roadmap.ui.theme.RoadmapTheme

@Composable
fun ImportDialog(onDismiss: () -> Unit, onImport: (RoadmapDraft) -> Unit) {
    var goal by remember { mutableStateOf("") }
    var paste by remember { mutableStateOf("") }
    val prompt = remember(goal) { buildImportPrompt(goal) }
    val preview = remember(paste) { previewImport(paste) }
    val clipboard = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                            .background(Brush.linearGradient(RoadmapHue.Cyan.colors(RoadmapTheme.colors.isDark))),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White) }
                    Column(Modifier.weight(1f)) {
                        Text("Import roadmap", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Generate JSON with any LLM, then paste it back. The app makes no network calls.",
                            style = MaterialTheme.typography.bodySmall, color = RoadmapTheme.colors.muted,
                        )
                    }
                }

                // Your goal
                OutlinedTextField(
                    goal, { goal = it },
                    label = { Text("Your goal") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Panel 1 — prompt
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("1 · Prompt template", style = MaterialTheme.typography.labelMedium,
                        color = RoadmapTheme.colors.muted, modifier = Modifier.weight(1f))
                    TextButton(onClick = { clipboard.setText(AnnotatedString(prompt)) }) {
                        Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp)); Text("Copy")
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    SelectionContainer {
                        Text(
                            prompt,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = RoadmapTheme.colors.muted,
                            modifier = Modifier.heightIn(max = 170.dp).verticalScroll(rememberScrollState()).padding(12.dp),
                        )
                    }
                }

                // Panel 2 — paste
                Text("2 · Paste JSON", style = MaterialTheme.typography.labelMedium, color = RoadmapTheme.colors.muted)
                OutlinedTextField(
                    paste, { paste = it },
                    placeholder = { Text("Paste the JSON here") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                )

                // Live note
                when (val p = preview) {
                    is ImportPreview.Valid -> NoteRow(
                        Icons.Rounded.CheckCircle, RoadmapTheme.colors.done,
                        "Valid · 1 roadmap, ${p.milestones} milestone(s), ${p.steps} step(s) — imported atomically.",
                    )
                    is ImportPreview.Invalid -> NoteRow(Icons.Rounded.ErrorOutline, RoadmapTheme.colors.overdue, p.reason)
                    ImportPreview.Empty -> Unit
                }

                // Footer
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Cancel", onDismiss, Modifier.weight(1f))
                    PrimaryButton(
                        "Import",
                        onClick = { (preview as? ImportPreview.Valid)?.let { onImport(it.draft) } },
                        modifier = Modifier.weight(1f),
                        enabled = preview is ImportPreview.Valid,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteRow(icon: ImageVector, color: Color, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ImportDialogPreview() {
    RoadmapTheme { ImportDialog(onDismiss = {}, onImport = {}) }
}
