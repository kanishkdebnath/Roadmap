package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.entity.LinkEntity
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity
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
class CascadeTest {
    private lateinit var db: RoadmapDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
    }
    @After fun teardown() = db.close()

    @Test fun deleting_roadmap_cascades_to_milestones_steps_links() = runTest {
        val rid = db.roadmapDao().insert(RoadmapEntity(title = "Goal"))
        val mid = db.milestoneDao().insert(MilestoneEntity(roadmapId = rid, title = "M", position = 0))
        val sid = db.stepDao().insert(StepEntity(milestoneId = mid, title = "S", position = 0))
        db.linkDao().insertAll(listOf(LinkEntity(stepId = sid, url = "https://a", position = 0)))

        db.roadmapDao().deleteById(rid)

        assertEquals(0, db.milestoneDao().getByRoadmap(rid).size)
        assertEquals(0, db.stepDao().getByMilestone(mid).size)
        assertEquals(0, db.linkDao().getByStep(sid).size)
    }
}
