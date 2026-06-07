package com.example.roadmap.data

import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.relation.RoadmapCard
import com.example.roadmap.data.relation.RoadmapWithChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** Plain models for atomic bulk import (Phase 7 maps validated JSON to these). */
@Serializable
data class LinkDraft(val url: String, val label: String? = null)
@Serializable
data class StepDraft(val title: String, val completed: Boolean = false, val links: List<LinkDraft> = emptyList())
@Serializable
data class MilestoneDraft(val title: String, val description: String? = null, val deadline: String? = null, val steps: List<StepDraft> = emptyList())
@Serializable
data class RoadmapDraft(val title: String, val description: String? = null, val deadline: String? = null, val milestones: List<MilestoneDraft> = emptyList())

interface RoadmapRepository {
    fun observeRoadmaps(archived: Boolean): Flow<List<RoadmapEntity>>
    fun observeRoadmap(id: Long): Flow<RoadmapWithChildren?>          // children sorted by position
    fun observeRoadmapCards(archived: Boolean, query: String): Flow<List<RoadmapCard>>

    suspend fun createRoadmap(title: String, description: String? = null, deadline: String? = null): Long
    suspend fun updateRoadmap(id: Long, title: String, description: String?, deadline: String?)
    suspend fun setArchived(id: Long, archived: Boolean)
    suspend fun deleteRoadmap(id: Long)

    suspend fun addMilestone(roadmapId: Long, title: String, description: String? = null, deadline: String? = null): Long
    suspend fun updateMilestone(id: Long, title: String, description: String?, deadline: String?)
    suspend fun deleteMilestone(id: Long)
    suspend fun reorderMilestones(roadmapId: Long, orderedIds: List<Long>)

    suspend fun addStep(milestoneId: Long, title: String): Long
    suspend fun updateStepTitle(id: Long, title: String)
    suspend fun setStepCompleted(id: Long, completed: Boolean)
    suspend fun deleteStep(id: Long)
    suspend fun reorderSteps(milestoneId: Long, orderedIds: List<Long>)
    suspend fun setStepLinks(stepId: Long, links: List<LinkDraft>)

    suspend fun importRoadmap(draft: RoadmapDraft): Long              // one atomic transaction
}
