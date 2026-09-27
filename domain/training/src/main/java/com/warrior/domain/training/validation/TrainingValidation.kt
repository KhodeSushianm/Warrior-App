package com.warrior.domain.training.validation

import com.warrior.domain.training.model.Round
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutActivity
import kotlin.time.Duration

/**
 * Stable machine-readable identifiers for training errors the UI can show
 * (Phase 9, i18n-ready): feature layers map codes to string resources, so no
 * domain message text is ever rendered directly. Messages stay for logs.
 */
enum class TrainingErrorCode {
    OVERALL_INTENSITY_RANGE,
    NO_ACTIVITIES,
    ACTIVITY_DURATION_POSITIVE,
    ACTIVITY_INTENSITY_RANGE,
    ROUND_NUMBER_POSITIVE,
    ROUND_DURATION_POSITIVE,
    ROUND_REST_NON_NEGATIVE,
    ROUND_INTENSITY_RANGE,

    // Flow-level codes (Phase 9): emitted by feature ViewModels, not by the
    // validation rules above. Kept in the same stable enum so UI mapping stays single-source.
    SESSION_NOT_FOUND,
    NOT_SIGNED_IN,
    SAVE_FAILED,
    UNEXPECTED,
}

/** Thrown by use cases when domain validation fails (Architecture Rule: validation in Domain). */
class ValidationException(
    val messages: List<String>,
    val codes: List<TrainingErrorCode> = emptyList(),
) : IllegalArgumentException(messages.joinToString(separator = "; "))

/**
 * Domain validation rules (DB v4 §6 mirrors these as the last line of defense).
 * UI validation is a convenience; these rules are authoritative.
 */
object TrainingValidation {

    fun validate(session: TrainingSession): List<String> = collect(session).map { it.first }

    fun validateWithCodes(session: TrainingSession): List<Pair<String, TrainingErrorCode>> = collect(session)

    fun validateActivity(activity: WorkoutActivity): List<String> = collectActivity(activity).map { it.first }

    fun validateRound(round: Round): List<String> = collectRound(round).map { it.first }

    fun requireValid(session: TrainingSession) {
        val errors = collect(session)
        if (errors.isNotEmpty()) {
            throw ValidationException(
                messages = errors.map { it.first },
                codes = errors.map { it.second },
            )
        }
    }

    private fun collect(session: TrainingSession): List<Pair<String, TrainingErrorCode>> {
        val errors = mutableListOf<Pair<String, TrainingErrorCode>>()
        if (session.overallIntensity !in 1..10) {
            errors += "overallIntensity must be in 1..10, was ${session.overallIntensity}" to
                TrainingErrorCode.OVERALL_INTENSITY_RANGE
        }
        if (session.activities.isEmpty()) {
            errors += "a session must contain at least one activity" to TrainingErrorCode.NO_ACTIVITIES
        }
        session.activities.forEachIndexed { index, activity ->
            collectActivity(activity).forEach { (message, code) ->
                errors += "activities[$index]: $message" to code
            }
        }
        return errors
    }

    private fun collectActivity(activity: WorkoutActivity): List<Pair<String, TrainingErrorCode>> {
        val errors = mutableListOf<Pair<String, TrainingErrorCode>>()
        if (activity.duration <= Duration.ZERO) {
            errors += "duration must be > 0, was ${activity.duration}" to TrainingErrorCode.ACTIVITY_DURATION_POSITIVE
        }
        if (activity.intensity !in 1..10) {
            errors += "intensity must be in 1..10, was ${activity.intensity}" to TrainingErrorCode.ACTIVITY_INTENSITY_RANGE
        }
        activity.rounds.forEachIndexed { index, round ->
            collectRound(round).forEach { (message, code) ->
                errors += "rounds[$index]: $message" to code
            }
        }
        return errors
    }

    private fun collectRound(round: Round): List<Pair<String, TrainingErrorCode>> {
        val errors = mutableListOf<Pair<String, TrainingErrorCode>>()
        if (round.roundNumber <= 0) {
            errors += "roundNumber must be > 0, was ${round.roundNumber}" to TrainingErrorCode.ROUND_NUMBER_POSITIVE
        }
        if (round.duration <= Duration.ZERO) {
            errors += "duration must be > 0, was ${round.duration}" to TrainingErrorCode.ROUND_DURATION_POSITIVE
        }
        if (round.restDuration < Duration.ZERO) {
            errors += "restDuration must be >= 0, was ${round.restDuration}" to TrainingErrorCode.ROUND_REST_NON_NEGATIVE
        }
        if (round.intensity !in 1..10) {
            errors += "intensity must be in 1..10, was ${round.intensity}" to TrainingErrorCode.ROUND_INTENSITY_RANGE
        }
        return errors
    }
}
