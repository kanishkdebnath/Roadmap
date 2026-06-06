package com.example.roadmap.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "milestone",
    foreignKeys = [ForeignKey(
        entity = RoadmapEntity::class,
        parentColumns = ["id"], childColumns = ["roadmapId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("roadmapId", "position")],
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roadmapId: Long,
    val title: String,
    val description: String? = null,
    val deadline: String? = null,
    val position: Int,
    val completedAt: Long? = null,         // derived; set when all steps complete
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
