package com.example.roadmap.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "roadmap")
data class RoadmapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
)
