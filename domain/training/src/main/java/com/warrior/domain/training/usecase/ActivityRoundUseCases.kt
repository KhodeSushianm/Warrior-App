package com.warrior.domain.training.usecase

import com.warrior.domain.training.TrainingRepository
import com.warrior.domain.training.model.Round
import com.warrior.domain.training.model.WorkoutActivity
import com.warrior.domain.training.validation.TrainingValidation
import javax.inject.Inject

/**
 * Activity/Round edits operate on the session aggregate and are persisted
 * atomically through [TrainingRepository.updateSession] (Architecture Rule 7).
 */
class AddWorkoutActivity @Inject constructor(private val repository: TrainingRepository) {
    suspend operator fun invoke(userId: Long, sessionId: Long, activity: WorkoutActivity): Result<Unit> =
        runCatching {
            val session = requireSession(repository, userId, sessionId)
            val updated = session.copy(
                activities = session.activities + activity.copy(id = 0, sessionId = sessionId),
            )
            TrainingValidation.requireValid(updated)
            repository.updateSession(updated)
        }
}

class UpdateWorkoutActivity @Inject constructor(private val repository: TrainingRepository) {
    suspend operator fun invoke(userId: Long, sessionId: Long, activity: WorkoutActivity): Result<Unit> =
        runCatching {
            val session = requireSession(repository, userId, sessionId)
            require(session.activities.any { it.id == activity.id }) {
                "activity ${activity.id} does not belong to session $sessionId"
            }
            val updated = session.copy(
                activities = session.activities.map {
                    if (it.id == activity.id) activity.copy(id = it.id, sessionId = sessionId, rounds = it.rounds) else it
                },
            )
            TrainingValidation.requireValid(updated)
            repository.updateSession(updated)
        }
}

class DeleteWorkoutActivity @Inject constructor(private val repository: TrainingRepository) {
    suspend operator fun invoke(userId: Long, sessionId: Long, activityId: Long): Result<Unit> =
        runCatching {
            val session = requireSession(repository, userId, sessionId)
            val updated = session.copy(activities = session.activities.filterNot { it.id == activityId })
            TrainingValidation.requireValid(updated)
            repository.updateSession(updated)
        }
}

class AddRound @Inject constructor(private val repository: TrainingRepository) {
    suspend operator fun invoke(userId: Long, sessionId: Long, activityId: Long, round: Round): Result<Unit> =
        runCatching {
            val session = requireSession(repository, userId, sessionId)
            val updated = session.copy(
                activities = session.activities.map { activity ->
                    if (activity.id == activityId) {
                        val nextNumber = (activity.rounds.maxOfOrNull { it.roundNumber } ?: 0) + 1
                        activity.copy(
                            rounds = activity.rounds + round.copy(id = 0, activityId = activityId, roundNumber = nextNumber),
                        )
                    } else {
                        activity
                    }
                },
            )
            require(updated.activities.any { it.id == activityId }) {
                "activity $activityId does not belong to session $sessionId"
            }
            TrainingValidation.requireValid(updated)
            repository.updateSession(updated)
        }
}

class UpdateRound @Inject constructor(private val repository: TrainingRepository) {
    suspend operator fun invoke(userId: Long, sessionId: Long, activityId: Long, round: Round): Result<Unit> =
        runCatching {
            val session = requireSession(repository, userId, sessionId)
            val updated = session.copy(
                activities = session.activities.map { activity ->
                    if (activity.id == activityId) {
                        activity.copy(
                            rounds = activity.rounds.map {
                                if (it.id == round.id) round.copy(id = it.id, activityId = activityId, roundNumber = it.roundNumber) else it
                            },
                        )
                    } else {
                        activity
                    }
                },
            )
            TrainingValidation.requireValid(updated)
            repository.updateSession(updated)
        }
}

class DeleteRound @Inject constructor(private val repository: TrainingRepository) {
    suspend operator fun invoke(userId: Long, sessionId: Long, activityId: Long, roundId: Long): Result<Unit> =
        runCatching {
            val session = requireSession(repository, userId, sessionId)
            val updated = session.copy(
                activities = session.activities.map { activity ->
                    if (activity.id == activityId) {
                        activity.copy(rounds = activity.rounds.filterNot { it.id == roundId })
                    } else {
                        activity
                    }
                },
            )
            TrainingValidation.requireValid(updated)
            repository.updateSession(updated)
        }
}

private suspend fun requireSession(
    repository: TrainingRepository,
    userId: Long,
    sessionId: Long,
): com.warrior.domain.training.model.TrainingSession =
    repository.getSession(userId, sessionId)
        ?: throw NoSuchElementException("session $sessionId not found for user $userId")
