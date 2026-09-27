package com.warrior.domain.progress

import com.warrior.domain.progress.week.WeekBoundaryProvider
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Phase 8 chart-input tests: every expected value is hand-computed.
 * Reference "now": Wed 2026-09-23 12:00 UTC -> current week Sat Sep 19 .. Fri Sep 25.
 */
class ProgressChartsTest {

    private val weeks = WeekBoundaryProvider(UTC)
    private val now = instant(UTC, 2026, 9, 23, hour = 12)
    private val thisWeekStart = instant(UTC, 2026, 9, 19)

    private fun cardio(date: Long, minutes: Long = 30, focus: FocusArea = FocusArea.CONDITIONING) =
        // id = date is unique enough for fixtures
        session(
            id = date,
            date = date,
            overallIntensity = 7,
            activities = listOf(activity(WorkoutType.CARDIO, minutes, focus)),
        )

    // ---------- volumeSeries ----------

    @Test
    fun volumeSeries_eightWeeksOldestFirst_windowEdgesExact() {
        val sessions = listOf(
            // far past -> outside the 8-week window
            cardio(instant(UTC, 2026, 7, 15), 60),
            // week of Aug 8
            cardio(instant(UTC, 2026, 8, 10), 30),
            // week of Sep 5
            cardio(instant(UTC, 2026, 9, 6), 40),
            // this week (Sat, week start)
            cardio(instant(UTC, 2026, 9, 19), 50),
            // this week (Mon)
            cardio(instant(UTC, 2026, 9, 21), 25),
            // next week -> outside
            cardio(instant(UTC, 2026, 9, 27), 20),
        )

        val series = ProgressEngine.volumeSeries(sessions, thisWeekStart, weekCount = 8, weeks = weeks)

        assertEquals(8, series.size)
        // Oldest first: Aug 1 .. Sep 19 (all Saturdays).
        assertEquals(
            listOf(
                instant(UTC, 2026, 8, 1),
                instant(UTC, 2026, 8, 8),
                instant(UTC, 2026, 8, 15),
                instant(UTC, 2026, 8, 22),
                instant(UTC, 2026, 8, 29),
                instant(UTC, 2026, 9, 5),
                instant(UTC, 2026, 9, 12),
                instant(UTC, 2026, 9, 19),
            ),
            series.map { it.weekStart },
        )
        assertEquals(
            listOf(0L, 30L, 0L, 0L, 0L, 40L, 0L, 75L),
            series.map { it.trainingMinutes },
        )
        assertEquals(listOf(0, 1, 0, 0, 0, 1, 0, 2), series.map { it.sessionCount })
        assertEquals(listOf(0, 1, 0, 0, 0, 1, 0, 2), series.map { it.trainingDays })
    }

    @Test
    fun volumeSeries_twoSessionsSameDay_countAsOneTrainingDay() {
        val sessions = listOf(
            cardio(instant(UTC, 2026, 9, 21), 30),
            cardio(instant(UTC, 2026, 9, 21), 40),
        )
        val series = ProgressEngine.volumeSeries(sessions, thisWeekStart, weekCount = 8, weeks = weeks)
        val current = series.last()
        assertEquals(70L, current.trainingMinutes)
        assertEquals(2, current.sessionCount)
        assertEquals(1, current.trainingDays)
    }

    // ---------- all-time distributions ----------

    @Test
    fun workoutDistribution_allTimeMinutesPerType_enumOrderZerosRemoved() {
        val sessions = listOf(
            session(
                id = 1,
                date = instant(UTC, 2026, 9, 19),
                overallIntensity = 8,
                activities = listOf(
                    activity(WorkoutType.HEAVY_BAG, 50, FocusArea.POWER),
                    activity(WorkoutType.MITT_WORK, 25, FocusArea.TIMING),
                ),
            ),
            session(
                id = 2,
                date = instant(UTC, 2026, 8, 10),
                overallIntensity = 7,
                activities = listOf(
                    activity(WorkoutType.CARDIO, 30, FocusArea.CONDITIONING),
                    activity(WorkoutType.HEAVY_BAG, 60, FocusArea.POWER),
                ),
            ),
        )
        val distribution = ProgressEngine.workoutDistribution(sessions)
        // Enum order (CARDIO first), SPARRING absent (zero minutes).
        assertEquals(
            listOf(WorkoutType.CARDIO, WorkoutType.HEAVY_BAG, WorkoutType.MITT_WORK),
            distribution.keys.toList(),
        )
        assertEquals(listOf(30L, 110L, 25L), distribution.values.toList())
    }

