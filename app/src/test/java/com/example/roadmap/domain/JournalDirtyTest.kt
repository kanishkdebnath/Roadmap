package com.example.roadmap.domain

import com.example.roadmap.data.journal.EventDraft
import com.example.roadmap.data.journal.JournalDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class JournalDirtyTest {
    private val date = LocalDate.of(2026, 6, 9)

    @Test fun normalized_trims_summary_and_drops_blank_events_and_caps_tags() {
        val d = JournalDraft(
            date = date, moodScale = 4,
            summary = "  hi  ",
            events = listOf(EventDraft("  keep  "), EventDraft("   ")),
        ).normalized()
        assertEquals("hi", d.summary)
        assertEquals(listOf("keep"), d.events.map { it.text })
    }

    @Test fun whitespace_only_change_is_not_dirty() {
        val saved = JournalDraft(date = date, moodScale = 4, summary = "hi")
        val edited = saved.copy(summary = "hi   ")
        assertFalse(canSaveJournal(edited, saved))
    }

    @Test fun real_change_with_mood_is_saveable() {
        val saved = JournalDraft(date = date, moodScale = 4, summary = "hi")
        val edited = saved.copy(summary = "bye")
        assertTrue(canSaveJournal(edited, saved))
    }

    @Test fun new_day_needs_a_mood_to_be_saveable() {
        assertFalse(canSaveJournal(JournalDraft(date = date, moodScale = 0), saved = null))
        assertTrue(canSaveJournal(JournalDraft(date = date, moodScale = 3), saved = null))
    }
}
