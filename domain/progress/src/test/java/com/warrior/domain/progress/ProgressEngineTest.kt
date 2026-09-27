package com.warrior.domain.progress

import com.warrior.domain.progress.week.WeekBoundaryProvider
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Acceptance: every metric equals a hand-computed fixture (EXECUTION-PLAN
 * Phase 7). Nothing here touches a database — the engine is pure.
 */
class ProgressEngineTest {

    private val weeks = WeekBoundaryProvider(UTC)

    // Week under test: Sat 2026-09-19 00:00 UTC .. Sat 2026-09-26 00:00 UTC.
    private val weekStart = instant(UTC, 2026, 9, 19)
    private val weekEnd = instant(UTC, 2026, 9, 26)

    private val sat = instant(UTC, 2026, 9, 19)
    private val mon = instant(UTC, 2026, 9, 21)
    private val prevSat = instant(UTC, 2026, 9, 12)
    private val prev2Sat = instant(UTC, 2026, 9, 5)
    private val nextSat = instant(UTC, 2026, 9, 26)

    /**
     * Hand-computed fixture:
     * S1 Sat: Heavy Bag 30m (3 rounds) + Mitt Work 20m (2 rounds) -> 50m, 5 rounds, overall 8
     * S2 Mon: Cardio 40m (0 rounds)                              -> 40m, 0 rounds, overall 7
     * S3 Mon (later createdAt): Sparring 25m (4 rounds)          -> 25m, 4 rounds, overall 8
     * Totals: 3 sessions, 115m, 9 rounds, avg intensity (8+7+8)/3 = 7.67 -> 8,
     *         2 training days, avg session 115/3 = 38.33 -> 38m.
     */
    private val thisWeekSessions = listOf(
        session(
            id = 1,
            date = sat,
            overallIntensity = 8,
            activities = listOf(
                activity(WorkoutType.HEAVY_BAG, 30, FocusArea.POWER, rounds = 3, intensity = 8),
                activity(WorkoutType.MITT_WORK, 20, FocusArea.TIMING, rounds = 2),
            ),
        ),
        session(
            id = 2,
            date = mon,
            overallIntensity = 7,
            activities = listOf(activity(WorkoutType.CARDIO, 40, FocusArea.CONDITIONING)),
        ),
        session(
            id = 3,
            date = mon,
            overallIntensity = 8,
            createdAt = mon + 3_600_000L,
            activities = listOf(activity(WorkoutType.SPARRING, 25, FocusArea.DEFENSE, rounds = 4, intensity = 9)),
        ),
    )

    @Test
    fun weeklyMetrics_matchesHandComputedFixture() {
        val metrics = ProgressEngine.weeklyMetrics(thisWeekSessions, weekStart, weekEnd)

        assertEquals(3, metrics.sessionCount)
        assertEquals(115L, metrics.trainingMinutes)
        assertEquals(9, metrics.totalRounds)
        assertEquals(8, metrics.averageIntensity)
        assertEquals(2, metrics.trainingDays)
        assertEquals(38L, metrics.averageSessionMinutes)
        assertEquals(
            mapOf(
                WorkoutType.CARDIO to 40L,
                WorkoutType.HEAVY_BAG to 30L,
                WorkoutType.MITT_WORK to 20L,
                WorkoutType.SPARRING to 25L,
            ),
            metrics.workoutDistribution,
        )
        assertEquals(
            mapOf(
                FocusArea.POWER to 30L,
                FocusArea.TIMING to 20L,
                FocusArea.CONDITIONING to 40L,
                FocusArea.DEFENSE to 25L,
            ),
            metrics.focusDistribution,
        )
    }

    @Test
    fun weeklyMetrics_averageIntensityRoundsHalfUp() {
        val sessions = listOf(
            session(id = 1, date = sat, overallIntensity = 7, activities = listOf(activity(WorkoutType.CARDIO, 10, FocusArea.POWER))),
            session(id = 2, date = mon, overallIntensity = 8, activities = listOf(activity(WorkoutType.CARDIO, 10, FocusArea.POWER))),
        )
        // (7 + 8) / 2 = 7.5 -> 8
        assertEquals(8, ProgressEngine.weeklyMetrics(sessions, weekStart, weekEnd).averageIntensity)
    }

