package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.relation.sorted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomRoadmapRepositoryTest {
    private lateinit var db: RoadmapDatabase
    private lateinit var repo: RoomRoadmapRepository
    private var clock = 1000L

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
        repo = RoomRoadmapRepository(db) { clock }
    }
    @After fun teardown() = db.close()

    @Test fun completing_all_steps_sets_milestone_completedAt_and_unchecking_clears_it() = runTest {
        val rid = repo.createRoadmap("Goal")
        val mid = repo.addMilestone(rid, "M")
        val s1 = repo.addStep(mid, "s1")
        val s2 = repo.addStep(mid, "s2")

        repo.setStepCompleted(s1, true)
        assertNull(repo.observeRoadmap(rid).first()!!.milestones[0].milestone.completedAt)

        clock = 2000L
        repo.setStepCompleted(s2, true)
        assertEquals(2000L, repo.observeRoadmap(rid).first()!!.milestones[0].milestone.completedAt)

        repo.setStepCompleted(s1, false)
        assertNull(repo.observeRoadmap(rid).first()!!.milestones[0].milestone.completedAt)
    }

    @Test fun reorderMilestones_persists_positions() = runTest {
        val rid = repo.createRoadmap("Goal")
        val a = repo.addMilestone(rid, "A")
        val b = repo.addMilestone(rid, "B")
        val c = repo.addMilestone(rid, "C")
        repo.reorderMilestones(rid, listOf(c, a, b))
        val titles = repo.observeRoadmap(rid).first()!!.milestones.map { it.milestone.title }
        assertEquals(listOf("C", "A", "B"), titles)
    }

    @Test fun importRoadmap_inserts_full_tree_atomically_with_completion() = runTest {
        val id = repo.importRoadmap(
            RoadmapDraft(
                title = "Imported",
                milestones = listOf(
                    MilestoneDraft("M1", steps = listOf(StepDraft("done", completed = true, links = listOf(LinkDraft("https://a", "A"))))),
                    MilestoneDraft("M2", steps = listOf(StepDraft("todo"))),
                ),
            ),
        )
        val tree = repo.observeRoadmap(id).first()!!.sorted()
        assertEquals("Imported", tree.roadmap.title)
        assertEquals(2, tree.milestones.size)
        assertNotNull(tree.milestones[0].milestone.completedAt)   // all steps complete
        assertNull(tree.milestones[1].milestone.completedAt)
        assertEquals(listOf("https://a"), tree.milestones[0].steps[0].links.map { it.url })
    }

    @Test fun deleteRoadmap_removes_it() = runTest {
        val rid = repo.createRoadmap("X")
        repo.deleteRoadmap(rid)
        assertEquals(0, repo.observeRoadmaps(false).first().size)
    }

    @Test fun reorderMilestones_rejects_invalid_id_set() = runTest {
        val rid = repo.createRoadmap("Goal")
        val a = repo.addMilestone(rid, "A")
        repo.addMilestone(rid, "B")
        var threw = false
        try {
            repo.reorderMilestones(rid, listOf(a, 999L))   // 999 is not a milestone of this roadmap
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test fun setStepLinks_replaces_existing_links() = runTest {
        val rid = repo.createRoadmap("Goal")
        val mid = repo.addMilestone(rid, "M")
        val sid = repo.addStep(mid, "S")
        repo.setStepLinks(sid, listOf(LinkDraft("https://old", "Old")))
        repo.setStepLinks(sid, listOf(LinkDraft("https://new1"), LinkDraft("https://new2")))
        val links = repo.observeRoadmap(rid).first()!!.sorted()
            .milestones[0].steps[0].links.map { it.url }
        assertEquals(listOf("https://new1", "https://new2"), links)
    }

    @Test fun updateStepTitle_changes_title_but_not_completion() = runTest {
        val rid = repo.createRoadmap("Goal")
        val mid = repo.addMilestone(rid, "M")
        val sid = repo.addStep(mid, "S")
        repo.setStepCompleted(sid, true)
        val before = repo.observeRoadmap(rid).first()!!.milestones[0].milestone.completedAt
        assertNotNull(before)

        repo.updateStepTitle(sid, "S renamed")

        val after = repo.observeRoadmap(rid).first()!!
        assertEquals("S renamed", after.milestones[0].steps[0].step.title)
        assertEquals(before, after.milestones[0].milestone.completedAt)   // unchanged by a title edit
    }
}
