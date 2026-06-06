package com.example.roadmap.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.roadmap.data.entity.RoadmapEntity

@Dao
interface RoadmapDao {
    @Insert
    suspend fun insert(roadmap: RoadmapEntity): Long

    @Query("SELECT * FROM roadmap")
    suspend fun getAll(): List<RoadmapEntity>
}
