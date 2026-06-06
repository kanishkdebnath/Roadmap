package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.entity.LinkEntity
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity
import com.example.roadmap.data.relation.sorted
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
class RelationTest {
    private lateinit var db: RoadmapDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
    }
    @After fun teardown() = db.close()

    @Test fun nested_tree_loads_and_sorts_by_position() = runTest {
        val rid = db.roadmapDao().insert(RoadmapEntity(title = "Goal"))
        val mid = db.milestoneDao().insert(MilestoneEntity(roadmapId = rid, title = "M1", position = 0))
        // insert steps out of position order to prove sorting
        val s2 = db.stepDao().insert(StepEntity(milestoneId = mid, title = "second", position = 1))
        val s1 = db.stepDao().insert(StepEntity(milestoneId = mid, title = "first", position = 0))
        db.linkDao().insertAll(listOf(LinkEntity(stepId = s1, url = "https://a", position = 0)))

        val tree = db.roadmapDao().observeWithChildren(rid).first()!!.sorted()
        assertEquals("Goal", tree.roadmap.title)
        assertEquals(1, tree.milestones.size)
        assertEquals(listOf("first", "second"), tree.milestones[0].steps.map { it.step.title })
        assertEquals(listOf("https://a"), tree.milestones[0].steps[0].links.map { it.url })
    }
}
