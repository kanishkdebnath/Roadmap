package com.example.roadmap.data.journal

import androidx.room.withTransaction
import com.example.roadmap.data.RoadmapDatabase
import com.example.roadmap.data.journal.entity.JournalDayEntity
import com.example.roadmap.data.journal.entity.JournalEventEntity
import com.example.roadmap.data.journal.entity.JournalLinkEntity
import com.example.roadmap.data.journal.entity.JournalReferenceEntity
import com.example.roadmap.data.journal.relation.DayMoodCell
import com.example.roadmap.data.journal.relation.JournalDayWithChildren
import com.example.roadmap.data.journal.relation.RefTarget
import com.example.roadmap.data.journal.relation.ResolvedReference
import com.example.roadmap.data.journal.relation.sorted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

class RoomJournalRepository(
    private val db: RoadmapDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : JournalRepository {

    private val days = db.journalDayDao()
    private val events = db.journalEventDao()
    private val links = db.journalLinkDao()
    private val references = db.journalReferenceDao()

    override fun observeMonth(month: YearMonth): Flow<List<DayMoodCell>> =
        days.observeMonth(month.atDay(1).toString(), month.plusMonths(1).atDay(1).toString())

    override fun observeDay(date: LocalDate): Flow<JournalDayWithChildren?> =
        days.observeWithChildren(date.toString()).map { it?.sorted() }

    override fun observeResolvedReferences(dayId: Long): Flow<List<ResolvedReference>> =
        references.observeResolved(dayId)

    override fun searchReferenceTargets(query: String): Flow<List<RefTarget>> =
        references.searchTargets(query)

    /** Full-day atomic upsert (spec §4): upsert the day by date, then delete+reinsert all children. */
    override suspend fun saveDay(draft: JournalDraft) = db.withTransaction {
        val iso = draft.date.toString()
        val t = now()
        val existing = days.getByDate(iso)
        val dayId = if (existing == null) {
            days.insert(
                JournalDayEntity(
                    date = iso, moodScale = draft.moodScale, moodTags = draft.moodTags,
                    summary = draft.summary, createdAt = t, updatedAt = t,
                )
            )
        } else {
            days.update(
                existing.copy(
                    moodScale = draft.moodScale, moodTags = draft.moodTags,
                    summary = draft.summary, updatedAt = t,        // createdAt preserved
                )
            )
            existing.id
        }
        events.deleteByDay(dayId)
        links.deleteByDay(dayId)
        references.deleteByDay(dayId)
        events.insertAll(draft.events.mapIndexed { i, e ->
            JournalEventEntity(dayId = dayId, text = e.text, important = e.important, time = e.time, position = i)
        })
        links.insertAll(draft.links.mapIndexed { i, l ->
            JournalLinkEntity(dayId = dayId, url = l.url, label = l.label, position = i)
        })
        references.insertAll(draft.references.mapIndexed { i, r ->
            JournalReferenceEntity(dayId = dayId, type = r.type, roadmapId = r.roadmapId, milestoneId = r.milestoneId, position = i)
        })
    }

    override suspend fun deleteDay(date: LocalDate) = days.deleteByDate(date.toString())
}
