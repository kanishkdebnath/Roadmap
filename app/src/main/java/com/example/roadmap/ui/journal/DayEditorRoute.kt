package com.example.roadmap.ui.journal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roadmap.data.journal.JournalRepository
import com.example.roadmap.ui.detail.ConfirmDeleteDialog
import java.time.LocalDate

/** Stateful host: owns the day VM, the reference picker, and the delete-confirm dialog. */
@Composable
fun DayEditorRoute(
    repository: JournalRepository,
    date: LocalDate,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: JournalDayViewModel = viewModel(
        factory = JournalDayViewModelFactory(repository, date),
        key = "day-$date",
    )
    val state by vm.uiState.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    DayEditorScreen(
        state = state,
        onBack = onBack,
        onSave = { vm.save(onBack) },
        onDelete = { showDeleteConfirm = true },
        onScale = { s -> vm.update { it.copy(moodScale = s) } },
        onToggleTag = { tag ->
            vm.update {
                if (tag in it.moodTags) it.copy(moodTags = it.moodTags - tag)
                else if (it.moodTags.size < 3) it.copy(moodTags = it.moodTags + tag)
                else it
            }
        },
        onSummary = { s -> vm.update { it.copy(summary = s.ifBlank { null }) } },
        onEvents = { events -> vm.update { it.copy(events = events) } },
        onLinks = { links -> vm.update { it.copy(links = links) } },
        onRemoveReference = { ref -> vm.update { it.copy(references = it.references - ref) } },
        onAddReference = { showPicker = true },
        modifier = modifier,
    )

    if (showPicker) {
        ReferencePicker(
            repository = repository,
            onDismiss = { showPicker = false },
            onPick = { target -> vm.addReference(target) },
        )
    }
    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Delete entry?",
            message = "This journal entry will be permanently removed.",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = { showDeleteConfirm = false; vm.delete(onBack) },
        )
    }
}
