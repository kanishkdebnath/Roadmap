package com.example.roadmap.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.roadmap.data.dao.LinkDao
import com.example.roadmap.data.dao.MilestoneDao
import com.example.roadmap.data.dao.RoadmapDao
import com.example.roadmap.data.dao.StepDao
import com.example.roadmap.data.entity.LinkEntity
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity

@Database(
    entities = [RoadmapEntity::class, MilestoneEntity::class, StepEntity::class, LinkEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class RoadmapDatabase : RoomDatabase() {
    abstract fun roadmapDao(): RoadmapDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun stepDao(): StepDao
    abstract fun linkDao(): LinkDao
}
