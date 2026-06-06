package com.example.roadmap.ui.list

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.RoadmapDatabase
import com.example.roadmap.data.RoomRoadmapRepository
import kotlinx.coroutines.Dispatchers
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
}
