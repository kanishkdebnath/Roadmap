package com.example.roadmap.data

import com.example.roadmap.data.entity.LinkEntity
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity
import com.example.roadmap.data.relation.MilestoneWithSteps
import com.example.roadmap.data.relation.RoadmapWithChildren
import com.example.roadmap.data.relation.StepWithLinks
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportJsonTest {
    private fun sampleTree() = RoadmapWithChildren(
        RoadmapEntity(id = 1, title = "Learn Kotlin", description = "From basics", deadline = "2026-09-30"),
        listOf(
            MilestoneWithSteps(
                MilestoneEntity(id = 1, roadmapId = 1, title = "Basics", position = 0),
                listOf(
                    StepWithLinks(
                        StepEntity(id = 1, milestoneId = 1, title = "Composables", completed = true, position = 0),
                        listOf(LinkEntity(stepId = 1, url = "https://developer.android.com", label = "Docs", position = 0)),
                    ),
                    StepWithLinks(
                        StepEntity(id = 2, milestoneId = 1, title = "State", completed = false, position = 1),
                        emptyList(),
                    ),
                ),
            ),
        ),
    )

    @Test fun toDraft_maps_the_tree_to_the_import_schema() {
        val d = sampleTree().toDraft()
        assertEquals("Learn Kotlin", d.title)
        assertEquals("From basics", d.description)
        assertEquals("2026-09-30", d.deadline)
        assertEquals(listOf("Composables", "State"), d.milestones.single().steps.map { it.title })
        assertEquals(true, d.milestones.single().steps[0].completed)
        assertEquals(listOf("https://developer.android.com"), d.milestones.single().steps[0].links.map { it.url })
        assertEquals("Docs", d.milestones.single().steps[0].links.single().label)
    }

    @Test fun export_round_trips_through_the_import_parser() {
        val tree = sampleTree()
        val parsed = parseRoadmapJson(exportRoadmapJson(tree)).getOrThrow()
        assertEquals(tree.toDraft(), parsed)
    }
}
