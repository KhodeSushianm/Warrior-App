package com.warrior.domain.progress

import com.warrior.domain.progress.week.WeekBoundaryProvider
import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.Round
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutActivity
import com.warrior.domain.training.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.minutes

/**
 * Phase 8 acceptance: the engine must stay smooth with a year of synthetic
 * data (~5,000 activities). Budgets are deliberately generous (weak CI JVM);
 * on device-class hardware these derivations run in single-digit milliseconds
 * per emission. Correctness invariants are asserted alongside timing so a
 * "fast but wrong" shortcut cannot pass.
 */
class ProgressEnginePerformanceTest {

    private val weeks = WeekBoundaryProvider(UTC)
    private val calculator = ProgressCalculator(weeks)
    private val now = instant(UTC, 2026, 9, 23, hour = 12)

    /** ~[sessionCount] sessions × 5 activities ≈ 5,000 activities over 365 days. */
    private fun syntheticYear(sessionCount: Int = 1_000): List<TrainingSession> {
        val random = Random(42) // deterministic fixtures
        val dayMillis = 86_400_000L
        val oldestDay = instant(UTC, 2025, 9, 24) // 365 days before "now"'s day
        val types = WorkoutType.entries
        val foci = FocusArea.entries
        return (0 until sessionCount).map { index ->
            val date = oldestDay + (index.toLong() * 365 / sessionCount) * dayMillis
            val activityCount = 5
            val activities = (0 until activityCount).map { aIndex ->
                val type = types[random.nextInt(types.size)]
                val rounds = if (type == WorkoutType.CARDIO) {
                    emptyList()
                } else {
                    (1..random.nextInt(2, 7)).map { n ->
                        Round(
                            roundNumber = n,
                            duration = 3.minutes,
                            restDuration = 1.minutes,
                            intensity = 1 + random.nextInt(10),
                        )
                    }
                }
                WorkoutActivity(
                    type = type,
                    duration = (5 + random.nextInt(12) * 5).minutes,
                    intensity = 1 + random.nextInt(10),
                    focusArea = foci[random.nextInt(foci.size)],
                    rounds = rounds,
                )
            }
            TrainingSession(
                id = index + 1L,
                userId = 1,
                date = date,
                createdAt = date + index,
                overallIntensity = 1 + random.nextInt(10),
                overallFeeling = Feeling.GOOD,
                activities = activities,
            )
        }
    }

    @Test
    fun oneYearOfData_snapshotsDeriveFastAndStayConsistent() {
        val sessions = syntheticYear()
        val activityCount = sessions.sumOf { it.activities.size }
        assertTrue("fixture should carry ~5,000 activities, had $activityCount", activityCount >= 4_500)

        // Warm up JIT/class-loading so the measured pass is representative.
        calculator.progressScreen(sessions, now)

        val homeMillis = measureTimeMillis { calculator.homeProgress(sessions, now) }
        val screenMillis = measureTimeMillis { calculator.progressScreen(sessions, now) }

        val home = calculator.homeProgress(sessions, now)
        val screen = calculator.progressScreen(sessions, now)

        // Timing budgets (generous for a 1GiB sandbox JVM):
        println("PERF one-year fixture: home=${homeMillis}ms screen=${screenMillis}ms sessions=${sessions.size}")
        assertTrue("homeProgress took ${homeMillis}ms (budget 1500ms)", homeMillis < 1_500)
        assertTrue("progressScreen took ${screenMillis}ms (budget 1500ms)", screenMillis < 1_500)

        // Invariant 1: distribution total == all-time activity minutes.
        val totalMinutes = sessions.sumOf { it.totalDuration.inWholeMinutes }
        assertEquals(totalMinutes, screen.workoutDistribution.values.sum())
        assertEquals(totalMinutes, home.records.totalTrainingMinutes)

        // Invariant 2: volume series covers exactly its 8-week window.
        val windowStart = screen.volumeSeries.first().weekStart
        val windowEnd = weeks.startOfNextWeek(screen.volumeSeries.last().weekStart)
        val windowMinutes = sessions
            .filter { it.date in windowStart until windowEnd }
            .sumOf { it.totalDuration.inWholeMinutes }
        assertEquals(windowMinutes, screen.volumeSeries.sumOf { it.trainingMinutes })

        // Invariant 3: thisWeek frame matches the Home snapshot (one rule).
        assertEquals(home.thisWeek.weekStart, screen.thisWeek.weekStart)
        assertEquals(home.thisWeek.trainingMinutes, screen.thisWeek.trainingMinutes)

        // Invariant 4: heatmap covers 3 months ending in the current one.
        assertEquals(3, home.heatmap.months.size)
        assertEquals(9, home.heatmap.months.last().month)
        assertEquals(23, home.heatmap.months.last().todayDay)
    }
}
