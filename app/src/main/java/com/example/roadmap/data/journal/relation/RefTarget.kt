package com.example.roadmap.data.journal.relation

import com.example.roadmap.data.journal.RefType

/** A pickable reference target (a roadmap or one of its milestones). */
data class RefTarget(
    val type: RefType,
    val roadmapId: Long,
    val milestoneId: Long?,
    val title: String,
)
