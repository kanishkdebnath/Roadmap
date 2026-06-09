package com.example.roadmap.data.journal.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.roadmap.data.journal.entity.JournalEventEntity

@Dao
interface JournalEventDao {
    @Insert suspend fun insertAll(events: List<JournalEventEntity>)

    @Query("SELECT * FROM journal_event WHERE dayId = :dayId ORDER BY position ASC")
    suspend fun getByDay(dayId: Long): List<JournalEventEntity>

    @Query("DELETE FROM journal_event WHERE dayId = :dayId")
    suspend fun deleteByDay(dayId: Long)
}
