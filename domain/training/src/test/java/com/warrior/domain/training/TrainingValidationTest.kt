package com.warrior.domain.training

import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.Round
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutActivity
import com.warrior.domain.training.model.WorkoutType
import com.warrior.domain.training.validation.TrainingErrorCode
import com.warrior.domain.training.validation.TrainingValidation
import com.warrior.domain.training.validation.ValidationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class TrainingValidationTest {

    private fun round(number: Int, rest: Int = 1, intensity: Int = 8) = Round(
        roundNumber = number,
        duration = 3.minutes,
        restDuration = rest.minutes,
        intensity = intensity,
    )

    private fun activity(intensity: Int = 8, durationMinutes: Long = 10, rounds: List<Round> = emptyList()) =
        WorkoutActivity(
            type = WorkoutType.HEAVY_BAG,
            duration = durationMinutes.minutes,
            intensity = intensity,
            focusArea = FocusArea.POWER,
            rounds = rounds,
        )

    private fun session(intensity: Int = 7, activities: List<WorkoutActivity> = listOf(activity())) =
        TrainingSession(
            userId = 1,
            date = 0,
            overallIntensity = intensity,
            overallFeeling = Feeling.GOOD,
            activities = activities,
        )

    @Test
    fun validSession_passes() {
        assertTrue(TrainingValidation.validate(session()).isEmpty())
    }

    @Test
    fun intensityBoundaries_accepted() {
        assertTrue(TrainingValidation.validate(session(intensity = 1)).isEmpty())
        assertTrue(TrainingValidation.validate(session(intensity = 10)).isEmpty())
    }

    @Test
    fun intensityOutOfRange_rejected() {
        assertEquals(1, TrainingValidation.validate(session(intensity = 0)).size)
        assertEquals(1, TrainingValidation.validate(session(intensity = 11)).size)
    }

    @Test
    fun emptyActivities_rejected() {
        val errors = TrainingValidation.validate(session(activities = emptyList()))
        assertTrue(errors.any { it.contains("at least one activity") })
    }

    @Test
    fun zeroDurationActivity_rejected() {
        val errors = TrainingValidation.validate(session(activities = listOf(activity(durationMinutes = 0))))
        assertTrue(errors.any { it.contains("duration must be > 0") })
    }

    @Test
    fun roundRules_enforced() {
        val badNumber = session(activities = listOf(activity(rounds = listOf(round(0)))))
        assertTrue(TrainingValidation.validate(badNumber).any { it.contains("roundNumber") })

        val badRest = session(activities = listOf(activity(rounds = listOf(round(1, rest = -1)))))
        assertTrue(TrainingValidation.validate(badRest).any { it.contains("restDuration") })

        val badIntensity = session(activities = listOf(activity(rounds = listOf(round(1, intensity = 42)))))
        assertTrue(TrainingValidation.validate(badIntensity).any { it.contains("intensity") })

        assertTrue(TrainingValidation.validate(session(activities = listOf(activity(rounds = listOf(round(1, rest = 0)))))).isEmpty())
    }

    @Test
    fun requireValid_throwsWithMatchingCodes() {
        val bad = session(intensity = 0, activities = emptyList())
        val error = runCatching { TrainingValidation.requireValid(bad) }.exceptionOrNull()
        assertTrue(error is ValidationException)
        val codes = (error as ValidationException).codes
        assertTrue(codes.contains(TrainingErrorCode.OVERALL_INTENSITY_RANGE))
        assertTrue(codes.contains(TrainingErrorCode.NO_ACTIVITIES))
        assertEquals(codes.size, error.messages.size)
    }
}
