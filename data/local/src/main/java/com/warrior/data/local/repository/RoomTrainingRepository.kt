package com.warrior.data.local.repository

import com.warrior.data.local.dao.RoundDao
import com.warrior.data.local.dao.TrainingSessionDao
import com.warrior.data.local.dao.WorkoutActivityDao
import com.warrior.data.local.mapper.SessionMapper
import com.warrior.domain.training.TrainingRepository
import com.warrior.domain.training.model.TrainingSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room implementation of [TrainingRepository] (Architecture v2.1 §9).
 * This is the only place DAOs are called; composite writes go through the
 * single transactional DAO entry points (Architecture Rule 7).
 */
@Singleton
class RoomTrainingRepository @Inject constructor(
    private val sessionDao: TrainingSessionDao,
    private val activityDao: WorkoutActivityDao,
    private val roundDao: RoundDao,
) : TrainingRepository {

    override fun observeSessions(userId: Long): Flow<List<TrainingSession>> =
        sessionDao.observeSessions(userId).map { headers ->
            headers.map { loadAggregate(userId, it.id) ?: error("session ${it.id} vanished while observing") }
        }

    override suspend fun getSession(userId: Long, sessionId: Long): TrainingSession? =
        loadAggregate(userId, sessionId)

    override suspend fun createSession(session: TrainingSession): Long =
        sessionDao.insertFullSession(
            SessionMapper.toEntity(session).copy(id = 0),
            session.activities.map { SessionMapper.toActivityWithRounds(it, sessionId = 0) },
        )

    override suspend fun updateSession(session: TrainingSession) {
        sessionDao.updateFullSession(
            SessionMapper.toEntity(session),
            session.activities.map { SessionMapper.toActivityWithRounds(it, sessionId = session.id) },
        )
    }

    override suspend fun deleteSession(userId: Long, sessionId: Long): Boolean {
        if (sessionDao.getSession(userId, sessionId) == null) return false
        sessionDao.deleteSession(userId, sessionId)
        return true
    }

    private suspend fun loadAggregate(userId: Long, sessionId: Long): TrainingSession? {
        val header = sessionDao.getSession(userId, sessionId) ?: return null
        val activities = activityDao.getActivities(userId, sessionId)
        val roundsByActivityId = activities.associate { activity ->
            activity.id to roundDao.getRounds(userId, activity.id)
        }
        return SessionMapper.toDomain(header, activities, roundsByActivityId)
    }
}
