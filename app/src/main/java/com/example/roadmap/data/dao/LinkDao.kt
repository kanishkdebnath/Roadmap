package com.example.roadmap.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.roadmap.data.entity.LinkEntity

@Dao
interface LinkDao {
    @Insert suspend fun insertAll(links: List<LinkEntity>)

    @Query("SELECT * FROM link WHERE stepId = :stepId ORDER BY position ASC")
    suspend fun getByStep(stepId: Long): List<LinkEntity>

    @Query("DELETE FROM link WHERE stepId = :stepId")
    suspend fun deleteByStep(stepId: Long)
}
