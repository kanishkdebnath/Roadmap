package com.example.roadmap.ui.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

class DateFormatTest {
    @Before fun setup() { Locale.setDefault(Locale.US) }

    @Test fun formats_iso_date_as_month_day() {
        assertEquals("Sep 30", formatDeadline("2026-09-30"))
        assertEquals("Jan 5", formatDeadline("2026-01-05"))
    }
    @Test fun null_or_invalid_is_null() {
        assertNull(formatDeadline(null))
        assertNull(formatDeadline("nope"))
    }
}
