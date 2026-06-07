package com.example.roadmap.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roadmap.data.RoadmapDraft
import com.example.roadmap.data.RoadmapRepository
import com.example.roadmap.data.relation.RoadmapCard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoadmapListUiState(
    val archived: Boolean = false,
    val query: String = "",
    val cards: List<RoadmapCard> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class RoadmapListViewModel(private val repository: RoadmapRepository) : ViewModel() {
    private val archived = MutableStateFlow(false)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<RoadmapListUiState> =
        combine(archived, query) { a, q -> a to q }
            .flatMapLatest { (a, q) ->
                repository.observeRoadmapCards(a, q).map { RoadmapListUiState(a, q, it) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoadmapListUiState())

    fun setArchived(value: Boolean) { archived.value = value }
    fun setQuery(value: String) { query.value = value }
    fun createRoadmap(title: String, description: String?, deadline: String?) {
        viewModelScope.launch { repository.createRoadmap(title, description, deadline) }
    }

    fun importRoadmap(draft: RoadmapDraft) {
        viewModelScope.launch { repository.importRoadmap(draft) }
    }
}

class RoadmapListViewModelFactory(private val repository: RoadmapRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RoadmapListViewModel(repository) as T
}
