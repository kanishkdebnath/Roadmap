package com.example.roadmap.data.journal

import com.example.roadmap.data.LinkDraft
import java.time.LocalDate

/** Editable shape of one day — the repository's saveDay input. Reuses LinkDraft from the roadmap layer. */
data class JournalDraft(
    val date: LocalDate,
    val moodScale: Int = 0,                 // 0 = unset; 1..5 once chosen
    val moodTags: List<MoodTag> = emptyList(),
    val summary: String? = null,
    val events: List<EventDraft> = emptyList(),
    val links: List<LinkDraft> = emptyList(),
    val references: List<ReferenceDraft> = emptyList(),
)

data class EventDraft(val text: String, val important: Boolean = false, val time: String? = null)

data class ReferenceDraft(val type: RefType, val roadmapId: Long, val milestoneId: Long? = null)
