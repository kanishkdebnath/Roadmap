package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity
import com.example.roadmap.data.relation.RoadmapWithChildren
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies whether observeWithChildren() re-emits when a DESCENDANT table (step) is written,
 * not just the roadmap row. Settles whether @Transaction+@Relation observes child tables.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReactivityTest {
    private lateinit var db: RoadmapDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
    }
    @After fun teardown() = db.close()

    @Test fun observeWithChildren_reemits_on_step_write() = runBlocking {
        val rid = db.roadmapDao().insert(RoadmapEntity(title = "G"))
        val mid = db.milestoneDao().insert(MilestoneEntity(roadmapId = rid, title = "M", position = 0))
        val sid = db.stepDao().insert(StepEntity(milestoneId = mid, title = "S", position = 0))

        val emissions = Channel<RoadmapWithChildren?>(Channel.UNLIMITED)
        val scope = CoroutineScope(Dispatchers.IO)
        val job = scope.launch {
            db.roadmapDao().observeWithChildren(rid).collect { emissions.send(it) }
        }
        try {
            val first = withTimeout(3000) { emissions.receive() }!!
            assertEquals(false, first.milestones[0].steps[0].step.completed)

            // Write to a CHILD table (step), not the roadmap row.
            db.stepDao().update(db.stepDao().getById(sid)!!.copy(completed = true))

            val updated = withTimeout(3000) {
                var e = emissions.receive()
                while (e?.milestones?.firstOrNull()?.steps?.firstOrNull()?.step?.completed != true) {
                    e = emissions.receive()
                }
                e
            }
            assertEquals(true, updated.milestones[0].steps[0].step.completed)
        } finally {
            job.cancel()
        }
    }
}
