package com.warrior.domain.training.validation

import com.warrior.domain.training.model.Round
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutActivity
import kotlin.time.Duration

/** Thrown by use cases when domain validation fails (Architecture Rule: validation in Domain). */
class ValidationException(val messages: List<String>) :
    IllegalArgumentException(messages.joinToString(separator = "; "))

/**
 * Domain validation rules (DB v4 §6 mirrors these as the last line of defense).
 * UI validation is a convenience; these rules are authoritative.
 */
object TrainingValidation {

    fun validate(session: TrainingSession): List<String> {
        val errors = mutableListOf<String>()
        if (session.overallIntensity !in 1..10) {
            errors += "overallIntensity must be in 1..10, was ${session.overallIntensity}"
        }
        if (session.activities.isEmpty()) {
            errors += "a session must contain at least one activity"
        }
        session.activities.forEachIndexed { index, activity ->
            validateActivity(activity).forEach { errors += "activities[$index]: $it" }
        }
        return errors
    }

    fun validateActivity(activity: WorkoutActivity): List<String> {
        val errors = mutableListOf<String>()
        if (activity.duration <= Duration.ZERO) {
            errors += "duration must be > 0, was ${activity.duration}"
        }
        if (activity.intensity !in 1..10) {
            errors += "intensity must be in 1..10, was ${activity.intensity}"
        }
        activity.rounds.forEachIndexed { index, round ->
            validateRound(round).forEach { errors += "rounds[$index]: $it" }
        }
        return errors
    }

    fun validateRound(round: Round): List<String> {
        val errors = mutableListOf<String>()
        if (round.roundNumber <= 0) {
            errors += "roundNumber must be > 0, was ${round.roundNumber}"
        }
        if (round.duration <= Duration.ZERO) {
            errors += "duration must be > 0, was ${round.duration}"
        }
        if (round.restDuration < Duration.ZERO) {
            errors += "restDuration must be >= 0, was ${round.restDuration}"
        }
        if (round.intensity !in 1..10) {
            errors += "intensity must be in 1..10, was ${round.intensity}"
        }
        return errors
    }

    fun requireValid(session: TrainingSession) {
        val errors = validate(session)
        if (errors.isNotEmpty()) throw ValidationException(errors)
    }
}
