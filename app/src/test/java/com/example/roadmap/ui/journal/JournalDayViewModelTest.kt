package com.example.roadmap.ui.journal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.LinkDraft
import com.example.roadmap.data.RoadmapDatabase
import com.example.roadmap.data.journal.EventDraft
import com.example.roadmap.data.journal.JournalDraft
import com.example.roadmap.data.journal.MoodTag
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JournalDayViewModelTest {
    private lateinit var db: RoadmapDatabase
    private lateinit var repo: RoomJournalRepository
    private val dispatcher = StandardTestDispatcher()
    private val date = LocalDate.of(2026, 6, 9)

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
        var t = 1L
        repo = RoomJournalRepository(db) { t++ }
    }
    @After fun teardown() { db.close(); Dispatchers.resetMain() }

    @Test fun loads_existing_day_into_draft() = runTest(dispatcher) {
        repo.saveDay(
            JournalDraft(date = date, moodScale = 4, moodTags = listOf(MoodTag.Focused),
                summary = "good", events = listOf(EventDraft("standup", important = true, time = "10am")))
        )
        advanceUntilIdle()
        val vm = JournalDayViewModel(repo, date)
        advanceUntilIdle()
        val s = vm.uiState.first { it.loaded }
        assertEquals(4, s.draft.moodScale)
        assertEquals(listOf(MoodTag.Focused), s.draft.moodTags)
        assertEquals("good", s.draft.summary)
        assertEquals(listOf("standup"), s.draft.events.map { it.text })
        assertFalse(s.canSave)
    }

    @Test fun editing_enables_save_and_persists() = runTest(dispatcher) {
        val vm = JournalDayViewModel(repo, date)
        advanceUntilIdle()
        assertFalse(vm.uiState.first { it.loaded }.canSave)
        vm.update { it.copy(moodScale = 4, summary = "new day") }
        assertTrue(vm.uiState.value.canSave)
        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()
        assertTrue(saved)
        val persisted = repo.observeDay(date).first()!!
        assertEquals(4, persisted.day.moodScale)
        assertEquals("new day", persisted.day.summary)
    }

    @Test fun invalid_link_url_blocks_save() = runTest(dispatcher) {
        val vm = JournalDayViewModel(repo, date)
        advanceUntilIdle()
        vm.update { it.copy(moodScale = 3, links = listOf(LinkDraft("not-a-url"))) }
        assertFalse(vm.uiState.value.canSave)
        vm.update { it.copy(links = listOf(LinkDraft("https://ok.com"))) }
        assertTrue(vm.uiState.value.canSave)
    }

    @Test fun delete_removes_the_day() = runTest(dispatcher) {
        repo.saveDay(JournalDraft(date = date, moodScale = 3))
        advanceUntilIdle()
        val vm = JournalDayViewModel(repo, date)
        advanceUntilIdle()
        vm.uiState.first { it.loaded }
        var deleted = false
        vm.delete { deleted = true }
        advanceUntilIdle()
        assertTrue(deleted)
        assertNull(repo.observeDay(date).first())
    }
}
