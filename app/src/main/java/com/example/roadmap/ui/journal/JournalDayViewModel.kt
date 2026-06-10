package com.example.roadmap.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roadmap.data.LinkDraft
import com.example.roadmap.data.journal.EventDraft
import com.example.roadmap.data.journal.JournalDraft
import com.example.roadmap.data.journal.JournalRepository
import com.example.roadmap.data.journal.RefType
import com.example.roadmap.data.journal.relation.JournalDayWithChildren
import com.example.roadmap.data.journal.relation.RefTarget
import com.example.roadmap.data.journal.ReferenceDraft
import com.example.roadmap.domain.canSaveJournal
import com.example.roadmap.domain.normalized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Identifies a reference target for the resolved-title lookup. */
data class RefKey(val roadmapId: Long, val milestoneId: Long?)

data class DayEditorUiState(
    val date: LocalDate,
    val draft: JournalDraft,
    val refTitles: Map<RefKey, String?> = emptyMap(),  // null title => deleted target
    val loaded: Boolean = false,
    val canSave: Boolean = false,
)

class JournalDayViewModel(
    private val repository: JournalRepository,
    private val date: LocalDate,
) : ViewModel() {

    private var saved: JournalDraft = JournalDraft(date = date)
    private val _state = MutableStateFlow(DayEditorUiState(date, JournalDraft(date = date)))
    val uiState: StateFlow<DayEditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val day = repository.observeDay(date).first()
            val draft = day?.toDraft(date) ?: JournalDraft(date = date)
            val titles = if (day != null) {
                repository.observeResolvedReferences(day.day.id).first()
                    .associate { RefKey(it.reference.roadmapId, it.reference.milestoneId) to it.resolvedTitle }
            } else {
                emptyMap()
            }
            saved = draft
            _state.value = DayEditorUiState(date, draft, titles, loaded = true, canSave = false)
        }
    }

    fun update(block: (JournalDraft) -> JournalDraft) {
        val d = block(_state.value.draft)
        _state.value = _state.value.copy(draft = d, canSave = computeCanSave(d))
    }

    fun addReference(target: RefTarget) {
        val d = _state.value.draft
        val exists = d.references.any {
            it.type == target.type && it.roadmapId == target.roadmapId && it.milestoneId == target.milestoneId
        }
        if (d.references.size >= 10 || exists) return
        val nd = d.copy(references = d.references + ReferenceDraft(target.type, target.roadmapId, target.milestoneId))
        val titles = _state.value.refTitles + (RefKey(target.roadmapId, target.milestoneId) to target.title)
        _state.value = _state.value.copy(draft = nd, refTitles = titles, canSave = computeCanSave(nd))
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.saveDay(_state.value.draft.normalized())
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteDay(date)
            onDeleted()
        }
    }

    private fun computeCanSave(d: JournalDraft): Boolean =
        canSaveJournal(d, saved) && d.links.all { it.url.isBlank() || it.url.isHttpUrl() }
}

private fun String.isHttpUrl() = startsWith("http://") || startsWith("https://")

private fun JournalDayWithChildren.toDraft(date: LocalDate) = JournalDraft(
    date = date,
    moodScale = day.moodScale,
    moodTags = day.moodTags,
    summary = day.summary,
    events = events.map { EventDraft(it.text, it.important, it.time) },
    links = links.map { LinkDraft(it.url, it.label) },
    references = references.map { ReferenceDraft(it.type, it.roadmapId, it.milestoneId) },
)

class JournalDayViewModelFactory(
    private val repository: JournalRepository,
    private val date: LocalDate,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        JournalDayViewModel(repository, date) as T
}
