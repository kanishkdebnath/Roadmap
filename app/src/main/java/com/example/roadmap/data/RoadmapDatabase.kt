package com.example.roadmap.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.roadmap.data.dao.LinkDao
import com.example.roadmap.data.dao.MilestoneDao
import com.example.roadmap.data.dao.RoadmapDao
import com.example.roadmap.data.dao.StepDao
import com.example.roadmap.data.entity.LinkEntity
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity
import com.example.roadmap.data.journal.JournalConverters
import com.example.roadmap.data.journal.entity.JournalDayEntity
import com.example.roadmap.data.journal.entity.JournalEventEntity
import com.example.roadmap.data.journal.entity.JournalLinkEntity
import com.example.roadmap.data.journal.entity.JournalReferenceEntity

@Database(
    entities = [
        RoadmapEntity::class, MilestoneEntity::class, StepEntity::class, LinkEntity::class,
        JournalDayEntity::class, JournalEventEntity::class, JournalLinkEntity::class, JournalReferenceEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(JournalConverters::class)
abstract class RoadmapDatabase : RoomDatabase() {
    abstract fun roadmapDao(): RoadmapDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun stepDao(): StepDao
    abstract fun linkDao(): LinkDao
    // journal DAO accessor methods are added in Task 5 (once those DAOs exist)
}
