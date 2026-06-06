package com.example.roadmap.domain

import com.example.roadmap.data.relation.MilestoneWithSteps
import com.example.roadmap.data.relation.RoadmapWithChildren
import java.time.LocalDate

/**
 * A deadline is overdue iff it is set, parses to a date strictly before `today` (local date),
 * and the item is not complete (spec §7.3). Unparseable dates are treated as not overdue.
 */
fun isOverdue(deadline: String?, isComplete: Boolean, today: LocalDate): Boolean {
    if (deadline == null || isComplete) return false
    val date = runCatching { LocalDate.parse(deadline) }.getOrNull() ?: return false
    return date.isBefore(today)
}

/** A milestone is complete when its derived completedAt is set. */
fun MilestoneWithSteps.isOverdue(today: LocalDate): Boolean =
    isOverdue(milestone.deadline, isComplete = milestone.completedAt != null, today = today)

/** A roadmap is complete when it has steps and all of them are done. */
fun RoadmapWithChildren.isOverdue(today: LocalDate): Boolean {
    val total = totalSteps()
    val complete = total > 0 && completedSteps() == total
    return isOverdue(roadmap.deadline, isComplete = complete, today = today)
}
