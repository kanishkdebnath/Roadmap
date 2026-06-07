package com.example.roadmap.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roadmap.data.LinkDraft
import com.example.roadmap.data.RoadmapRepository
import com.example.roadmap.data.relation.RoadmapWithChildren
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoadmapDetailUiState(val roadmap: RoadmapWithChildren? = null)

class RoadmapDetailViewModel(
    private val repository: RoadmapRepository,
    private val roadmapId: Long,
) : ViewModel() {

    val uiState: StateFlow<RoadmapDetailUiState> =
        repository.observeRoadmap(roadmapId)
            .map { RoadmapDetailUiState(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoadmapDetailUiState())

    // roadmap
    fun updateRoadmap(title: String, description: String?, deadline: String?) =
        launch { repository.updateRoadmap(roadmapId, title, description, deadline) }
    fun setArchived(archived: Boolean) = launch { repository.setArchived(roadmapId, archived) }
    fun deleteRoadmap() = launch { repository.deleteRoadmap(roadmapId) }

    // milestones
    fun addMilestone(title: String) = launch { repository.addMilestone(roadmapId, title) }
    fun updateMilestone(id: Long, title: String, description: String?, deadline: String?) =
        launch { repository.updateMilestone(id, title, description, deadline) }
    fun deleteMilestone(id: Long) = launch { repository.deleteMilestone(id) }

    // steps
    fun addStep(milestoneId: Long, title: String) = launch { repository.addStep(milestoneId, title) }
    fun updateStepTitle(id: Long, title: String) = launch { repository.updateStepTitle(id, title) }
    fun setStepCompleted(id: Long, completed: Boolean) = launch { repository.setStepCompleted(id, completed) }
    fun deleteStep(id: Long) = launch { repository.deleteStep(id) }
    fun setStepLinks(stepId: Long, links: List<LinkDraft>) = launch { repository.setStepLinks(stepId, links) }

    private inline fun launch(crossinline block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

class RoadmapDetailViewModelFactory(
    private val repository: RoadmapRepository,
    private val roadmapId: Long,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RoadmapDetailViewModel(repository, roadmapId) as T
}
