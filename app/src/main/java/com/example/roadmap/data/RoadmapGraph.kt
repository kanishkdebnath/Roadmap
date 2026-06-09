package com.example.roadmap.data

import android.content.Context
import androidx.room.Room

/** Minimal manual DI: a process-wide database + repository. Replace with Hilt later if desired. */
object RoadmapGraph {
    @Volatile private var database: RoadmapDatabase? = null

    fun database(context: Context): RoadmapDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext, RoadmapDatabase::class.java, "roadmap.db"
            ).addMigrations(MIGRATION_1_2).build().also { database = it }
        }

    fun repository(context: Context): RoadmapRepository = RoomRoadmapRepository(database(context))
}