    @Test
    fun topFocusAreas_descByMinutes_tiesByEnumName_cappedAtLimit() {
        val sessions = listOf(
            session(
                id = 1,
                date = instant(UTC, 2026, 9, 19),
                overallIntensity = 8,
                activities = listOf(
                    activity(WorkoutType.HEAVY_BAG, 50, FocusArea.POWER),
                    activity(WorkoutType.MITT_WORK, 45, FocusArea.TIMING),
                    activity(WorkoutType.SPARRING, 45, FocusArea.DEFENSE),
                    activity(WorkoutType.CARDIO, 30, FocusArea.JAB),
                    activity(WorkoutType.CARDIO, 10, FocusArea.FOOTWORK),
                ),
            ),
        )
        val top = ProgressEngine.topFocusAreas(sessions, limit = 4)
        // POWER 50; tie TIMING/DEFENSE at 45 -> DEFENSE first (enum-name tiebreak); JAB 30; FOOTWORK cut.
        assertEquals(
            listOf(
                FocusArea.POWER to 50L,
                FocusArea.DEFENSE to 45L,
                FocusArea.TIMING to 45L,
                FocusArea.JAB to 30L,
            ),
            top.map { it.focus to it.minutes },
        )
    }

    // ---------- monthsHeatmap ----------

    @Test
    fun monthsHeatmap_threeCalendarMonths_saturdayFirstColumns() {
        val sessions = listOf(
            cardio(instant(UTC, 2026, 7, 15)),
            cardio(instant(UTC, 2026, 8, 10)),
            // same day -> still one trained day
            cardio(instant(UTC, 2026, 8, 10)),
            // month-edge day belongs to September
            cardio(instant(UTC, 2026, 9, 1)),
            cardio(instant(UTC, 2026, 9, 19)),
            cardio(instant(UTC, 2026, 9, 21)),
            // previous month -> outside the 3-month window
            cardio(instant(UTC, 2026, 6, 30)),
        )

        val heatmap = ProgressEngine.monthsHeatmap(sessions, now, weeks.zone, monthCount = 3)

        assertEquals(3, heatmap.months.size)
        val (jul, aug, sep) = heatmap.months

        // July 2026: 31 days, Jul 1 is a Wednesday -> 4 leading blanks (Sat=0 .. Wed=4).
        assertEquals(2026 to 7, jul.year to jul.month)
        assertEquals(31, jul.daysInMonth)
        assertEquals(4, jul.firstDayColumnOffset)
        assertEquals(setOf(15), jul.trainedDays)
        assertNull(jul.todayDay)

        // August 2026: Aug 1 is a Saturday -> offset 0.
        assertEquals(2026 to 8, aug.year to aug.month)
        assertEquals(31, aug.daysInMonth)
        assertEquals(0, aug.firstDayColumnOffset)
        assertEquals(setOf(10), aug.trainedDays)
        assertNull(aug.todayDay)

        // September 2026: 30 days, Sep 1 is a Tuesday -> offset 3; today = 23.
        assertEquals(2026 to 9, sep.year to sep.month)
        assertEquals(30, sep.daysInMonth)
        assertEquals(3, sep.firstDayColumnOffset)
        assertEquals(setOf(1, 19, 21), sep.trainedDays)
        assertEquals(23, sep.todayDay)
    }

    @Test
    fun monthsHeatmap_leapFebruary_andYearRollover() {
        // now = Mar 15, 2028 (leap year) -> Jan 2028, Feb 2028 (29 days), Mar 2028.
        val marchNow = instant(UTC, 2028, 3, 15, hour = 12)
        val heatmap = ProgressEngine.monthsHeatmap(emptyList(), marchNow, weeks.zone, monthCount = 3)

        assertEquals(listOf(1, 2, 3), heatmap.months.map { it.month })
        assertEquals(listOf(2028, 2028, 2028), heatmap.months.map { it.year })
        assertEquals(29, heatmap.months[1].daysInMonth)
        assertEquals(15, heatmap.months[2].todayDay)

        // Year rollover: now = Jan 10, 2027 -> Nov 2026, Dec 2026, Jan 2027.
        val januaryNow = instant(UTC, 2027, 1, 10, hour = 12)
        val rolled = ProgressEngine.monthsHeatmap(emptyList(), januaryNow, weeks.zone, monthCount = 3)
        assertEquals(
            listOf(2026 to 11, 2026 to 12, 2027 to 1),
            rolled.months.map { it.year to it.month },
        )
        // Jan 1, 2027 is a Friday -> offset 6.
        assertEquals(6, rolled.months[2].firstDayColumnOffset)
    }

