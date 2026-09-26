package com.warrior.domain.training.usecase

import com.warrior.domain.training.TrainingRepository
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.validation.TrainingValidation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Creates a full session aggregate atomically. Fails with [com.warrior.domain.training.validation.ValidationException] on invalid input. */
class CreateTrainingSession @Inject constructor(private val repository: TrainingRepository) {
    suspend operator fun invoke(userId: Long, session: TrainingSession): Result<Long> = runCatching {
        val scoped = session.copy(userId = userId, id = 0)
        TrainingValidation.requireValid(scoped)
        repository.createSession(scoped)
    }
}

/** Replaces an existing aggregate. Ownership is enforced by the repository. */
class UpdateTrainingSession @Inject constructor(private val repository: TrainingRepository) {
    suspend operator fun invoke(userId: Long, session: TrainingSession): Result<Unit> = runCatching {
        val existing = repository.getSession(userId, session.id)
            ?: throw NoSuchElementException("session ${session.id} not found for user $userId")
        val scoped = session.copy(userId = userId, id = existing.id)
        TrainingValidation.requireValid(scoped)
        repository.updateSession(scoped)
    }
}

class DeleteTrainingSession @Inject constructor(private val repository: TrainingRepository) {
    suspend operator fun invoke(userId: Long, sessionId: Long): Result<Boolean> = runCatching {
        repository.deleteSession(userId, sessionId)
    }
}

class GetTrainingSession @Inject constructor(private val repository: TrainingRepository) {
    suspend operator fun invoke(userId: Long, sessionId: Long): TrainingSession? =
        repository.getSession(userId, sessionId)
}

class GetTrainingHistory @Inject constructor(private val repository: TrainingRepository) {
    operator fun invoke(userId: Long): Flow<List<TrainingSession>> =
        repository.observeSessions(userId)
}
