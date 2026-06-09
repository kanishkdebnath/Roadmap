package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.journal.EventDraft
import com.example.roadmap.data.journal.JournalDraft
import com.example.roadmap.data.journal.MoodTag
import com.example.roadmap.data.journal.RefType
import com.example.roadmap.data.journal.ReferenceDraft
import com.example.roadmap.data.journal.RoomJournalRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomJournalRepositoryTest {
    private lateinit var db: RoadmapDatabase
    private lateinit var repo: RoomJournalRepository
    private var clock = 1000L
    private val date = LocalDate.of(2026, 6, 9)

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
        repo = RoomJournalRepository(db) { clock }
    }
    @After fun teardown() = db.close()

    @Test fun saveDay_inserts_day_with_sorted_children() = runTest {
        repo.saveDay(
            JournalDraft(
                date = date, moodScale = 4, moodTags = listOf(MoodTag.Focused),
                summary = "good day",
                events = listOf(EventDraft("standup", important = true, time = "10am"), EventDraft("ship")),
            )
        )
        val day = repo.observeDay(date).first()!!
        assertEquals(4, day.day.moodScale)
        assertEquals(listOf(MoodTag.Focused), day.day.moodTags)
        assertEquals(listOf("standup", "ship"), day.events.map { it.text })
        assertEquals(listOf(0, 1), day.events.map { it.position })
    }

    @Test fun saveDay_again_preserves_createdAt_bumps_updatedAt_and_replaces_children() = runTest {
        repo.saveDay(JournalDraft(date = date, moodScale = 3, events = listOf(EventDraft("first"))))
        val created = repo.observeDay(date).first()!!.day.createdAt

        clock = 2000L
        repo.saveDay(JournalDraft(date = date, moodScale = 5, events = listOf(EventDraft("second"))))
        val day = repo.observeDay(date).first()!!
        assertEquals(created, day.day.createdAt)      // preserved
        assertEquals(2000L, day.day.updatedAt)        // bumped
        assertEquals(5, day.day.moodScale)
        assertEquals(listOf("second"), day.events.map { it.text })   // replaced, not appended
    }

    @Test fun deleteDay_removes_entry() = runTest {
        repo.saveDay(JournalDraft(date = date, moodScale = 3))
        repo.deleteDay(date)
        assertNull(repo.observeDay(date).first())
    }

    @Test fun observeMonth_lists_days_of_that_month() = runTest {
        repo.saveDay(JournalDraft(date = LocalDate.of(2026, 6, 1), moodScale = 2))
        repo.saveDay(JournalDraft(date = LocalDate.of(2026, 6, 30), moodScale = 4))
        repo.saveDay(JournalDraft(date = LocalDate.of(2026, 7, 1), moodScale = 5))
        val cells = repo.observeMonth(YearMonth.of(2026, 6)).first()
        assertEquals(listOf("2026-06-01", "2026-06-30"), cells.map { it.date })
    }

    @Test fun reference_resolves_then_becomes_deleted() = runTest {
        val rid = db.roadmapDao().insert(RoadmapEntity(title = "Kotlin"))
        repo.saveDay(
            JournalDraft(date = date, moodScale = 4, references = listOf(ReferenceDraft(RefType.Roadmap, rid)))
        )
        val dayId = repo.observeDay(date).first()!!.day.id
        assertEquals("Kotlin", repo.observeResolvedReferences(dayId).first()[0].resolvedTitle)

        db.roadmapDao().deleteById(rid)
        assertNull(repo.observeResolvedReferences(dayId).first()[0].resolvedTitle)
    }
}
