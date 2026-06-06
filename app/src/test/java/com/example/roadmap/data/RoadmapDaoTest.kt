package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.entity.RoadmapEntity
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
class RoadmapDaoTest {
    private lateinit var db: RoadmapDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
    }
    @After fun teardown() = db.close()

    @Test fun observeByArchived_filters_and_orders_by_updatedAt_desc() = runTest {
        val dao = db.roadmapDao()
        dao.insert(RoadmapEntity(title = "A", archived = false, updatedAt = 100))
        dao.insert(RoadmapEntity(title = "B", archived = false, updatedAt = 200))
        dao.insert(RoadmapEntity(title = "Z", archived = true, updatedAt = 300))
        val active = dao.observeByArchived(false).first()
        assertEquals(listOf("B", "A"), active.map { it.title })
        assertEquals(listOf("Z"), dao.observeByArchived(true).first().map { it.title })
    }
}
