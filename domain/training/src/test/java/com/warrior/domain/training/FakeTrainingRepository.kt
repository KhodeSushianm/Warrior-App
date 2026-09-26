package com.warrior.domain.training

import com.warrior.domain.training.model.TrainingSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory repository fake for pure-JVM use case tests (Architecture Rule 8). */
class FakeTrainingRepository : TrainingRepository {

    private val sessions = mutableMapOf<Long, TrainingSession>()
    private val state = MutableStateFlow<List<TrainingSession>>(emptyList())
    private var nextId = 1L

    override fun observeSessions(userId: Long): Flow<List<TrainingSession>> =
        state.map { all ->
            all.filter { it.userId == userId }.sortedByDescending { it.date }
        }

    override suspend fun getSession(userId: Long, sessionId: Long): TrainingSession? =
        sessions[sessionId]?.takeIf { it.userId == userId }

    override suspend fun createSession(session: TrainingSession): Long {
        val id = nextId++
        sessions[id] = session.copy(id = id)
        publish()
        return id
    }

    override suspend fun updateSession(session: TrainingSession) {
        check(sessions.containsKey(session.id)) { "session ${session.id} does not exist" }
        sessions[session.id] = session
        publish()
    }

    override suspend fun deleteSession(userId: Long, sessionId: Long): Boolean {
        val existing = sessions[sessionId] ?: return false
        if (existing.userId != userId) return false
        sessions.remove(sessionId)
        publish()
        return true
    }

    private fun publish() {
        state.value = sessions.values.toList()
    }
}
