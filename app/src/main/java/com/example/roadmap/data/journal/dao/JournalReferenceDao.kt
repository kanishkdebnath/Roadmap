package com.example.roadmap.data.journal.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.roadmap.data.journal.entity.JournalReferenceEntity
import com.example.roadmap.data.journal.relation.RefTarget
import com.example.roadmap.data.journal.relation.ResolvedReference
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalReferenceDao {
    @Insert suspend fun insertAll(references: List<JournalReferenceEntity>)

    @Query("SELECT * FROM journal_reference WHERE dayId = :dayId ORDER BY position ASC")
    suspend fun getByDay(dayId: Long): List<JournalReferenceEntity>

    @Query("DELETE FROM journal_reference WHERE dayId = :dayId")
    suspend fun deleteByDay(dayId: Long)

    /** Resolve each reference's current target title (null => deleted). */
    @Query(
        """
        SELECT ref.*,
          CASE ref.type
            WHEN 'Roadmap'  THEN (SELECT title FROM roadmap   WHERE id = ref.roadmapId)
            WHEN 'Milestone' THEN (SELECT title FROM milestone WHERE id = ref.milestoneId)
          END AS resolvedTitle
        FROM journal_reference ref
        WHERE ref.dayId = :dayId
        ORDER BY ref.position ASC
        """
    )
    fun observeResolved(dayId: Long): Flow<List<ResolvedReference>>

    /** Searchable picker over the app's roadmaps + milestones. */
    @Query(
        """
        SELECT 'Roadmap' AS type, r.id AS roadmapId, NULL AS milestoneId, r.title AS title
          FROM roadmap r WHERE (:q = '' OR r.title LIKE '%' || :q || '%')
        UNION ALL
        SELECT 'Milestone' AS type, m.roadmapId AS roadmapId, m.id AS milestoneId, m.title AS title
          FROM milestone m WHERE (:q = '' OR m.title LIKE '%' || :q || '%')
        ORDER BY title ASC
        """
    )
    fun searchTargets(q: String): Flow<List<RefTarget>>
}
