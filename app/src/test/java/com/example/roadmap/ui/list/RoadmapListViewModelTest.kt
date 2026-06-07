package com.example.roadmap.ui.list

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.LinkDraft
import com.example.roadmap.data.MilestoneDraft
import com.example.roadmap.data.RoadmapDatabase
import com.example.roadmap.data.RoadmapDraft
import com.example.roadmap.data.RoomRoadmapRepository
import com.example.roadmap.data.StepDraft
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoadmapListViewModelTest {
    private lateinit var db: RoadmapDatabase
    private lateinit var vm: RoadmapListViewModel
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
        var t = 1L
        vm = RoadmapListViewModel(RoomRoadmapRepository(db) { t++ })
    }
    @After fun teardown() { db.close(); Dispatchers.resetMain() }

    @Test fun cards_reflect_repository_and_query_filters() = runTest(dispatcher) {
        vm.createRoadmap("Learn Kotlin", null, null)
        vm.createRoadmap("Run marathon", null, null)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.first { it.cards.size == 2 }.cards.size)

        vm.setQuery("kotlin")
        advanceUntilIdle()
        assertEquals(listOf("Learn Kotlin"),
            vm.uiState.first { it.query == "kotlin" }.cards.map { it.roadmap.title })
    }

    @Test fun archived_scope_switch() = runTest(dispatcher) {
        vm.createRoadmap("Active", null, null)
        advanceUntilIdle()
        vm.setArchived(true)
        advanceUntilIdle()
        assertEquals(0, vm.uiState.first { it.archived }.cards.size)
    }

    @Test fun import_persists_full_tree_into_active() = runTest(dispatcher) {
        val draft = RoadmapDraft(
            title = "Imported Goal",
            milestones = listOf(
                MilestoneDraft("M1", steps = listOf(StepDraft("s1"), StepDraft("s2", completed = true))),
                MilestoneDraft("M2", steps = listOf(StepDraft("s3", links = listOf(LinkDraft("https://x.com", "X"))))),
            ),
        )
        vm.importRoadmap(draft)
        advanceUntilIdle()
        val card = vm.uiState.first { it.cards.size == 1 }.cards.single()
        assertEquals("Imported Goal", card.roadmap.title)
        assertEquals(2, card.milestoneCount)
        assertEquals(3, card.totalSteps)
        assertEquals(1, card.completedSteps)
    }
}
