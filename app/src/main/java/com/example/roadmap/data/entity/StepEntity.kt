package com.example.roadmap.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "step",
    foreignKeys = [ForeignKey(
        entity = MilestoneEntity::class,
        parentColumns = ["id"], childColumns = ["milestoneId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("milestoneId", "position")],
)
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val milestoneId: Long,
    val title: String,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val position: Int,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
