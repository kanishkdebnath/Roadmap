package com.example.roadmap.domain

import com.example.roadmap.data.journal.JournalDraft

/** Canonical form for fair comparison: trim text, drop blank children, cap tags at 3. */
fun JournalDraft.normalized(): JournalDraft = copy(
    summary = summary?.trim()?.ifBlank { null },
    moodTags = moodTags.distinct().take(3),
    events = events.mapNotNull { e ->
        e.text.trim().ifBlank { null }?.let { e.copy(text = it, time = e.time?.trim()?.ifBlank { null }) }
    },
    links = links.mapNotNull { l ->
        l.url.trim().ifBlank { null }?.let { l.copy(url = it, label = l.label?.trim()?.ifBlank { null }) }
    },
)

/** Save is allowed only when a mood (1..5) is set AND the day differs from what's stored. */
fun canSaveJournal(draft: JournalDraft, saved: JournalDraft?): Boolean =
    draft.moodScale in 1..5 && (saved == null || draft.normalized() != saved.normalized())
