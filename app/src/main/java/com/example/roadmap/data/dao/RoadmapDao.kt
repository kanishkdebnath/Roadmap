package com.example.roadmap.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.relation.RoadmapWithChildren
import kotlinx.coroutines.flow.Flow

@Dao
interface RoadmapDao {
    @Insert suspend fun insert(roadmap: RoadmapEntity): Long
    @Update suspend fun update(roadmap: RoadmapEntity)

    @Query("SELECT * FROM roadmap WHERE archived = :archived ORDER BY updatedAt DESC")
    fun observeByArchived(archived: Boolean): Flow<List<RoadmapEntity>>

    @Query("SELECT * FROM roadmap WHERE id = :id")
    suspend fun getById(id: Long): RoadmapEntity?

    @Query("UPDATE roadmap SET archived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long)

    @Query("DELETE FROM roadmap WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    @Query("SELECT * FROM roadmap WHERE id = :id")
    fun observeWithChildren(id: Long): Flow<RoadmapWithChildren?>
}
