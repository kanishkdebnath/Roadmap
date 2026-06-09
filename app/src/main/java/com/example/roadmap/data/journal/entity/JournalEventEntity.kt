package com.example.roadmap.data.journal.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_event",
    foreignKeys = [ForeignKey(
        entity = JournalDayEntity::class,
        parentColumns = ["id"], childColumns = ["dayId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("dayId", "position")],
)
data class JournalEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val text: String,                       // 1..500
    val important: Boolean = false,
    val time: String? = null,               // optional free text, <= 20
    val position: Int,
)
