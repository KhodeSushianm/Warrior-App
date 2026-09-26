package com.warrior.domain.training

import com.warrior.domain.training.model.TrainingSession
import kotlinx.coroutines.flow.Flow

/**
 * Boundary between Domain and Data (Architecture v2.1 §9).
 * ViewModels/Use Cases depend on this interface only; the Room implementation
 * lives in :data:local. Every method is ownership-scoped by [userId].
 */
interface TrainingRepository {

    fun observeSessions(userId: Long): Flow<List<TrainingSession>>

    suspend fun getSession(userId: Long, sessionId: Long): TrainingSession?

    /** Creates the full aggregate atomically; returns the new session id. */
    suspend fun createSession(session: TrainingSession): Long

    /** Replaces the aggregate (activities/rounds included) atomically. */
    suspend fun updateSession(session: TrainingSession)

    /** Returns false when the session does not belong to [userId] or is absent. */
    suspend fun deleteSession(userId: Long, sessionId: Long): Boolean
}
