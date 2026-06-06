package com.example.roadmap.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.roadmap.data.entity.StepEntity

@Dao
interface StepDao {
    @Insert suspend fun insert(step: StepEntity): Long
    @Update suspend fun update(step: StepEntity)

    @Query("SELECT * FROM step WHERE milestoneId = :milestoneId ORDER BY position ASC")
    suspend fun getByMilestone(milestoneId: Long): List<StepEntity>

    @Query("SELECT * FROM step WHERE id = :id")
    suspend fun getById(id: Long): StepEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM step WHERE milestoneId = :milestoneId")
    suspend fun maxPosition(milestoneId: Long): Int

    @Query("UPDATE step SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Query("DELETE FROM step WHERE id = :id")
    suspend fun deleteById(id: Long)
}
