package com.warrior.domain.progress

import com.warrior.domain.progress.model.HomeProgress
import com.warrior.domain.progress.week.WeekBoundaryProvider
import com.warrior.domain.training.model.TrainingSession
import javax.inject.Inject

/**
 * Orchestrates the pure [ProgressEngine] with the central week rule: builds
 * the full Home snapshot (this week, last week for deltas, streak, records,
 * recent sessions) from one raw session list, so every number on screen comes
 * from the same consistent pass (Architecture v2.1 §13.3 — Room is the only
 * source of truth).
 */
class ProgressCalculator @Inject constructor(
    private val weeks: WeekBoundaryProvider,
) {

    fun homeProgress(sessions: List<TrainingSession>, nowMillis: Long): HomeProgress {
        val thisWeekStart = weeks.startOfWeek(nowMillis)
        val thisWeekEnd = weeks.startOfNextWeek(thisWeekStart)
        val lastWeekStart = weeks.startOfPreviousWeek(thisWeekStart)
        return HomeProgress(
            thisWeek = ProgressEngine.weeklyMetrics(sessions, thisWeekStart, thisWeekEnd),
            lastWeek = ProgressEngine.weeklyMetrics(sessions, lastWeekStart, thisWeekStart),
            streakWeeks = ProgressEngine.streakWeeks(sessions, thisWeekStart, weeks),
            records = ProgressEngine.personalRecords(sessions),
            recentSessions = ProgressEngine.recentSessions(sessions, RECENT_LIMIT),
        )
    }

    companion object {
        const val RECENT_LIMIT = 3
    }
}
