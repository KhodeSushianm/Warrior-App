package com.warrior.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.warrior.data.local.entity.RoundEntity

/**
 * Database Design v4 §16 — Ownership Enforced.
 * Rounds join through activities up to training_sessions.userId.
 */
@Dao
interface RoundDao {

    @Query(
        """
        SELECT r.*
        FROM rounds r
        INNER JOIN workout_activities wa
            ON r.activityId = wa.id
        INNER JOIN training_sessions ts
            ON wa.sessionId = ts.id
        WHERE r.id = :roundId
        AND ts.userId = :userId
        """,
    )
    suspend fun getById(userId: Long, roundId: Long): RoundEntity?

    @Query(
        """
        SELECT r.*
        FROM rounds r
        INNER JOIN workout_activities wa
            ON r.activityId = wa.id
        INNER JOIN training_sessions ts
            ON wa.sessionId = ts.id
        WHERE r.activityId = :activityId
        AND ts.userId = :userId
        ORDER BY r.roundNumber ASC
        """,
    )
    suspend fun getRounds(userId: Long, activityId: Long): List<RoundEntity>

    @Insert
    suspend fun insert(round: RoundEntity): Long

    @Insert
    suspend fun insertAll(rounds: List<RoundEntity>)

    @Update
    suspend fun update(round: RoundEntity)

    @Query(
        """
        DELETE FROM rounds
        WHERE id = :roundId
        AND activityId IN (
            SELECT wa.id
            FROM workout_activities wa
            INNER JOIN training_sessions ts
                ON wa.sessionId = ts.id
            WHERE ts.userId = :userId
        )
        """,
    )
    suspend fun delete(userId: Long, roundId: Long)

    /** Full-table read for backup export (Season 2 / Phase 12). */
    @Query("SELECT * FROM rounds")
    suspend fun getAllForExport(): List<RoundEntity>

    /** Raw insert with explicit id — used only by backup import (Phase 12). */
    @Insert
    suspend fun insertForImport(round: RoundEntity): Long
}
