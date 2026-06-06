package com.example.roadmap.domain

import com.example.roadmap.data.relation.MilestoneWithSteps
import com.example.roadmap.data.relation.RoadmapWithChildren

/** Fraction in 0f..1f; 0 when there are no steps (spec §7.2 divide-by-zero guard). */
fun progressFraction(completed: Int, total: Int): Float =
    if (total <= 0) 0f else completed.toFloat() / total.toFloat()

fun MilestoneWithSteps.totalSteps(): Int = steps.size
fun MilestoneWithSteps.completedSteps(): Int = steps.count { it.step.completed }
fun MilestoneWithSteps.progress(): Float = progressFraction(completedSteps(), totalSteps())

fun RoadmapWithChildren.totalSteps(): Int = milestones.sumOf { it.totalSteps() }
fun RoadmapWithChildren.completedSteps(): Int = milestones.sumOf { it.completedSteps() }
fun RoadmapWithChildren.progress(): Float = progressFraction(completedSteps(), totalSteps())
