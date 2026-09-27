package com.warrior.core.common.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class DateFormatsTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val dayMs = 86_400_000L

    @Test
    fun dayHeader_handlesTodayYesterdayAndFullDate() {
        val now = 1_758_800_000_000L // fixed "now"
        val today = TimeUtils.localDayMidnightUtcMillis(now, utc)
        assertEquals("Today", DateFormats.dayHeader(today, now, utc))
        assertEquals("Yesterday", DateFormats.dayHeader(today - dayMs, now, utc))
        // older day renders a full Gregorian label with weekday
        val label = DateFormats.dayHeader(today - 5 * dayMs, now, utc)
        assert(label.matches(Regex("[A-Z][a-z]{2}, [A-Z][a-z]{2} \\d{1,2}, \\d{4}"))) { label }
    }

    @Test
    fun durationLabel_formatsHoursAndMinutes() {
        assertEquals("2h 45m", DateFormats.durationLabel(165))
        assertEquals("45m", DateFormats.durationLabel(45))
        assertEquals("0m", DateFormats.durationLabel(0))
    }

    @Test
    fun monthLabel_shortEnglishMonthNames() {
        assertEquals("Jul", DateFormats.monthLabel(2026, 7))
        assertEquals("Sep", DateFormats.monthLabel(2026, 9))
        assertEquals("Dec", DateFormats.monthLabel(2026, 12))
        assertEquals("Feb", DateFormats.monthLabel(2028, 2))
    }

    @Test
    fun weekRange_formatsFirstAndLastLocalDay() {
        // Plain UTC week: Sat Sep 19 .. next Sat Sep 26 (exclusive).
        assertEquals(
            "Sep 19 – Sep 25",
            DateFormats.weekRange(utcMillis(2026, 9, 19), utcMillis(2026, 9, 26), utc),
        )
    }

    @Test
    fun weekRange_nonUtcZoneAndDstWeek() {
        // Asia/Tehran (+3:30): local week Sep 26 .. Oct 3.
        val tehran = TimeZone.getTimeZone("Asia/Tehran")
        assertEquals(
            "Sep 26 – Oct 2",
            DateFormats.weekRange(localMillis(tehran, 2026, 9, 26), localMillis(tehran, 2026, 10, 3), tehran),
        )
        // Europe/Berlin across the spring-forward: the 167h week still ends on Apr 3 local.
        val berlin = TimeZone.getTimeZone("Europe/Berlin")
        assertEquals(
            "Mar 28 – Apr 3",
            DateFormats.weekRange(localMillis(berlin, 2026, 3, 28), localMillis(berlin, 2026, 4, 4), berlin),
        )
    }

    private fun utcMillis(year: Int, month: Int, day: Int): Long = localMillis(utc, year, month, day)

    private fun localMillis(zone: TimeZone, year: Int, month: Int, day: Int): Long =
        java.util.Calendar.getInstance(zone)
            .apply {
                clear()
                set(year, month - 1, day, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
}
