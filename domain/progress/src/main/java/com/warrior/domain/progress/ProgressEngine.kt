package com.warrior.domain.progress

import com.warrior.domain.progress.model.FocusSlice
import com.warrior.domain.progress.model.MonthHeatmap
import com.warrior.domain.progress.model.PersonalRecords
import com.warrior.domain.progress.model.TrainingHeatmap
import com.warrior.domain.progress.model.WeeklyMetrics
import com.warrior.domain.progress.model.WeeklyVolume
import com.warrior.domain.progress.week.WeekBoundaryProvider
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutType
import java.util.Calendar
import java.util.TimeZone
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
        val workout = minutesByWorkoutType(inWeek)
        val focus = minutesByFocusArea(inWeek)
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
            workoutDistribution = WorkoutType.entries.associateWith { workout[it] ?: 0L }
                .filterValues { it > 0 },
            focusDistribution = FocusArea.entries.associateWith { focus[it] ?: 0L }
                .filterValues { it > 0 },
        )
    }

    fun personalRecords(sessions: List<TrainingSession>): PersonalRecords {
        if (sessions.isEmpty()) return PersonalRecords()
        // Most recent first so ties resolve toward the newer session.
        val ordered = sessions.sortedWith(
            compareByDescending<TrainingSession> { it.date }.thenByDescending { it.createdAt },
        )
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
            .sortedWith(
                compareByDescending<TrainingSession> { it.date }.thenByDescending { it.createdAt },
            )
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

    /**
     * Volume bars for the last [weekCount] weeks ending at the week that
     * starts on [thisWeekStart] — oldest first, last entry = current week
     * (Phase 8 chart input, matches the "last 8 weeks" reference).
     */
    fun volumeSeries(
        sessions: List<TrainingSession>,
        thisWeekStart: Long,
        weekCount: Int,
        weeks: WeekBoundaryProvider,
    ): List<WeeklyVolume> {
        val starts = ArrayList<Long>(weekCount)
        var cursor = thisWeekStart
        repeat(weekCount) {
            starts.add(cursor)
            cursor = weeks.startOfPreviousWeek(cursor)
        }
        starts.reverse() // oldest first
        // One bucket pass through the *central* week rule — no boundary math here.
        val byWeek = sessions.groupBy { weeks.startOfWeek(it.date) }
        return starts.map { weekStart ->
            val inWeek = byWeek[weekStart].orEmpty()
            WeeklyVolume(
                weekStart = weekStart,
                trainingMinutes = inWeek.sumOf { it.totalDuration.inWholeMinutes },
                sessionCount = inWeek.size,
                trainingDays = inWeek.map { it.date }.distinct().size,
            )
        }
    }

    /** All-time training minutes per workout type (enum order, zeros removed). */
    fun workoutDistribution(sessions: List<TrainingSession>): Map<WorkoutType, Long> {
        val totals = minutesByWorkoutType(sessions)
        return WorkoutType.entries.associateWith { totals[it] ?: 0L }
            .filterValues { it > 0 }
    }

    /** All-time top focus areas by minutes, descending, capped at [limit]. */
    fun topFocusAreas(sessions: List<TrainingSession>, limit: Int): List<FocusSlice> =
        minutesByFocusArea(sessions).entries
            .sortedWith(compareByDescending<Map.Entry<FocusArea, Long>> { it.value }.thenBy { it.key.name })
            .take(limit)
            .map { FocusSlice(it.key, it.value) }

    /**
     * Calendar-month dot grids for the Home heatmap (Phase 8): the last
     * [monthCount] months ending with the current one, oldest first. Columns
     * run Saturday→Friday (the same central rule, §13.2), so day 1 sits at
     * [MonthHeatmap.firstDayColumnOffset]. A day is "trained" when at least
     * one session's local date falls on it; only the current month carries
     * [MonthHeatmap.todayDay].
     */
    fun monthsHeatmap(
        sessions: List<TrainingSession>,
        nowMillis: Long,
        zone: TimeZone,
        monthCount: Int = 3,
    ): TrainingHeatmap {
        val now = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth0 = now.get(Calendar.MONTH)
        val todayDay = now.get(Calendar.DAY_OF_MONTH)

        // Bucket trained day-numbers per (year, month0) in one pass.
        val trained = HashMap<Int, MutableSet<Int>>()
        val calendar = Calendar.getInstance(zone)
        sessions.forEach { session ->
            calendar.timeInMillis = session.date
            val key = calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH)
            trained.getOrPut(key) { mutableSetOf() }.add(calendar.get(Calendar.DAY_OF_MONTH))
        }

        val months = (monthCount - 1 downTo 0).map { back ->
            val monthCal = Calendar.getInstance(zone).apply {
                clear()
                set(currentYear, currentMonth0, 1)
                add(Calendar.MONTH, -back)
            }
            val year = monthCal.get(Calendar.YEAR)
            val month0 = monthCal.get(Calendar.MONTH)
            MonthHeatmap(
                year = year,
                month = month0 + 1,
                firstDayColumnOffset = ((monthCal.get(Calendar.DAY_OF_WEEK) - WeekBoundaryProvider.WEEK_START_DAY) + 7) % 7,
                daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH),
                trainedDays = trained[year * 100 + month0]?.toSet() ?: emptySet(),
                todayDay = if (back == 0) todayDay else null,
            )
        }
        return TrainingHeatmap(months)
    }

    private fun minutesByWorkoutType(sessions: List<TrainingSession>): Map<WorkoutType, Long> {
        val totals = mutableMapOf<WorkoutType, Long>()
        sessions.forEach { session ->
            session.activities.forEach { activity ->
                totals.merge(activity.type, activity.duration.inWholeMinutes, Long::plus)
            }
        }
        return totals
    }

    private fun minutesByFocusArea(sessions: List<TrainingSession>): Map<FocusArea, Long> {
        val totals = mutableMapOf<FocusArea, Long>()
        sessions.forEach { session ->
            session.activities.forEach { activity ->
                totals.merge(activity.focusArea, activity.duration.inWholeMinutes, Long::plus)
            }
        }
        return totals
    }
}
