package com.example.roadmap.data.journal.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.roadmap.data.journal.MoodTag

@Entity(tableName = "journal_day", indices = [Index(value = ["date"], unique = true)])
data class JournalDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,                       // ISO YYYY-MM-DD, unique (one entry per day)
    val moodScale: Int,                     // 1..5
    val moodTags: List<MoodTag> = emptyList(),   // JSON column via converter, <= 3
    val summary: String? = null,            // <= 500
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
