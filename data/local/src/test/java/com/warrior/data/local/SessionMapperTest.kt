package com.warrior.data.local

import com.warrior.data.local.entity.RoundEntity
import com.warrior.data.local.entity.TrainingSessionEntity
import com.warrior.data.local.entity.WorkoutActivityEntity
import com.warrior.data.local.mapper.SessionMapper
import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.Round
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutActivity
import com.warrior.domain.training.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/** Plain-JVM mapper tests (no Android/Robolectric needed). */
class SessionMapperTest {

    private val now = 1_758_000_000_000L

    @Test
    fun entityToDomain_mapsDurationsAndNesting() {
        val sessionEntity = TrainingSessionEntity(
            id = 7,
            userId = 3,
            date = now,
            startedAt = now,
            endedAt = now + 3_600_000,
            overallIntensity = 8,
            overallFeeling = Feeling.EXCELLENT,
            notes = "note",
            createdAt = now,
            updatedAt = now,
        )
        val activityEntity = WorkoutActivityEntity(
            id = 11,
            sessionId = 7,
            type = WorkoutType.SPARRING,
            duration = 60_000,
            intensity = 9,
            focusArea = FocusArea.TIMING,
            notes = null,
            createdAt = now,
            updatedAt = now,
        )
        val roundEntity = RoundEntity(
            id = 21,
            activityId = 11,
            roundNumber = 2,
            duration = 180_000,
            restDuration = 0,
            intensity = 9,
            notes = null,
            createdAt = now,
            updatedAt = now,
        )

        val domain = SessionMapper.toDomain(sessionEntity, listOf(activityEntity), mapOf(11L to listOf(roundEntity)))

        assertEquals(7L, domain.id)
        assertEquals(3L, domain.userId)
        assertEquals(Feeling.EXCELLENT, domain.overallFeeling)
        assertEquals(1, domain.activities.size)
        assertEquals(1.minutes, domain.activities[0].duration)
        assertEquals(1, domain.activities[0].rounds.size)
        assertEquals(3.minutes, domain.activities[0].rounds[0].duration)
        assertEquals(0.minutes, domain.activities[0].rounds[0].restDuration)
        assertEquals(1.minutes, domain.totalDuration)
        assertEquals(1, domain.totalRounds)
    }

    @Test
    fun domainToEntity_roundTripsDurations() {
        val domain = TrainingSession(
            id = 5,
            userId = 1,
            date = now,
            overallIntensity = 6,
            overallFeeling = Feeling.TIRED,
            activities = listOf(
                WorkoutActivity(
                    id = 9,
                    sessionId = 5,
                    type = WorkoutType.CARDIO,
                    duration = 25.minutes,
                    intensity = 5,
                    focusArea = FocusArea.CONDITIONING,
                    rounds = listOf(
                        Round(id = 31, activityId = 9, roundNumber = 1, duration = 2.minutes, restDuration = 1.minutes, intensity = 7),
                    ),
                ),
            ),
        )

        val entity = SessionMapper.toEntity(domain)
        assertEquals(5L, entity.id)
        assertEquals(6, entity.overallIntensity)

        val pair = SessionMapper.toActivityWithRounds(domain.activities[0], sessionId = 5)
        assertEquals(25 * 60_000L, pair.activity.duration)
        assertEquals(5L, pair.activity.sessionId)
        assertEquals(1, pair.rounds.size)
        assertEquals(2 * 60_000L, pair.rounds[0].duration)
    }
}
