package com.example.roadmap.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roadmap.data.journal.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class JournalMonthUiState(
    val month: YearMonth,
    val moodByDate: Map<LocalDate, Int> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class JournalMonthViewModel(
    private val repository: JournalRepository,
    initialMonth: YearMonth = YearMonth.now(),
) : ViewModel() {
    private val month = MutableStateFlow(initialMonth)

    val uiState: StateFlow<JournalMonthUiState> =
        month.flatMapLatest { m ->
            repository.observeMonth(m).map { cells ->
                JournalMonthUiState(m, cells.associate { LocalDate.parse(it.date) to it.moodScale })
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalMonthUiState(initialMonth))

    fun shiftMonth(delta: Long) { month.value = month.value.plusMonths(delta) }
}

class JournalMonthViewModelFactory(private val repository: JournalRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = JournalMonthViewModel(repository) as T
}
