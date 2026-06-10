package com.example.roadmap.ui.journal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.RoadmapDatabase
import com.example.roadmap.data.journal.JournalDraft
import com.example.roadmap.data.journal.RoomJournalRepository
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
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JournalMonthViewModelTest {
    private lateinit var db: RoadmapDatabase
    private lateinit var repo: RoomJournalRepository
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
        var t = 1L
        repo = RoomJournalRepository(db) { t++ }
    }
    @After fun teardown() { db.close(); Dispatchers.resetMain() }

    @Test fun emits_mood_cells_for_the_initial_month() = runTest(dispatcher) {
        repo.saveDay(JournalDraft(date = LocalDate.of(2026, 6, 9), moodScale = 4))
        repo.saveDay(JournalDraft(date = LocalDate.of(2026, 5, 2), moodScale = 2))
        advanceUntilIdle()
        val vm = JournalMonthViewModel(repo, initialMonth = YearMonth.of(2026, 6))
        val s = vm.uiState.first { it.moodByDate.isNotEmpty() }
        assertEquals(YearMonth.of(2026, 6), s.month)
        assertEquals(mapOf(LocalDate.of(2026, 6, 9) to 4), s.moodByDate)
    }

    @Test fun shiftMonth_changes_month_and_reloads_cells() = runTest(dispatcher) {
        repo.saveDay(JournalDraft(date = LocalDate.of(2026, 5, 2), moodScale = 2))
        advanceUntilIdle()
        val vm = JournalMonthViewModel(repo, initialMonth = YearMonth.of(2026, 6))
        vm.uiState.first { it.month == YearMonth.of(2026, 6) }   // start collecting
        vm.shiftMonth(-1)
        advanceUntilIdle()
        val s = vm.uiState.first { it.month == YearMonth.of(2026, 5) }
        assertEquals(mapOf(LocalDate.of(2026, 5, 2) to 2), s.moodByDate)
    }
}
