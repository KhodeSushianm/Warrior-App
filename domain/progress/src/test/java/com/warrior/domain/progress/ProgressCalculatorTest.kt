package com.warrior.domain.progress

import com.warrior.domain.progress.week.WeekBoundaryProvider
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Integration of the pure engine with the central week rule: one consistent
 * snapshot per raw session list (EXECUTION-PLAN Phase 7 acceptance).
 */
class ProgressCalculatorTest {

    private val weeks = WeekBoundaryProvider(UTC)
    private val calculator = ProgressCalculator(weeks)

    private val now = instant(UTC, 2026, 9, 23, hour = 12) // Wednesday
    private val sat = instant(UTC, 2026, 9, 19)
    private val mon = instant(UTC, 2026, 9, 21)
    private val prevSat = instant(UTC, 2026, 9, 12)
    private val nextSat = instant(UTC, 2026, 9, 26)

    @Test
    fun homeProgress_buildsOneConsistentSnapshot() {
        val sessions = listOf(
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
            // Last week: 1 session, 60m, 6 rounds.
            session(
                id = 4,
                date = prevSat,
                overallIntensity = 8,
                activities = listOf(activity(WorkoutType.HEAVY_BAG, 60, FocusArea.POWER, rounds = 6)),
            ),
            // Next Saturday (weekEndExclusive): must stay out of "this week".
            session(
                id = 5,
                date = nextSat,
                overallIntensity = 5,
                activities = listOf(activity(WorkoutType.CARDIO, 10, FocusArea.POWER)),
            ),
        )

        val progress = calculator.homeProgress(sessions, now)

        // Week frames.
        assertEquals(sat, progress.thisWeek.weekStart)
        assertEquals(nextSat, progress.thisWeek.weekEndExclusive)
        assertEquals(prevSat, progress.lastWeek.weekStart)
        assertEquals(sat, progress.lastWeek.weekEndExclusive)

        // This week vs last week (deltas the UI shows).
        assertEquals(3, progress.thisWeek.sessionCount)
        assertEquals(115L, progress.thisWeek.trainingMinutes)
        assertEquals(60L, progress.lastWeek.trainingMinutes) // S5 excluded
        assertEquals(1, progress.lastWeek.sessionCount)

        // Streak: week of Sep 19 and week of Sep 12 trained, Sep 5 not -> 2.
        assertEquals(2, progress.streakWeeks)

        // Records span ALL history (including the future-dated S5).
        assertEquals(60L, progress.records.longestSessionMinutes)
        assertEquals(4L, progress.records.longestSessionId)
        assertEquals(6, progress.records.mostRoundsInSession)
        assertEquals(185L, progress.records.totalTrainingMinutes) // 50+40+25+60+10
        assertEquals(5, progress.records.totalSessions)
        assertEquals(15, progress.records.totalRounds) // 5+0+4+6+0

        // Recent: date desc (S5 future-dated sorts first), createdAt desc within a day.
        assertEquals(listOf(5L, 3L, 2L), progress.recentSessions.map { it.id })
    }

    @Test
    fun homeProgress_emptyHistory_rendersEmptyButValidSnapshot() {
        val progress = calculator.homeProgress(emptyList(), now)

        assertEquals(0, progress.thisWeek.sessionCount)
        assertEquals(0, progress.lastWeek.sessionCount)
        assertEquals(0, progress.streakWeeks)
        assertEquals(true, progress.records.isEmpty)
        assertEquals(emptyList<Nothing>(), progress.recentSessions)
        assertEquals(sat, progress.thisWeek.weekStart)
    }
}
