package com.example.roadmap.data

import androidx.room.withTransaction
import com.example.roadmap.data.entity.LinkEntity
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity
import com.example.roadmap.data.relation.RoadmapWithChildren
import com.example.roadmap.data.relation.sorted
import com.example.roadmap.domain.isValidReorder
import com.example.roadmap.domain.milestoneCompletedAt
import com.example.roadmap.domain.positionsFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRoadmapRepository(
    private val db: RoadmapDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : RoadmapRepository {

    private val roadmaps = db.roadmapDao()
    private val milestones = db.milestoneDao()
    private val steps = db.stepDao()
    private val links = db.linkDao()

    override fun observeRoadmaps(archived: Boolean): Flow<List<RoadmapEntity>> =
        roadmaps.observeByArchived(archived)

    override fun observeRoadmap(id: Long): Flow<RoadmapWithChildren?> =
        roadmaps.observeWithChildren(id).map { it?.sorted() }

    override suspend fun createRoadmap(title: String, description: String?, deadline: String?): Long {
        val t = now()
        return roadmaps.insert(RoadmapEntity(title = title, description = description, deadline = deadline, createdAt = t, updatedAt = t))
    }

    override suspend fun updateRoadmap(id: Long, title: String, description: String?, deadline: String?) {
        val existing = roadmaps.getById(id) ?: return
        roadmaps.update(existing.copy(title = title, description = description, deadline = deadline, updatedAt = now()))
    }

    override suspend fun setArchived(id: Long, archived: Boolean) = roadmaps.setArchived(id, archived, now())

    override suspend fun deleteRoadmap(id: Long) = roadmaps.deleteById(id)

    override suspend fun addMilestone(roadmapId: Long, title: String, description: String?, deadline: String?): Long {
        val t = now()
        val position = milestones.maxPosition(roadmapId) + 1
        return milestones.insert(MilestoneEntity(roadmapId = roadmapId, title = title, description = description, deadline = deadline, position = position, createdAt = t, updatedAt = t))
    }

    override suspend fun updateMilestone(id: Long, title: String, description: String?, deadline: String?) {
        val existing = milestones.getById(id) ?: return
        milestones.update(existing.copy(title = title, description = description, deadline = deadline, updatedAt = now()))
    }

    override suspend fun deleteMilestone(id: Long) = milestones.deleteById(id)

    override suspend fun reorderMilestones(roadmapId: Long, orderedIds: List<Long>) = db.withTransaction {
        val current = milestones.getByRoadmap(roadmapId).map { it.id }
        require(isValidReorder(current, orderedIds)) { "reorder ids must be a permutation of existing milestones" }
        positionsFor(orderedIds).forEach { (id, pos) -> milestones.updatePosition(id, pos) }
    }

    override suspend fun addStep(milestoneId: Long, title: String): Long = db.withTransaction {
        val t = now()
        val position = steps.maxPosition(milestoneId) + 1
        val id = steps.insert(StepEntity(milestoneId = milestoneId, title = title, position = position, createdAt = t, updatedAt = t))
        recompute(milestoneId)
        id
    }

    override suspend fun updateStepTitle(id: Long, title: String) {
        val existing = steps.getById(id) ?: return
        steps.update(existing.copy(title = title, updatedAt = now()))
    }

    override suspend fun setStepCompleted(id: Long, completed: Boolean) = db.withTransaction {
        val existing = steps.getById(id) ?: return@withTransaction
        val t = now()
        steps.update(existing.copy(completed = completed, completedAt = if (completed) t else null, updatedAt = t))
        recompute(existing.milestoneId)
    }

    override suspend fun deleteStep(id: Long) = db.withTransaction {
        val existing = steps.getById(id) ?: return@withTransaction
        steps.deleteById(id)
        recompute(existing.milestoneId)
    }

    override suspend fun reorderSteps(milestoneId: Long, orderedIds: List<Long>) = db.withTransaction {
        val current = steps.getByMilestone(milestoneId).map { it.id }
        require(isValidReorder(current, orderedIds)) { "reorder ids must be a permutation of existing steps" }
        positionsFor(orderedIds).forEach { (id, pos) -> steps.updatePosition(id, pos) }
    }

    override suspend fun setStepLinks(stepId: Long, links: List<LinkDraft>) = db.withTransaction {
        this.links.deleteByStep(stepId)
        this.links.insertAll(links.mapIndexed { i, l -> LinkEntity(stepId = stepId, url = l.url, label = l.label, position = i) })
    }

    override suspend fun importRoadmap(draft: RoadmapDraft): Long = db.withTransaction {
        val t = now()
        val rid = roadmaps.insert(RoadmapEntity(title = draft.title, description = draft.description, deadline = draft.deadline, createdAt = t, updatedAt = t))
        draft.milestones.forEachIndexed { mIndex, m ->
            val mid = milestones.insert(MilestoneEntity(roadmapId = rid, title = m.title, description = m.description, deadline = m.deadline, position = mIndex, createdAt = t, updatedAt = t))
            m.steps.forEachIndexed { sIndex, s ->
                val sid = steps.insert(StepEntity(milestoneId = mid, title = s.title, completed = s.completed, completedAt = if (s.completed) t else null, position = sIndex, createdAt = t, updatedAt = t))
                if (s.links.isNotEmpty()) {
                    links.insertAll(s.links.mapIndexed { lIndex, l -> LinkEntity(stepId = sid, url = l.url, label = l.label, position = lIndex) })
                }
            }
            recompute(mid)
        }
        rid
    }

    /** Recompute a milestone's derived completedAt from its current steps. */
    private suspend fun recompute(milestoneId: Long) {
        val t = now()
        val completions = steps.getByMilestone(milestoneId).map { it.completed }
        milestones.setCompletedAt(milestoneId, milestoneCompletedAt(completions, t), t)
    }
}
