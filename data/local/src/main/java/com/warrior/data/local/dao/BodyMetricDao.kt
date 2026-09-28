package com.warrior.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.warrior.data.local.entity.BodyMetricEntity
import kotlinx.coroutines.flow.Flow

/** Body-metric checkpoints (schema v2, Season 2 Phase 12). Ownership-scoped. */
@Dao
interface BodyMetricDao {

    @Insert
    suspend fun insert(metric: BodyMetricEntity): Long

    @Query(
        """
        SELECT * FROM body_metrics
        WHERE userId = :userId
        ORDER BY date DESC, createdAt DESC
        """,
    )
    fun observeByUser(userId: Long): Flow<List<BodyMetricEntity>>

    @Query(
        """
        SELECT * FROM body_metrics
        WHERE userId = :userId
        ORDER BY date DESC, createdAt DESC
        LIMIT 1
        """,
    )
    suspend fun latest(userId: Long): BodyMetricEntity?

    @Query(
        """
        DELETE FROM body_metrics
        WHERE id = :id AND userId = :userId
        """,
    )
    suspend fun deleteById(userId: Long, id: Long): Int

    /** Full-table read for backup export (Season 2 / Phase 12). */
    @Query("SELECT * FROM body_metrics")
    suspend fun getAllForExport(): List<BodyMetricEntity>
}
