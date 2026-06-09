package com.example.roadmap.data.journal.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.roadmap.data.journal.entity.JournalLinkEntity

@Dao
interface JournalLinkDao {
    @Insert suspend fun insertAll(links: List<JournalLinkEntity>)

    @Query("SELECT * FROM journal_link WHERE dayId = :dayId ORDER BY position ASC")
    suspend fun getByDay(dayId: Long): List<JournalLinkEntity>

    @Query("DELETE FROM journal_link WHERE dayId = :dayId")
    suspend fun deleteByDay(dayId: Long)
}
