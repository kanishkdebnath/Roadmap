package com.example.roadmap.data.journal.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.roadmap.data.journal.RefType

/**
 * roadmapId / milestoneId are PLAIN columns (not FKs) so deleting a roadmap leaves the row;
 * the chip then resolves to "(deleted)" at render (spec §3).
 */
@Entity(
    tableName = "journal_reference",
    foreignKeys = [ForeignKey(
        entity = JournalDayEntity::class,
        parentColumns = ["id"], childColumns = ["dayId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("dayId"), Index("roadmapId"), Index("milestoneId")],
)
data class JournalReferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val type: RefType,
    val roadmapId: Long,
    val milestoneId: Long? = null,          // set when type == Milestone
    val position: Int,
)
