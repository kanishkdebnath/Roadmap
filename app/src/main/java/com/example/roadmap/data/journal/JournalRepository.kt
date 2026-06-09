package com.example.roadmap.data.journal

import com.example.roadmap.data.journal.relation.DayMoodCell
import com.example.roadmap.data.journal.relation.JournalDayWithChildren
import com.example.roadmap.data.journal.relation.RefTarget
import com.example.roadmap.data.journal.relation.ResolvedReference
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface JournalRepository {
    fun observeMonth(month: YearMonth): Flow<List<DayMoodCell>>
    fun observeDay(date: LocalDate): Flow<JournalDayWithChildren?>     // children sorted by position
    fun observeResolvedReferences(dayId: Long): Flow<List<ResolvedReference>>
    fun searchReferenceTargets(query: String): Flow<List<RefTarget>>

    suspend fun saveDay(draft: JournalDraft)                          // one atomic transaction
    suspend fun deleteDay(date: LocalDate)
}
