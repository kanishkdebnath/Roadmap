package com.example.roadmap.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "link",
    foreignKeys = [ForeignKey(
        entity = StepEntity::class,
        parentColumns = ["id"], childColumns = ["stepId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("stepId", "position")],
)
data class LinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stepId: Long,
    val url: String,                       // http/https only (validated in import, Phase 7)
    val label: String? = null,
    val position: Int,
)
