package com.warrior.domain.progress

import com.warrior.domain.training.TrainingRepository
import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.Round
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutActivity
import com.warrior.domain.training.model.WorkoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.TimeZone
import kotlin.time.Duration.Companion.minutes

/** Deterministic instant builder: explicit wall-clock fields in [zone]. */
internal fun instant(
    zone: TimeZone,
    year: Int,
    month: Int,
    day: Int,
    hour: Int = 0,
    minute: Int = 0,
    second: Int = 0,
    millis: Int = 0,
): Long = Calendar.getInstance(zone)
    .apply {
        clear()
        set(year, month - 1, day, hour, minute, second)
        set(Calendar.MILLISECOND, millis)
    }.timeInMillis

internal val UTC: TimeZone = TimeZone.getTimeZone("UTC")

internal fun activity(
    type: WorkoutType,
    minutes: Long,
    focus: FocusArea,
    rounds: Int = 0,
    intensity: Int = 7,
): WorkoutActivity = WorkoutActivity(
    type = type,
    duration = minutes.minutes,
    intensity = intensity,
    focusArea = focus,
    rounds = (1..rounds).map { number ->
        Round(
            roundNumber = number,
            duration = 3.minutes,
            restDuration = 1.minutes,
            intensity = intensity,
        )
    },
)

internal fun session(
    id: Long,
    date: Long,
    overallIntensity: Int,
    createdAt: Long = date,
    activities: List<WorkoutActivity>,
): TrainingSession = TrainingSession(
    id = id,
    userId = 1,
    date = date,
    createdAt = createdAt,
    overallIntensity = overallIntensity,
    overallFeeling = Feeling.GOOD,
    activities = activities,
)

/** In-memory repository fake (mirrors the feature-module test fakes). */
internal class FakeTrainingRepository : TrainingRepository {

    private val sessions = mutableMapOf<Long, TrainingSession>()
    private val state = MutableStateFlow<List<TrainingSession>>(emptyList())
    private var nextId = 1L

    override fun observeSessions(userId: Long): Flow<List<TrainingSession>> =
        state.map { all -> all.filter { it.userId == userId } }

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
