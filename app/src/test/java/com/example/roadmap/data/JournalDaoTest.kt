package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.journal.RefType
import com.example.roadmap.data.journal.entity.JournalDayEntity
import com.example.roadmap.data.journal.entity.JournalEventEntity
import com.example.roadmap.data.journal.entity.JournalReferenceEntity
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JournalDaoTest {
    private lateinit var db: RoadmapDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
    }
    @After fun teardown() = db.close()

    @Test fun deleting_day_cascades_to_events() = runTest {
        val dayId = db.journalDayDao().insert(JournalDayEntity(date = "2026-06-09", moodScale = 4))
        db.journalEventDao().insertAll(listOf(JournalEventEntity(dayId = dayId, text = "e", position = 0)))
        db.journalDayDao().deleteByDate("2026-06-09")
        assertEquals(0, db.journalEventDao().getByDay(dayId).size)
    }

    @Test fun observeMonth_returns_only_days_in_range() = runTest {
        db.journalDayDao().insert(JournalDayEntity(date = "2026-05-31", moodScale = 1))
        db.journalDayDao().insert(JournalDayEntity(date = "2026-06-09", moodScale = 4))
        db.journalDayDao().insert(JournalDayEntity(date = "2026-07-01", moodScale = 5))
        val cells = db.journalDayDao().observeMonth("2026-06-01", "2026-07-01").first()
        assertEquals(listOf("2026-06-09"), cells.map { it.date })
        assertEquals(4, cells[0].moodScale)
    }

    @Test fun observeResolved_gives_title_then_null_after_target_deleted() = runTest {
        val rid = db.roadmapDao().insert(RoadmapEntity(title = "Kotlin"))
        val dayId = db.journalDayDao().insert(JournalDayEntity(date = "2026-06-09", moodScale = 4))
        db.journalReferenceDao().insertAll(
            listOf(JournalReferenceEntity(dayId = dayId, type = RefType.Roadmap, roadmapId = rid, position = 0))
        )
        assertEquals("Kotlin", db.journalReferenceDao().observeResolved(dayId).first()[0].resolvedTitle)

        db.roadmapDao().deleteById(rid)
        assertNull(db.journalReferenceDao().observeResolved(dayId).first()[0].resolvedTitle)
    }

    @Test fun searchTargets_matches_roadmaps_and_milestones() = runTest {
        val rid = db.roadmapDao().insert(RoadmapEntity(title = "Learn Compose"))
        db.milestoneDao().insert(MilestoneEntity(roadmapId = rid, title = "Compose Basics", position = 0))
        val titles = db.journalReferenceDao().searchTargets("Compose").first().map { it.title }
        assertEquals(listOf("Compose Basics", "Learn Compose"), titles)  // ORDER BY title ASC
    }
}
