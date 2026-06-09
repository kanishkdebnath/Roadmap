package com.example.roadmap.data.journal.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.roadmap.data.journal.entity.JournalDayEntity
import com.example.roadmap.data.journal.entity.JournalEventEntity
import com.example.roadmap.data.journal.entity.JournalLinkEntity
import com.example.roadmap.data.journal.entity.JournalReferenceEntity

data class JournalDayWithChildren(
    @Embedded val day: JournalDayEntity,
    @Relation(parentColumn = "id", entityColumn = "dayId") val events: List<JournalEventEntity>,
    @Relation(parentColumn = "id", entityColumn = "dayId") val links: List<JournalLinkEntity>,
    @Relation(parentColumn = "id", entityColumn = "dayId") val references: List<JournalReferenceEntity>,
)

/** Returns a copy with each child list ordered by its `position` (Room doesn't order @Relation lists). */
fun JournalDayWithChildren.sorted(): JournalDayWithChildren = copy(
    events = events.sortedBy { it.position },
    links = links.sortedBy { it.position },
    references = references.sortedBy { it.position },
)
