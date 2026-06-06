package com.example.roadmap.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.roadmap.data.dao.RoadmapDao
import com.example.roadmap.data.entity.RoadmapEntity

@Database(entities = [RoadmapEntity::class], version = 1, exportSchema = false)
abstract class RoadmapDatabase : RoomDatabase() {
    abstract fun roadmapDao(): RoadmapDao
}
