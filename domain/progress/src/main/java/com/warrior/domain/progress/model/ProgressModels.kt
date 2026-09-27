package com.warrior.domain.progress.model

import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutType

/**
 * Everything the Progress Engine derives for one week (Architecture v2.1
 * §13.1). Computed on the fly from raw Room data on every emission — never
 * stored (§13.4). Distributions are *training minutes* per key, in stable
 * enum order, zero entries removed.
 */
data class WeeklyMetrics(
    val weekStart: Long,
    val weekEndExclusive: Long,
    val sessionCount: Int = 0,
    val totalRounds: Int = 0,
    val trainingMinutes: Long = 0,
    val averageIntensity: Int = 0,
    val trainingDays: Int = 0,
    val averageSessionMinutes: Long = 0,
    val workoutDistribution: Map<WorkoutType, Long> = emptyMap(),
    val focusDistribution: Map<FocusArea, Long> = emptyMap(),
) {
    val isEmpty: Boolean
        get() = sessionCount == 0
}

/**
 * All-time personal records (Architecture v2.1 §13 — "Longest Session, Most
 * Rounds"; EXECUTION-PLAN Phase 7 adds total training time). Ties are broken
 * toward the most recent session (date desc, createdAt desc).
 */
data class PersonalRecords(
    val longestSessionMinutes: Long = 0,
    val longestSessionId: Long? = null,
    val mostRoundsInSession: Int = 0,
    val mostRoundsSessionId: Long? = null,
    val totalTrainingMinutes: Long = 0,
    val totalSessions: Int = 0,
    val totalRounds: Int = 0,
) {
    val isEmpty: Boolean
        get() = totalSessions == 0
}

/** Single consistent snapshot for the Home dashboard, derived in one pass. */
data class HomeProgress(
    val thisWeek: WeeklyMetrics,
    val lastWeek: WeeklyMetrics,
    val streakWeeks: Int,
    val records: PersonalRecords,
    val recentSessions: List<TrainingSession>,
)
