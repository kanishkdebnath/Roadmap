package com.example.roadmap.domain

import com.example.roadmap.data.LinkDraft
import com.example.roadmap.data.MilestoneDraft
import com.example.roadmap.data.RoadmapDraft
import com.example.roadmap.data.StepDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportValidationTest {
    @Test fun minimal_valid_roadmap_passes() {
        val result = validateImport(RoadmapDraft(title = "Goal"))
        assertEquals(ImportValidation.Valid, result)
    }

    @Test fun full_valid_tree_passes() {
        val draft = RoadmapDraft(
            title = "Goal",
            milestones = listOf(
                MilestoneDraft("M", steps = listOf(
                    StepDraft("S", links = listOf(LinkDraft("https://a", "A"), LinkDraft("http://b"))),
                )),
            ),
        )
        assertEquals(ImportValidation.Valid, validateImport(draft))
    }

    @Test fun blank_roadmap_title_is_invalid() {
        assertTrue(validateImport(RoadmapDraft(title = "")) is ImportValidation.Invalid)
    }

    @Test fun overlong_title_is_invalid() {
        assertTrue(validateImport(RoadmapDraft(title = "x".repeat(201))) is ImportValidation.Invalid)
    }

    @Test fun non_http_link_is_invalid() {
        val draft = RoadmapDraft(
            title = "Goal",
            milestones = listOf(MilestoneDraft("M", steps = listOf(
                StepDraft("S", links = listOf(LinkDraft("ftp://x"))),
            ))),
        )
        assertTrue(validateImport(draft) is ImportValidation.Invalid)
    }

    @Test fun too_many_milestones_is_invalid() {
        val draft = RoadmapDraft(title = "Goal", milestones = List(101) { MilestoneDraft("M") })
        assertTrue(validateImport(draft) is ImportValidation.Invalid)
    }

    @Test fun too_many_links_is_invalid() {
        val draft = RoadmapDraft(
            title = "Goal",
            milestones = listOf(MilestoneDraft("M", steps = listOf(
                StepDraft("S", links = List(21) { LinkDraft("https://a") }),
            ))),
        )
        assertTrue(validateImport(draft) is ImportValidation.Invalid)
    }

    @Test fun too_many_steps_is_invalid() {
        val draft = RoadmapDraft(
            title = "Goal",
            milestones = listOf(MilestoneDraft("M", steps = List(201) { StepDraft("S") })),
        )
        assertTrue(validateImport(draft) is ImportValidation.Invalid)
    }

    @Test fun overlong_description_is_invalid() {
        assertTrue(validateImport(RoadmapDraft(title = "Goal", description = "x".repeat(2001))) is ImportValidation.Invalid)
    }

    @Test fun boundary_length_titles_are_valid() {
        assertEquals(ImportValidation.Valid, validateImport(RoadmapDraft(title = "x")))            // length 1
        assertEquals(ImportValidation.Valid, validateImport(RoadmapDraft(title = "x".repeat(200)))) // length 200
    }

    @Test fun blank_milestone_and_step_titles_are_invalid() {
        assertTrue(validateImport(RoadmapDraft(title = "Goal", milestones = listOf(MilestoneDraft("")))) is ImportValidation.Invalid)
        assertTrue(
            validateImport(
                RoadmapDraft(title = "Goal", milestones = listOf(MilestoneDraft("M", steps = listOf(StepDraft(""))))),
            ) is ImportValidation.Invalid,
        )
    }
}
