package com.example.roadmap.domain

/**
 * A milestone is complete iff it has >= 1 step and every step is completed (spec §7.1).
 * Returns `now` when complete, else null.
 */
fun milestoneCompletedAt(stepCompletions: List<Boolean>, now: Long): Long? =
    if (stepCompletions.isNotEmpty() && stepCompletions.all { it }) now else null
