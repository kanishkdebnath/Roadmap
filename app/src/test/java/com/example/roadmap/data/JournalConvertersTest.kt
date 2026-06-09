package com.example.roadmap.data

import com.example.roadmap.data.journal.JournalConverters
import com.example.roadmap.data.journal.MoodTag
import com.example.roadmap.data.journal.RefType
import org.junit.Assert.assertEquals
import org.junit.Test

class JournalConvertersTest {
    private val c = JournalConverters()

    @Test fun moodTags_round_trip() {
        val tags = listOf(MoodTag.Focused, MoodTag.Grateful)
        assertEquals(tags, c.toMoodTags(c.fromMoodTags(tags)))
    }

    @Test fun empty_moodTags_round_trips_to_empty_list() {
        assertEquals(emptyList<MoodTag>(), c.toMoodTags(c.fromMoodTags(emptyList())))
    }

    @Test fun blank_json_decodes_to_empty_list() {
        assertEquals(emptyList<MoodTag>(), c.toMoodTags(""))
    }

    @Test fun refType_round_trip() {
        assertEquals(RefType.Milestone, c.toRefType(c.fromRefType(RefType.Milestone)))
    }
}
