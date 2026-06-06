package com.example.roadmap.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.roadmap.data.entity.MilestoneEntity

@Dao
interface MilestoneDao {
    @Insert suspend fun insert(milestone: MilestoneEntity): Long
    @Update suspend fun update(milestone: MilestoneEntity)

    @Query("SELECT * FROM milestone WHERE roadmapId = :roadmapId ORDER BY position ASC")
    suspend fun getByRoadmap(roadmapId: Long): List<MilestoneEntity>

    @Query("SELECT * FROM milestone WHERE id = :id")
    suspend fun getById(id: Long): MilestoneEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM milestone WHERE roadmapId = :roadmapId")
    suspend fun maxPosition(roadmapId: Long): Int

    @Query("UPDATE milestone SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Query("UPDATE milestone SET completedAt = :completedAt, updatedAt = :now WHERE id = :id")
    suspend fun setCompletedAt(id: Long, completedAt: Long?, now: Long)

    @Query("DELETE FROM milestone WHERE id = :id")
    suspend fun deleteById(id: Long)
}
