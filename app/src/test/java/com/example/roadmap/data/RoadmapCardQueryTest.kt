package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoadmapCardQueryTest {
    private lateinit var db: RoadmapDatabase
    private lateinit var repo: RoomRoadmapRepository
    private var clock = 1L
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
        repo = RoomRoadmapRepository(db) { clock++ }   // increasing clock => deterministic updatedAt order
    }
    @After fun teardown() = db.close()

    @Test fun card_has_aggregate_counts() = runTest {
        val rid = repo.createRoadmap("Learn Kotlin")
        val m1 = repo.addMilestone(rid, "Basics")
        repo.addMilestone(rid, "Compose")
        val s1 = repo.addStep(m1, "s1"); repo.addStep(m1, "s2")
        repo.setStepCompleted(s1, true)
        val card = repo.observeRoadmapCards(archived = false, query = "").first().single()
        assertEquals(2, card.milestoneCount)
        assertEquals(2, card.totalSteps)
        assertEquals(1, card.completedSteps)
    }

    @Test fun search_matches_title_description_and_milestone_title() = runTest {
        val a = repo.createRoadmap("Run a marathon", description = "fitness goal")
        repo.addMilestone(a, "Long runs")
        val b = repo.createRoadmap("Learn Spanish")
        repo.addMilestone(b, "Vocabulary")

        // milestone title match -> surfaces parent
        assertEquals(listOf("Run a marathon"),
            repo.observeRoadmapCards(false, "long").first().map { it.roadmap.title })
        // description match
        assertEquals(listOf("Run a marathon"),
            repo.observeRoadmapCards(false, "fitness").first().map { it.roadmap.title })
        // title match
        assertEquals(listOf("Learn Spanish"),
            repo.observeRoadmapCards(false, "spanish").first().map { it.roadmap.title })
        // case-insensitive (uppercase query against mixed-case milestone title)
        assertEquals(listOf("Run a marathon"),
            repo.observeRoadmapCards(false, "LONG").first().map { it.roadmap.title })
        // no match
        assertEquals(0, repo.observeRoadmapCards(false, "zzz").first().size)
    }

    @Test fun cards_ordered_by_updatedAt_desc() = runTest {
        repo.createRoadmap("First")
        repo.createRoadmap("Second")   // later create => higher updatedAt => sorts first
        assertEquals(listOf("Second", "First"),
            repo.observeRoadmapCards(false, "").first().map { it.roadmap.title })
    }

    @Test fun archived_scope_filters() = runTest {
        val a = repo.createRoadmap("Active one")
        repo.createRoadmap("To archive").also { repo.setArchived(it, true) }
        assertEquals(listOf("Active one"), repo.observeRoadmapCards(false, "").first().map { it.roadmap.title })
        assertEquals(listOf("To archive"), repo.observeRoadmapCards(true, "").first().map { it.roadmap.title })
    }
}
