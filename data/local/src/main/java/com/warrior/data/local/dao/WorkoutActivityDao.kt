package com.warrior.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.warrior.data.local.entity.WorkoutActivityEntity

/**
 * Database Design v4 §15 — Ownership Enforced.
 * Every read/delete joins up to training_sessions.userId so cross-user
 * access is impossible at SQL level (Architecture Rule 6).
 */
@Dao
interface WorkoutActivityDao {

    @Query(
        """
        SELECT wa.*
        FROM workout_activities wa
        INNER JOIN training_sessions ts
            ON wa.sessionId = ts.id
        WHERE wa.id = :activityId
        AND ts.userId = :userId
        """,
    )
    suspend fun getById(userId: Long, activityId: Long): WorkoutActivityEntity?

    @Query(
        """
        SELECT wa.*
        FROM workout_activities wa
        INNER JOIN training_sessions ts
            ON wa.sessionId = ts.id
        WHERE wa.sessionId = :sessionId
        AND ts.userId = :userId
        ORDER BY wa.id ASC
        """,
    )
    suspend fun getActivities(userId: Long, sessionId: Long): List<WorkoutActivityEntity>

    @Query(
        """
        SELECT COALESCE(SUM(wa.duration), 0)
        FROM workout_activities wa
        INNER JOIN training_sessions ts
            ON wa.sessionId = ts.id
        WHERE wa.sessionId = :sessionId
        AND ts.userId = :userId
        """,
    )
    suspend fun sumDurationBySession(userId: Long, sessionId: Long): Long

    @Insert
    suspend fun insert(activity: WorkoutActivityEntity): Long

    @Insert
    suspend fun insertAll(activities: List<WorkoutActivityEntity>): List<Long>

    @Update
    suspend fun update(activity: WorkoutActivityEntity)

    @Query(
        """
        DELETE FROM workout_activities
        WHERE id = :activityId
        AND sessionId IN (
            SELECT id
            FROM training_sessions
            WHERE userId = :userId
        )
        """,
    )
    suspend fun delete(userId: Long, activityId: Long)

    /** Full-table read for backup export (Season 2 / Phase 12). */
    @Query("SELECT * FROM workout_activities")
    suspend fun getAllForExport(): List<WorkoutActivityEntity>

    /** Raw insert with explicit id — used only by backup import (Phase 12). */
    @Insert
    suspend fun insertForImport(activity: WorkoutActivityEntity): Long
}
