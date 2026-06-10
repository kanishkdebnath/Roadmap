package com.example.roadmap.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * A Sunday-first month grid for the journal calendar: leading nulls for the days before
 * the 1st, every day of the month, then trailing nulls so the list is whole weeks (size % 7 == 0).
 * Pure — reused by the PR-2 scaffold and the PR-3 heatmap.
 */
fun journalMonthGrid(month: YearMonth): List<LocalDate?> {
    val lead = month.atDay(1).dayOfWeek.value % 7        // Mon=1..Sat=6, Sun(7)→0
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells = List<LocalDate?>(lead) { null } + days
    val trail = (7 - cells.size % 7) % 7
    return cells + List(trail) { null }
}
