package com.example.roadmap.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.roadmap.data.entity.LinkEntity
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity

data class StepWithLinks(
    @Embedded val step: StepEntity,
    @Relation(parentColumn = "id", entityColumn = "stepId") val links: List<LinkEntity>,
)

data class MilestoneWithSteps(
    @Embedded val milestone: MilestoneEntity,
    @Relation(entity = StepEntity::class, parentColumn = "id", entityColumn = "milestoneId")
    val steps: List<StepWithLinks>,
)

data class RoadmapWithChildren(
    @Embedded val roadmap: RoadmapEntity,
    @Relation(entity = MilestoneEntity::class, parentColumn = "id", entityColumn = "roadmapId")
    val milestones: List<MilestoneWithSteps>,
)

/** Returns a copy with every level ordered by its `position`. */
fun RoadmapWithChildren.sorted(): RoadmapWithChildren = copy(
    milestones = milestones.sortedBy { it.milestone.position }.map { m ->
        m.copy(steps = m.steps.sortedBy { it.step.position }.map { s ->
            s.copy(links = s.links.sortedBy { it.position })
        })
    },
)
