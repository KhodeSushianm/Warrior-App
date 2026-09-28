package com.warrior.domain.progress

import com.warrior.domain.progress.week.WeekBoundaryProvider
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Jalali heatmap (Season 2 / Phase 13) — hand-computed against the reference
 * jalaali calendar: "now" = Sun 2026-09-27 12:00 UTC = 1405/07/05.
 * Window: Mordad 1405, Shahrivar 1405, Mehr 1405.
 * Anchor days: Mehr 1 = 2026-09-23 (Wed -> offset 4), Shahrivar 1 = 2026-08-23
 * (Sun -> offset 1), Mordad 1 = 2026-07-23 (Thu -> offset 5).
 */
class JalaliHeatmapTest {

    private val weeks = WeekBoundaryProvider(UTC)
    private val now = instant(UTC, 2026, 9, 27, hour = 12)

    private fun cardio(date: Long) = session(
        id = date,
        date = date,
        overallIntensity = 7,
        activities = listOf(activity(WorkoutType.CARDIO, 30, FocusArea.CONDITIONING)),
    )

    @Test
    fun jalaliHeatmap_threePersianMonths_withOffsetsAndTrainedDays() {
        val sessions = listOf(
            // Mordad 19
            cardio(instant(UTC, 2026, 8, 10)),
            // Shahrivar 10
            cardio(instant(UTC, 2026, 9, 1)),
            // Mehr 5 (today)
            cardio(instant(UTC, 2026, 9, 27)),
            // Tir -> outside the 3-month window
            cardio(instant(UTC, 2026, 7, 20)),
        )

        val heatmap = ProgressEngine.monthsHeatmap(sessions, now, UTC, monthCount = 3, jalali = true)

        assertEquals(3, heatmap.months.size)
        val (mordad, shahrivar, mehr) = heatmap.months

        assertEquals(1405 to 5, mordad.year to mordad.month)
        assertEquals(31, mordad.daysInMonth)
        assertEquals(5, mordad.firstDayColumnOffset) // Mordad 1 = Thursday
        assertEquals(setOf(19), mordad.trainedDays)
        assertNull(mordad.todayDay)

        assertEquals(1405 to 6, shahrivar.year to shahrivar.month)
        assertEquals(31, shahrivar.daysInMonth)
        assertEquals(1, shahrivar.firstDayColumnOffset) // Shahrivar 1 = Sunday
        assertEquals(setOf(10), shahrivar.trainedDays)
        assertNull(shahrivar.todayDay)

        assertEquals(1405 to 7, mehr.year to mehr.month)
        assertEquals(30, mehr.daysInMonth)
        assertEquals(4, mehr.firstDayColumnOffset) // Mehr 1 = Wednesday
        assertEquals(setOf(5), mehr.trainedDays)
        assertEquals(5, mehr.todayDay)
    }

    @Test
    fun jalaliHeatmap_esfandLeapAndYearRollover() {
        // 2026-03-10 = 1404-12-19 (reference-verified): Dey, Bahman, Esfand 1404 (29d, non-leap).
        val springNow = instant(UTC, 2026, 3, 10, hour = 12)
        val heatmap = ProgressEngine.monthsHeatmap(emptyList(), springNow, UTC, monthCount = 3, jalali = true)
        assertEquals(
            listOf(1404 to 10, 1404 to 11, 1404 to 12),
            heatmap.months.map { it.year to it.month },
        )
        assertEquals(29, heatmap.months[2].daysInMonth) // 1404 is NOT leap
        assertEquals(19, heatmap.months[2].todayDay)

        // Leap year rollover: 2025-03-15 = 1403-12-25 (1403 IS leap -> Esfand 30).
        val leapNow = instant(UTC, 2025, 3, 15, hour = 12)
        val leapMap = ProgressEngine.monthsHeatmap(emptyList(), leapNow, UTC, monthCount = 3, jalali = true)
        assertEquals(
            listOf(1403 to 10, 1403 to 11, 1403 to 12),
            leapMap.months.map { it.year to it.month },
        )
        assertEquals(30, leapMap.months[2].daysInMonth)

        // Year rollover: 2026-04-05 = 1405-01-16 -> Farvardin, Esfand 1404, Bahman 1404.
        val rollover = instant(UTC, 2026, 4, 5, hour = 12)
        val rolled = ProgressEngine.monthsHeatmap(emptyList(), rollover, UTC, monthCount = 3, jalali = true)
        assertEquals(
            listOf(1404 to 11, 1404 to 12, 1405 to 1),
            rolled.months.map { it.year to it.month },
        )
        assertEquals(31, rolled.months[2].daysInMonth) // Farvardin
    }
}
