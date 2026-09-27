package com.warrior.domain.progress

import com.warrior.domain.progress.model.HomeProgress
import com.warrior.domain.progress.model.ProgressSnapshot
import com.warrior.domain.progress.week.WeekBoundaryProvider
import com.warrior.domain.training.model.TrainingSession
import javax.inject.Inject

/**
 * Orchestrates the pure [ProgressEngine] with the central week rule: builds
 * the full Home and Progress snapshots from one raw session list, so every
 * number on screen comes from the same consistent pass (Architecture v2.1
 * §13.3 — Room is the only source of truth).
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
            heatmap = ProgressEngine.monthsHeatmap(sessions, nowMillis, weeks.zone, HEATMAP_MONTHS),
        )
    }

    /** Progress screen (Phase 8): comparison + 8-week volume + all-time distributions. */
    fun progressScreen(sessions: List<TrainingSession>, nowMillis: Long): ProgressSnapshot {
        val thisWeekStart = weeks.startOfWeek(nowMillis)
        val thisWeekEnd = weeks.startOfNextWeek(thisWeekStart)
        val lastWeekStart = weeks.startOfPreviousWeek(thisWeekStart)
        return ProgressSnapshot(
            thisWeek = ProgressEngine.weeklyMetrics(sessions, thisWeekStart, thisWeekEnd),
            lastWeek = ProgressEngine.weeklyMetrics(sessions, lastWeekStart, thisWeekStart),
            volumeSeries = ProgressEngine.volumeSeries(sessions, thisWeekStart, VOLUME_WEEKS, weeks),
            workoutDistribution = ProgressEngine.workoutDistribution(sessions),
            topFocusAreas = ProgressEngine.topFocusAreas(sessions, FOCUS_LIMIT),
        )
    }

    companion object {
        const val RECENT_LIMIT = 3
        const val HEATMAP_MONTHS = 3
        const val VOLUME_WEEKS = 8
        const val FOCUS_LIMIT = 4
    }
}
