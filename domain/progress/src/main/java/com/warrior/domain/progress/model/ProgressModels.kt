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
    val heatmap: TrainingHeatmap = TrainingHeatmap(emptyList()),
)

/** One training-volume bar (Phase 8): a whole week reduced to chart inputs. */
data class WeeklyVolume(
    val weekStart: Long,
    val trainingMinutes: Long = 0,
    val sessionCount: Int = 0,
    val trainingDays: Int = 0,
)

/** One slice of the all-time focus distribution (Phase 8). */
data class FocusSlice(
    val focus: FocusArea,
    val minutes: Long,
)

/**
 * Everything the Progress screen derives (Phase 8): week-over-week comparison,
 * an 8-week volume series (oldest first, last entry = current week) and
 * all-time distributions — matching the approved ui-preview reference.
 */
data class ProgressSnapshot(
    val thisWeek: WeeklyMetrics,
    val lastWeek: WeeklyMetrics,
    val volumeSeries: List<WeeklyVolume>,
    val workoutDistribution: Map<WorkoutType, Long>,
    val topFocusAreas: List<FocusSlice>,
)

/**
 * One calendar-month block of the Home training-days heatmap. Grid columns run
 * Saturday→Friday (same central rule as §13.2) — [firstDayColumnOffset] is the
 * number of leading blank cells before day 1.
 */
data class MonthHeatmap(
    val year: Int,
    val month: Int,
    val firstDayColumnOffset: Int,
    val daysInMonth: Int,
    val trainedDays: Set<Int> = emptySet(),
    val todayDay: Int? = null,
)

/** The last N calendar months (oldest first, last = current month). */
data class TrainingHeatmap(val months: List<MonthHeatmap>)
