package com.example.roadmap.domain

import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity
import com.example.roadmap.data.relation.MilestoneWithSteps
import com.example.roadmap.data.relation.RoadmapWithChildren
import com.example.roadmap.data.relation.StepWithLinks
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressTest {
    private fun step(completed: Boolean) =
        StepWithLinks(StepEntity(milestoneId = 1, title = "s", completed = completed, position = 0), emptyList())

    private fun milestone(vararg completed: Boolean) =
        MilestoneWithSteps(MilestoneEntity(roadmapId = 1, title = "m", position = 0), completed.map { step(it) })

    @Test fun fraction_guards_divide_by_zero() {
        assertEquals(0f, progressFraction(0, 0), 0.0001f)
    }

    @Test fun fraction_is_completed_over_total() {
        assertEquals(0.5f, progressFraction(1, 2), 0.0001f)
        assertEquals(1f, progressFraction(3, 3), 0.0001f)
    }

    @Test fun milestone_progress_counts_its_steps() {
        val m = milestone(true, false, true, false)   // 2 of 4
        assertEquals(2, m.completedSteps())
        assertEquals(4, m.totalSteps())
        assertEquals(0.5f, m.progress(), 0.0001f)
    }

    @Test fun roadmap_progress_aggregates_across_milestones() {
        val roadmap = RoadmapWithChildren(
            RoadmapEntity(title = "r"),
            listOf(milestone(true, true), milestone(false, false, true)),   // 2/2 + 1/3 = 3/5
        )
        assertEquals(3, roadmap.completedSteps())
        assertEquals(5, roadmap.totalSteps())
        assertEquals(0.6f, roadmap.progress(), 0.0001f)
    }

    @Test fun empty_roadmap_is_zero_progress() {
        val roadmap = RoadmapWithChildren(RoadmapEntity(title = "r"), emptyList())
        assertEquals(0f, roadmap.progress(), 0.0001f)
    }
}
