package com.example.roadmap.ui.journal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roadmap.data.journal.JournalRepository
import java.time.LocalDate

/** Stateful host: owns the month VM, renders the heatmap, bubbles day taps up to the app. */
@Composable
fun JournalRoute(
    repository: JournalRepository,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: JournalMonthViewModel = viewModel(factory = JournalMonthViewModelFactory(repository))
    val state by vm.uiState.collectAsState()
    MonthHeatmapScreen(
        state = state,
        today = LocalDate.now(),
        onPrevMonth = { vm.shiftMonth(-1) },
        onNextMonth = { vm.shiftMonth(1) },
        onOpenDay = onOpenDay,
        modifier = modifier,
    )
}
