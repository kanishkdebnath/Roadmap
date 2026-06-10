package com.example.roadmap.data

import com.example.roadmap.data.relation.RoadmapWithChildren
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val exportJson = Json { prettyPrint = true }

/** Map a (sorted) roadmap tree to the §8 import schema — the inverse of importRoadmap. */
fun RoadmapWithChildren.toDraft(): RoadmapDraft = RoadmapDraft(
    title = roadmap.title,
    description = roadmap.description,
    deadline = roadmap.deadline,
    milestones = milestones.map { m ->
        MilestoneDraft(
            title = m.milestone.title,
            description = m.milestone.description,
            deadline = m.milestone.deadline,
            steps = m.steps.map { s ->
                StepDraft(
                    title = s.step.title,
                    completed = s.step.completed,
                    links = s.links.map { LinkDraft(it.url, it.label) },
                )
            },
        )
    },
)

/** Pretty-printed JSON snapshot matching the import schema; round-trips with [parseRoadmapJson]. */
fun exportRoadmapJson(tree: RoadmapWithChildren): String = exportJson.encodeToString(tree.toDraft())
