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
        assertEquals("Today", DateFormats.dayHeader(today, now, utc, todayLabel = "Today", yesterdayLabel = "Yesterday"))
        assertEquals(
            "Yesterday",
            DateFormats.dayHeader(today - dayMs, now, utc, todayLabel = "Today", yesterdayLabel = "Yesterday"),
        )
        // older day renders a full Gregorian label with weekday
        val label = DateFormats.dayHeader(today - 5 * dayMs, now, utc, todayLabel = "Today", yesterdayLabel = "Yesterday")
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

    @Test
    fun jalaliDayHeader_weekRangeAndShort_faLocale() {
        val fa = java.util.Locale("fa")
        val now = localMillis(utc, 2026, 9, 27) + 12 * 3_600_000L // Sun 2026-09-27 12:00 UTC = 1405/7/5

        // A previous day: 2026-09-20 (Sunday) = 1405/6/29 -> full Jalali label with Persian digits.
        val header = DateFormats.dayHeader(
            localMillis(utc, 2026, 9, 20),
            now,
            utc,
            todayLabel = "امروز",
            yesterdayLabel = "دیروز",
            calendar = DisplayCalendar.JALALI,
            locale = fa,
        )
        assertEquals("یکشنبه ۲۹ شهریور ۱۴۰۵", header)

        // Today/yesterday still resolve through the passed labels.
        assertEquals(
            "امروز",
            DateFormats.dayHeader(
                localMillis(utc, 2026, 9, 27),
                now,
                utc,
                todayLabel = "امروز",
                yesterdayLabel = "دیروز",
                calendar = DisplayCalendar.JALALI,
                locale = fa,
            ),
        )

        // Jalali week range: Sat 2026-09-26 (1405/7/4) .. Fri 2026-10-02 (1405/7/10).
        assertEquals(
            "۴ مهر – ۱۰ مهر",
            DateFormats.weekRange(
                localMillis(utc, 2026, 9, 26),
                localMillis(utc, 2026, 10, 3),
                utc,
                calendar = DisplayCalendar.JALALI,
                locale = fa,
            ),
        )

        // Compact Jalali day: Thu 2026-09-24 = 1405/7/2 -> "پ ۲ مهر".
        assertEquals(
            "پ ۲ مهر",
            DateFormats.short(
                localMillis(utc, 2026, 9, 24),
                utc,
                calendar = DisplayCalendar.JALALI,
                locale = fa,
            ),
        )

        // Jalali month label for the heatmap.
        assertEquals("مهر", DateFormats.monthLabel(1405, 7, calendar = DisplayCalendar.JALALI))

        // Gregorian path stays byte-identical to v1 behavior.
        assertEquals(
            "Sep 19 – Sep 25",
            DateFormats.weekRange(localMillis(utc, 2026, 9, 19), localMillis(utc, 2026, 9, 26), utc),
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