    @Test
    fun weeklyMetrics_weekStartInclusive_weekEndExclusive() {
        val atStart = session(id = 1, date = weekStart, overallIntensity = 5, activities = listOf(activity(WorkoutType.CARDIO, 10, FocusArea.POWER)))
        val atEnd = session(id = 2, date = weekEnd, overallIntensity = 5, activities = listOf(activity(WorkoutType.CARDIO, 10, FocusArea.POWER)))

        val metrics = ProgressEngine.weeklyMetrics(listOf(atStart, atEnd), weekStart, weekEnd)
        assertEquals(1, metrics.sessionCount)
        assertEquals(10L, metrics.trainingMinutes)
    }

    @Test
    fun weeklyMetrics_emptyWeek_isAllZerosAndEmptyDistributions() {
        val metrics = ProgressEngine.weeklyMetrics(emptyList(), weekStart, weekEnd)
        assertEquals(0, metrics.sessionCount)
        assertEquals(0L, metrics.trainingMinutes)
        assertEquals(0, metrics.totalRounds)
        assertEquals(0, metrics.averageIntensity)
        assertEquals(0, metrics.trainingDays)
        assertEquals(0L, metrics.averageSessionMinutes)
        assertTrue(metrics.workoutDistribution.isEmpty())
        assertTrue(metrics.focusDistribution.isEmpty())
        assertTrue(metrics.isEmpty)
    }

    @Test
    fun personalRecords_longestMostRoundsAndTotals() {
        val sessions = listOf(
            session(id = 10, date = instant(UTC, 2026, 9, 5), overallIntensity = 7, activities = listOf(activity(WorkoutType.HEAVY_BAG, 50, FocusArea.POWER, rounds = 5))),
            session(id = 11, date = instant(UTC, 2026, 9, 12), overallIntensity = 6, activities = listOf(activity(WorkoutType.CARDIO, 75, FocusArea.CONDITIONING))),
            session(id = 12, date = sat, overallIntensity = 9, activities = listOf(activity(WorkoutType.SPARRING, 60, FocusArea.DEFENSE, rounds = 8))),
        )
        val records = ProgressEngine.personalRecords(sessions)

        assertEquals(75L, records.longestSessionMinutes)
        assertEquals(11L, records.longestSessionId)
        assertEquals(8, records.mostRoundsInSession)
        assertEquals(12L, records.mostRoundsSessionId)
        assertEquals(185L, records.totalTrainingMinutes) // 50 + 75 + 60
        assertEquals(3, records.totalSessions)
        assertEquals(13, records.totalRounds) // 5 + 0 + 8
    }

    @Test
    fun personalRecords_tiesResolveToTheMostRecentSession() {
        val sessions = listOf(
            session(id = 11, date = instant(UTC, 2026, 9, 12), overallIntensity = 6, activities = listOf(activity(WorkoutType.CARDIO, 75, FocusArea.CONDITIONING))),
            session(id = 13, date = instant(UTC, 2026, 9, 20), overallIntensity = 6, activities = listOf(activity(WorkoutType.CARDIO, 75, FocusArea.CONDITIONING))),
            session(id = 12, date = sat, overallIntensity = 9, activities = listOf(activity(WorkoutType.SPARRING, 60, FocusArea.DEFENSE, rounds = 8))),
            session(id = 14, date = mon, overallIntensity = 8, activities = listOf(activity(WorkoutType.MITT_WORK, 30, FocusArea.TIMING, rounds = 8))),
        )
        val records = ProgressEngine.personalRecords(sessions)

        assertEquals(75L, records.longestSessionMinutes)
        assertEquals(13L, records.longestSessionId) // later date wins the 75m tie
        assertEquals(8, records.mostRoundsInSession)
        assertEquals(14L, records.mostRoundsSessionId) // later date wins the 8-round tie
    }

    @Test
    fun personalRecords_emptyHistory_isEmpty() {
        val records = ProgressEngine.personalRecords(emptyList())
        assertTrue(records.isEmpty)
        assertEquals(0L, records.longestSessionMinutes)
        assertEquals(null, records.longestSessionId)
        assertEquals(0, records.mostRoundsInSession)
    }

