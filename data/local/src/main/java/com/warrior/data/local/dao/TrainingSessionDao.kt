package com.warrior.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.warrior.data.local.entity.RoundEntity
import com.warrior.data.local.entity.TrainingSessionEntity
import com.warrior.data.local.entity.WorkoutActivityEntity
import kotlinx.coroutines.flow.Flow

/**
 * Database Design v4 §14 + §18 + §19.
 *
 * The transaction boundary for composite writes is [insertFullSession] in this
 * single DAO (Architecture Rule 7). Repositories must not call the protected
 * inserts separately.
 */
@Dao
abstract class TrainingSessionDao {

    @Query(
        """
        SELECT * FROM training_sessions
        WHERE userId = :userId
        ORDER BY date DESC
        """,
    )
    abstract fun observeSessions(userId: Long): Flow<List<TrainingSessionEntity>>

    @Query(
        """
        SELECT * FROM training_sessions
        WHERE userId = :userId
        AND id = :sessionId
        """,
    )
    abstract suspend fun getSession(userId: Long, sessionId: Long): TrainingSessionEntity?

    /**
     * Ownership-enforced session duration (DB v4 §18): SUM of activity durations,
     * never startedAt/endedAt.
     */
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
    abstract suspend fun getSessionTotalDuration(userId: Long, sessionId: Long): Long

    @Insert
    protected abstract suspend fun insertSession(session: TrainingSessionEntity): Long

    @Insert
    protected abstract suspend fun insertActivities(
        activities: List<WorkoutActivityEntity>,
    ): List<Long>

    @Insert
    protected abstract suspend fun insertRounds(rounds: List<RoundEntity>)

    @Update
    abstract suspend fun updateSession(session: TrainingSessionEntity)

    @Query(
        """
        DELETE FROM training_sessions
        WHERE userId = :userId
        AND id = :sessionId
        """,
    )
    abstract suspend fun deleteSession(userId: Long, sessionId: Long)

    /**
     * Atomic create: session + activities + rounds in one transaction.
     * Any failure rolls everything back (DB v4 §19).
     */
    @Transaction
    open suspend fun insertFullSession(
        session: TrainingSessionEntity,
        activitiesWithRounds: List<ActivityWithRounds>,
    ): Long {
        val sessionId = insertSession(session)
        insertActivitiesAndRounds(sessionId, activitiesWithRounds)
        return sessionId
    }

    /**
     * Atomic replace of an existing aggregate (Phase 3 edit flow):
     * update header, drop old activities (rounds cascade), insert the new tree.
     */
    @Transaction
    open suspend fun updateFullSession(
        session: TrainingSessionEntity,
        activitiesWithRounds: List<ActivityWithRounds>,
    ) {
        updateSession(session)
        deleteActivitiesOfSession(session.id)
        insertActivitiesAndRounds(session.id, activitiesWithRounds)
    }

    @Query(
        """
        DELETE FROM workout_activities
        WHERE sessionId = :sessionId
        """,
    )
    protected abstract suspend fun deleteActivitiesOfSession(sessionId: Long)

    private suspend fun insertActivitiesAndRounds(
        sessionId: Long,
        activitiesWithRounds: List<ActivityWithRounds>,
    ) {
        val activityIds = insertActivities(
            activitiesWithRounds.map {
                it.activity.copy(sessionId = sessionId)
            },
        )

        // Room returns insertAll IDs in input order, so this pairing is internal and safe.
        val allRounds = activitiesWithRounds.flatMapIndexed { index, awr ->
            awr.rounds.map { it.copy(activityId = activityIds[index]) }
        }

        if (allRounds.isNotEmpty()) {
            insertRounds(allRounds)
        }
    }
}
