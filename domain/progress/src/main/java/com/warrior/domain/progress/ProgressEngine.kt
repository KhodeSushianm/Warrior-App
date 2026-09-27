package com.warrior.domain.progress

import com.warrior.domain.progress.model.PersonalRecords
import com.warrior.domain.progress.model.WeeklyMetrics
import com.warrior.domain.progress.week.WeekBoundaryProvider
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutType
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Pure Progress Engine (Architecture v2.1 §13): deterministic functions over
 * raw session data, zero Android dependencies, nothing persisted (§13.4).
 * A session belongs to a week when its `date` (local-day midnight, DB v4 §9)
 * falls in `[weekStart, weekEndExclusive)` — both bounds are local midnights
 * produced by [WeekBoundaryProvider], so the comparison is exact.
 */
object ProgressEngine {

    fun weeklyMetrics(
        sessions: List<TrainingSession>,
        weekStart: Long,
        weekEndExclusive: Long,
    ): WeeklyMetrics {
        val inWeek = sessions.filter { it.date in weekStart until weekEndExclusive }
        val sessionCount = inWeek.size
        val trainingMinutes = inWeek.sumOf { it.totalDuration.inWholeMinutes }
        val workoutDistribution = mutableMapOf<WorkoutType, Long>()
        val focusDistribution = mutableMapOf<FocusArea, Long>()
        inWeek.forEach { session ->
            session.activities.forEach { activity ->
                val minutes = activity.duration.inWholeMinutes
                workoutDistribution.merge(activity.type, minutes, Long::plus)
                focusDistribution.merge(activity.focusArea, minutes, Long::plus)
            }
        }
        return WeeklyMetrics(
            weekStart = weekStart,
            weekEndExclusive = weekEndExclusive,
            sessionCount = sessionCount,
            totalRounds = inWeek.sumOf { it.totalRounds },
            trainingMinutes = trainingMinutes,
            averageIntensity = if (sessionCount == 0) {
                0
            } else {
                (inWeek.sumOf { it.overallIntensity }.toDouble() / sessionCount).roundToInt()
            },
            trainingDays = inWeek.map { it.date }.distinct().size,
            averageSessionMinutes = if (sessionCount == 0) {
                0
            } else {
                (trainingMinutes.toDouble() / sessionCount).roundToLong()
            },
            // Stable enum order for UI legends/charts; zero entries removed.
            workoutDistribution = WorkoutType.entries.associateWith { workoutDistribution[it] ?: 0L }
                .filterValues { it > 0 },
            focusDistribution = FocusArea.entries.associateWith { focusDistribution[it] ?: 0L }
                .filterValues { it > 0 },
        )
    }

    fun personalRecords(sessions: List<TrainingSession>): PersonalRecords {
        if (sessions.isEmpty()) return PersonalRecords()
        // Most recent first so ties resolve toward the newer session.
        val ordered = sessions.sortedWith(compareByDescending<TrainingSession> { it.date }.thenByDescending { it.createdAt })
        var longest: TrainingSession = ordered[0]
        var mostRounds: TrainingSession = ordered[0]
        ordered.forEach { session ->
            if (session.totalDuration > longest.totalDuration) longest = session
            if (session.totalRounds > mostRounds.totalRounds) mostRounds = session
        }
        return PersonalRecords(
            longestSessionMinutes = longest.totalDuration.inWholeMinutes,
            longestSessionId = longest.id,
            mostRoundsInSession = mostRounds.totalRounds,
            mostRoundsSessionId = mostRounds.id,
            totalTrainingMinutes = sessions.sumOf { it.totalDuration.inWholeMinutes },
            totalSessions = sessions.size,
            totalRounds = sessions.sumOf { it.totalRounds },
        )
    }

    /** Newest first (date desc, createdAt desc), capped at [limit]. */
    fun recentSessions(sessions: List<TrainingSession>, limit: Int): List<TrainingSession> =
        sessions
            .sortedWith(compareByDescending<TrainingSession> { it.date }.thenByDescending { it.createdAt })
            .take(limit)

    /**
     * Consecutive Sat→Fri weeks with at least one session, ending at the
     * current week. An empty current week does *not* break the streak (it is
     * still in progress) — counting then starts from the previous week.
     */
    fun streakWeeks(
        sessions: List<TrainingSession>,
        thisWeekStart: Long,
        weeks: WeekBoundaryProvider,
    ): Int {
        val trainedWeekStarts = sessions.map { weeks.startOfWeek(it.date) }.toSet()
        var cursor = if (thisWeekStart in trainedWeekStarts) {
            thisWeekStart
        } else {
            weeks.startOfPreviousWeek(thisWeekStart)
        }
        var streak = 0
        while (cursor in trainedWeekStarts) {
            streak++
            cursor = weeks.startOfPreviousWeek(cursor)
        }
        return streak
    }
}
