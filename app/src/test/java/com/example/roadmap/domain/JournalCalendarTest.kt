package com.example.roadmap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class JournalCalendarTest {
    @Test fun june_2026_has_one_leading_blank_and_fills_whole_weeks() {
        val grid = journalMonthGrid(YearMonth.of(2026, 6))   // Jun 1 2026 is a Monday
        assertEquals(0, grid.size % 7)                       // rectangular (whole weeks)
        assertEquals(35, grid.size)                          // 1 lead + 30 days + 4 trail
        assertNull(grid[0])                                  // Sunday slot before Mon Jun 1
        assertEquals(LocalDate.of(2026, 6, 1), grid[1])
        assertEquals(LocalDate.of(2026, 6, 9), grid[9])
        assertEquals(LocalDate.of(2026, 6, 30), grid[30])
        assertNull(grid[34])                                 // trailing pad
        assertEquals(30, grid.count { it != null })
    }

    @Test fun month_starting_on_sunday_has_no_leading_blank() {
        val grid = journalMonthGrid(YearMonth.of(2026, 3))   // Mar 1 2026 is a Sunday
        assertEquals(LocalDate.of(2026, 3, 1), grid[0])
        assertEquals(0, grid.size % 7)
    }

    @Test fun february_2027_has_28_days_and_whole_weeks() {
        val grid = journalMonthGrid(YearMonth.of(2027, 2))   // Feb 1 2027 is a Monday
        assertNull(grid[0])
        assertEquals(LocalDate.of(2027, 2, 1), grid[1])
        assertEquals(28, grid.count { it != null })
        assertEquals(0, grid.size % 7)
    }
}
