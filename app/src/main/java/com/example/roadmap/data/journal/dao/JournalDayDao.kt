package com.example.roadmap.data.journal.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.roadmap.data.journal.entity.JournalDayEntity
import com.example.roadmap.data.journal.relation.DayMoodCell
import com.example.roadmap.data.journal.relation.JournalDayWithChildren
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDayDao {
    @Insert suspend fun insert(day: JournalDayEntity): Long
    @Update suspend fun update(day: JournalDayEntity)

    @Query("SELECT * FROM journal_day WHERE date = :date")
    suspend fun getByDate(date: String): JournalDayEntity?

    @Transaction
    @Query("SELECT * FROM journal_day WHERE date = :date")
    fun observeWithChildren(date: String): Flow<JournalDayWithChildren?>

    @Query("SELECT date, moodScale FROM journal_day WHERE date >= :start AND date < :end ORDER BY date ASC")
    fun observeMonth(start: String, end: String): Flow<List<DayMoodCell>>

    @Query("DELETE FROM journal_day WHERE date = :date")
    suspend fun deleteByDate(date: String)
}
