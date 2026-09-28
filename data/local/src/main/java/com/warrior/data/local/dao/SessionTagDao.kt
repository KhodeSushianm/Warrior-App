package com.warrior.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.warrior.data.local.entity.SessionTagEntity
import kotlinx.coroutines.flow.Flow

/** Session tags (schema v2). The tags UI lands in Phase 17; DAO is ready. */
@Dao
interface SessionTagDao {

    @Insert
    suspend fun insert(tag: SessionTagEntity): Long

    @Query(
        """
        SELECT * FROM session_tags
        WHERE userId = :userId
        ORDER BY tag ASC
        """,
    )
    fun observeByUser(userId: Long): Flow<List<SessionTagEntity>>

    @Query(
        """
        DELETE FROM session_tags
        WHERE id = :id AND userId = :userId
        """,
    )
    suspend fun deleteById(userId: Long, id: Long): Int

    /** Full-table read for backup export (Season 2 / Phase 12). */
    @Query("SELECT * FROM session_tags")
    suspend fun getAllForExport(): List<SessionTagEntity>
}
