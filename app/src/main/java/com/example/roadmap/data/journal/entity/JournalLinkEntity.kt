package com.example.roadmap.data.journal.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_link",
    foreignKeys = [ForeignKey(
        entity = JournalDayEntity::class,
        parentColumns = ["id"], childColumns = ["dayId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("dayId", "position")],
)
data class JournalLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val url: String,                        // http/https only (validated in UI layer)
    val label: String? = null,
    val position: Int,
)
