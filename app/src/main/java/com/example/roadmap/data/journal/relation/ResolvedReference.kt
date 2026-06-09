package com.example.roadmap.data.journal.relation

import androidx.room.Embedded
import com.example.roadmap.data.journal.entity.JournalReferenceEntity

/** A reference joined to its target's current title; null title => target was deleted. */
data class ResolvedReference(
    @Embedded val reference: JournalReferenceEntity,
    val resolvedTitle: String?,
)
