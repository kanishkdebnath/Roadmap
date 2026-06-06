package com.example.roadmap.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "roadmap", indices = [Index("archived", "updatedAt")])
data class RoadmapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val deadline: String? = null,          // ISO-8601 date YYYY-MM-DD
    val archived: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
