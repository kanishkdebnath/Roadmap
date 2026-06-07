package com.example.roadmap.ui.detail

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.RoadmapDatabase
import com.example.roadmap.data.RoomRoadmapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoadmapDetailViewModelTest {
    private lateinit var db: RoadmapDatabase
    private lateinit var repo: RoomRoadmapRepository
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
        var t = 1L
        repo = RoomRoadmapRepository(db) { t++ }
    }
    @After fun teardown() { db.close(); Dispatchers.resetMain() }

    @Test fun observes_tree_and_toggles_step_completion() = runTest(dispatcher) {
        val rid = repo.createRoadmap("Goal")
        val mid = repo.addMilestone(rid, "M")
        val sid = repo.addStep(mid, "S")
        val vm = RoadmapDetailViewModel(repo, rid)

        advanceUntilIdle()
        val tree = vm.uiState.first { it.roadmap != null }.roadmap!!
        assertEquals("Goal", tree.roadmap.title)
        assertEquals(1, tree.milestones.size)
        assertEquals("S", tree.milestones[0].steps[0].step.title)

        vm.setStepCompleted(sid, true)
        advanceUntilIdle()
        val after = vm.uiState.first { it.roadmap?.milestones?.firstOrNull()?.steps?.firstOrNull()?.step?.completed == true }
        assertNotNull(after.roadmap!!.milestones[0].milestone.completedAt)  // milestone auto-completes (F6)
    }

    @Test fun reorders_milestones_and_persists_new_order() = runTest(dispatcher) {
        val rid = repo.createRoadmap("Goal")
        val m1 = repo.addMilestone(rid, "First")
        val m2 = repo.addMilestone(rid, "Second")
        val m3 = repo.addMilestone(rid, "Third")
        val vm = RoadmapDetailViewModel(repo, rid)
        advanceUntilIdle()

        val before = vm.uiState.first { it.roadmap?.milestones?.size == 3 }.roadmap!!
        assertEquals(listOf(m1, m2, m3), before.milestones.map { it.milestone.id })

        vm.reorderMilestones(listOf(m3, m1, m2))
        advanceUntilIdle()

        val after = vm.uiState.first {
            it.roadmap?.milestones?.map { m -> m.milestone.id } == listOf(m3, m1, m2)
        }.roadmap!!
        assertEquals(listOf(m3, m1, m2), after.milestones.map { it.milestone.id })
    }

    @Test fun reorders_steps_and_persists_new_order() = runTest(dispatcher) {
        val rid = repo.createRoadmap("Goal")
        val mid = repo.addMilestone(rid, "M")
        val s1 = repo.addStep(mid, "One")
        val s2 = repo.addStep(mid, "Two")
        val s3 = repo.addStep(mid, "Three")
        val vm = RoadmapDetailViewModel(repo, rid)
        advanceUntilIdle()

        val before = vm.uiState.first { it.roadmap?.milestones?.firstOrNull()?.steps?.size == 3 }.roadmap!!
        assertEquals(listOf(s1, s2, s3), before.milestones[0].steps.map { it.step.id })

        vm.reorderSteps(mid, listOf(s2, s3, s1))
        advanceUntilIdle()

        val after = vm.uiState.first {
            it.roadmap?.milestones?.firstOrNull()?.steps?.map { s -> s.step.id } == listOf(s2, s3, s1)
        }.roadmap!!
        assertEquals(listOf(s2, s3, s1), after.milestones[0].steps.map { it.step.id })
    }
}
