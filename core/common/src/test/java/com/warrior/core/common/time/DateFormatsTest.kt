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
}