    @Test
    fun recentSessions_sortedByDateThenCreatedAtDesc_cappedAtLimit() {
        val sessions = listOf(
            session(id = 21, date = sat, overallIntensity = 7, createdAt = 5, activities = listOf(activity(WorkoutType.CARDIO, 10, FocusArea.POWER))),
            session(id = 22, date = mon, overallIntensity = 7, createdAt = 1, activities = listOf(activity(WorkoutType.CARDIO, 10, FocusArea.POWER))),
            session(id = 23, date = mon, overallIntensity = 7, createdAt = 9, activities = listOf(activity(WorkoutType.CARDIO, 10, FocusArea.POWER))),
            session(id = 24, date = instant(UTC, 2026, 9, 20), overallIntensity = 7, createdAt = 3, activities = listOf(activity(WorkoutType.CARDIO, 10, FocusArea.POWER))),
            session(id = 25, date = instant(UTC, 2026, 9, 18), overallIntensity = 7, createdAt = 7, activities = listOf(activity(WorkoutType.CARDIO, 10, FocusArea.POWER))),
        )
        assertEquals(
            listOf(23L, 22L, 24L),
            ProgressEngine.recentSessions(sessions, limit = 3).map { it.id },
        )
    }

    @Test
    fun streakWeeks_countsConsecutiveWeeksIncludingTheCurrentOne() {
        val now = instant(UTC, 2026, 9, 23, hour = 12)
        val thisWeekStart = weeks.startOfWeek(now)
        val card = listOf(activity(WorkoutType.CARDIO, 10, FocusArea.POWER))
        val sessions = listOf(
            // week of Sep 19
            session(id = 1, date = sat, overallIntensity = 7, activities = card),
            // week of Sep 12
            session(id = 2, date = instant(UTC, 2026, 9, 13), overallIntensity = 7, activities = card),
            // week of Sep 5
            session(id = 3, date = instant(UTC, 2026, 9, 6), overallIntensity = 7, activities = card),
        )
        assertEquals(3, ProgressEngine.streakWeeks(sessions, thisWeekStart, weeks))
    }

    @Test
    fun streakWeeks_inProgressCurrentWeekDoesNotBreakTheStreak() {
        val now = instant(UTC, 2026, 9, 23, hour = 12) // current week still empty
        val thisWeekStart = weeks.startOfWeek(now)
        val card = listOf(activity(WorkoutType.CARDIO, 10, FocusArea.POWER))
        val sessions = listOf(
            session(id = 2, date = instant(UTC, 2026, 9, 13), overallIntensity = 7, activities = card),
            session(id = 3, date = instant(UTC, 2026, 9, 6), overallIntensity = 7, activities = card),
        )
        assertEquals(2, ProgressEngine.streakWeeks(sessions, thisWeekStart, weeks))
    }

    @Test
    fun streakWeeks_gapWeekEndsTheStreak() {
        val now = instant(UTC, 2026, 9, 23, hour = 12)
        val thisWeekStart = weeks.startOfWeek(now)
        val card = listOf(activity(WorkoutType.CARDIO, 10, FocusArea.POWER))
        // Trained this week and two weeks ago, skipped the week of Sep 12:
        val sessions = listOf(
            session(id = 1, date = sat, overallIntensity = 7, activities = card),
            session(id = 3, date = instant(UTC, 2026, 9, 6), overallIntensity = 7, activities = card),
        )
        assertEquals(1, ProgressEngine.streakWeeks(sessions, thisWeekStart, weeks))
    }

    @Test
    fun streakWeeks_noHistory_isZero() {
        val now = instant(UTC, 2026, 9, 23, hour = 12)
        assertEquals(0, ProgressEngine.streakWeeks(emptyList(), weeks.startOfWeek(now), weeks))
    }

    @Test
    fun streakWeeks_longerChainAcrossFourWeeks() {
        val now = instant(UTC, 2026, 9, 23, hour = 12)
        val thisWeekStart = weeks.startOfWeek(now)
        val card = listOf(activity(WorkoutType.CARDIO, 10, FocusArea.POWER))
        val sessions = listOf(
            session(id = 1, date = sat, overallIntensity = 7, activities = card),
            session(id = 2, date = prevSat, overallIntensity = 7, activities = card),
            session(id = 3, date = prev2Sat, overallIntensity = 7, activities = card),
            session(id = 4, date = instant(UTC, 2026, 8, 30), overallIntensity = 7, activities = card),
        )
        assertEquals(4, ProgressEngine.streakWeeks(sessions, thisWeekStart, weeks))
    }
}
