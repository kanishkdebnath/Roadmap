package com.example.roadmap.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OverdueTest {
    private val today = LocalDate.of(2026, 6, 6)

    @Test fun past_deadline_and_incomplete_is_overdue() {
        assertTrue(isOverdue(deadline = "2026-06-05", isComplete = false, today = today))
    }

    @Test fun complete_is_never_overdue() {
        assertFalse(isOverdue(deadline = "2026-06-05", isComplete = true, today = today))
    }

    @Test fun future_or_today_deadline_is_not_overdue() {
        assertFalse(isOverdue("2026-06-07", isComplete = false, today = today))
        assertFalse(isOverdue("2026-06-06", isComplete = false, today = today)) // due today, not yet overdue
    }

    @Test fun null_or_unparseable_deadline_is_not_overdue() {
        assertFalse(isOverdue(null, isComplete = false, today = today))
        assertFalse(isOverdue("not-a-date", isComplete = false, today = today))
    }
}
