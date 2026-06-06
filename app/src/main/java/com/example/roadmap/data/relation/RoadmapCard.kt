package com.example.roadmap.data.relation

import androidx.room.Embedded
import com.example.roadmap.data.entity.RoadmapEntity

/** List-card projection: a roadmap plus aggregate counts for its ring + milestone badge. */
data class RoadmapCard(
    @Embedded val roadmap: RoadmapEntity,
    val milestoneCount: Int,
    val totalSteps: Int,
    val completedSteps: Int,
)