    @Test
    fun monthsHeatmap_respectsDeviceTimezone() {
        // Fri 2026-09-25 22:00 UTC = Sat 2026-09-26 01:30 in Tehran (+3:30).
        // A session dated Tehran-midnight of Sep 26 is UTC Sep 25 20:30.
        val tehran = java.util.TimeZone.getTimeZone("Asia/Tehran")
        val sessionInTehran = cardio(instant(tehran, 2026, 9, 26))
        val nowTehran = instant(tehran, 2026, 9, 26, hour = 12)

        val heatmap = ProgressEngine.monthsHeatmap(listOf(sessionInTehran), nowTehran, tehran, monthCount = 1)
        assertEquals(setOf(26), heatmap.months.single().trainedDays)
        assertEquals(26, heatmap.months.single().todayDay)

        // The same instants read as UTC: the session's local day is Sep 25,
        // while "now" (Sep 26 08:30 UTC) makes today the 26th.
        val utcHeatmap = ProgressEngine.monthsHeatmap(listOf(sessionInTehran), nowTehran, UTC, monthCount = 1)
        assertEquals(setOf(25), utcHeatmap.months.single().trainedDays)
        assertEquals(26, utcHeatmap.months.single().todayDay)
    }

    // ---------- calculator: progressScreen ----------

    @Test
    fun progressScreen_oneConsistentSnapshot() {
        val calculator = ProgressCalculator(weeks)
        val sessions = listOf(
            session(
                id = 1,
                date = instant(UTC, 2026, 9, 19),
                overallIntensity = 9,
                activities = listOf(
                    activity(WorkoutType.HEAVY_BAG, 30, FocusArea.POWER, rounds = 3),
                    activity(WorkoutType.MITT_WORK, 20, FocusArea.TIMING, rounds = 2),
                ),
            ),
            session(
                id = 2,
                date = instant(UTC, 2026, 9, 21),
                overallIntensity = 7,
                activities = listOf(activity(WorkoutType.CARDIO, 40, FocusArea.CONDITIONING)),
            ),
            session(
                id = 3,
                date = instant(UTC, 2026, 9, 12),
                overallIntensity = 8,
                activities = listOf(activity(WorkoutType.HEAVY_BAG, 60, FocusArea.POWER, rounds = 6)),
            ),
        )

        val snapshot = calculator.progressScreen(sessions, now)

        // Frames identical to the Home snapshot (same central rule).
        assertEquals(thisWeekStart, snapshot.thisWeek.weekStart)
        assertEquals(instant(UTC, 2026, 9, 12), snapshot.lastWeek.weekStart)
        assertEquals(2, snapshot.thisWeek.sessionCount)
        assertEquals(90L, snapshot.thisWeek.trainingMinutes) // 50 + 40
        assertEquals(1, snapshot.lastWeek.sessionCount)

        // Volume: 8 weeks, W-1 = 60m, NOW = 110m.
        assertEquals(8, snapshot.volumeSeries.size)
        assertEquals(60L, snapshot.volumeSeries[6].trainingMinutes)
        assertEquals(90L, snapshot.volumeSeries[7].trainingMinutes)

        // All-time distributions (enum order / desc top-4).
        assertEquals(
            listOf(WorkoutType.CARDIO, WorkoutType.HEAVY_BAG, WorkoutType.MITT_WORK),
            snapshot.workoutDistribution.keys.toList(),
        )
        assertEquals(listOf(40L, 90L, 20L), snapshot.workoutDistribution.values.toList())
        assertEquals(
            listOf(FocusArea.POWER to 90L, FocusArea.CONDITIONING to 40L, FocusArea.TIMING to 20L),
            snapshot.topFocusAreas.map { it.focus to it.minutes },
        )
    }
}
