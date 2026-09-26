package com.warrior.data.local.mapper

import com.warrior.data.local.dao.ActivityWithRounds
import com.warrior.data.local.entity.RoundEntity
import com.warrior.data.local.entity.TrainingSessionEntity
import com.warrior.data.local.entity.WorkoutActivityEntity
import com.warrior.domain.training.model.Round
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutActivity
import kotlin.time.Duration.Companion.milliseconds

/**
 * Entity ↔ Domain mapping (Architecture v2.1 §10).
 * Durations: Long millis in entities, kotlin.time.Duration in domain.
 * Timestamps stay Long UTC epoch millis on both sides (DB v4 §9).
 */
object SessionMapper {

    fun toDomain(
        session: TrainingSessionEntity,
        activities: List<WorkoutActivityEntity>,
        roundsByActivityId: Map<Long, List<RoundEntity>>,
    ): TrainingSession = TrainingSession(
        id = session.id,
        userId = session.userId,
        date = session.date,
        startedAt = session.startedAt,
        endedAt = session.endedAt,
        overallIntensity = session.overallIntensity,
        overallFeeling = session.overallFeeling,
        notes = session.notes,
        createdAt = session.createdAt,
        updatedAt = session.updatedAt,
        activities = activities.map { it.toDomain(roundsByActivityId[it.id].orEmpty()) },
    )

    fun toEntity(session: TrainingSession): TrainingSessionEntity = TrainingSessionEntity(
        id = session.id,
        userId = session.userId,
        date = session.date,
        startedAt = session.startedAt,
        endedAt = session.endedAt,
        overallIntensity = session.overallIntensity,
        overallFeeling = session.overallFeeling,
        notes = session.notes,
        createdAt = session.createdAt,
        updatedAt = session.updatedAt,
    )

    /**
     * Activity + rounds pair for the transactional DAO writes.
     * `sessionId` / round `activityId` are assigned inside the DAO transaction;
     * pass 0 placeholders for creates.
     */
    fun toActivityWithRounds(activity: WorkoutActivity, sessionId: Long): ActivityWithRounds =
        ActivityWithRounds(
            activity = WorkoutActivityEntity(
                id = activity.id,
                sessionId = sessionId,
                type = activity.type,
                duration = activity.duration.inWholeMilliseconds,
                intensity = activity.intensity,
                focusArea = activity.focusArea,
                notes = activity.notes,
                createdAt = activity.createdAt,
                updatedAt = activity.updatedAt,
            ),
            rounds = activity.rounds.map { round ->
                RoundEntity(
                    id = round.id,
                    activityId = round.activityId,
                    roundNumber = round.roundNumber,
                    duration = round.duration.inWholeMilliseconds,
                    restDuration = round.restDuration.inWholeMilliseconds,
                    intensity = round.intensity,
                    notes = round.notes,
                    createdAt = round.createdAt,
                    updatedAt = round.updatedAt,
                )
            },
        )

    private fun WorkoutActivityEntity.toDomain(rounds: List<RoundEntity>): WorkoutActivity =
        WorkoutActivity(
            id = id,
            sessionId = sessionId,
            type = type,
            duration = duration.milliseconds,
            intensity = intensity,
            focusArea = focusArea,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
            rounds = rounds.map { it.toDomain() },
        )

    private fun RoundEntity.toDomain(): Round = Round(
        id = id,
        activityId = activityId,
        roundNumber = roundNumber,
        duration = duration.milliseconds,
        restDuration = restDuration.milliseconds,
        intensity = intensity,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
