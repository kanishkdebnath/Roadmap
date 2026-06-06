package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.entity.RoadmapEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Proves Room runs as a JVM unit test under Robolectric (no emulator). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoadmapDatabaseSmokeTest {
    @Test
    fun insert_and_read() = runTest {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RoadmapDatabase::class.java,
        ).build()
        try {
            db.roadmapDao().insert(RoadmapEntity(title = "Learn Rust"))
            assertEquals(1, db.roadmapDao().getAll().size)
        } finally {
            db.close()
        }
    }
}
