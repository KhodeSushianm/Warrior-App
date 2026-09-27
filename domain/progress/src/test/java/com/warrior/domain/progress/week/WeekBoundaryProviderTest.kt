package com.warrior.domain.progress.week

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Week-boundary acceptance tests (EXECUTION-PLAN Phase 7): fixed instants,
 * Friday/Saturday edges, non-UTC zones and DST transitions. All expected
 * values are hand-computed wall-clock instants.
 */
class WeekBoundaryProviderTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val utcWeeks = WeekBoundaryProvider(utc)

    private fun instant(
        zone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        millis: Int = 0,
    ): Long = Calendar.getInstance(zone)
        .apply {
            clear()
            set(year, month - 1, day, hour, minute, second)
            set(Calendar.MILLISECOND, millis)
        }.timeInMillis

    @Test
    fun weekStartDayIsSaturday_singleCentralRule() {
        // Architecture v2.1 §13.2: the only place the rule exists.
        assertEquals(Calendar.SATURDAY, WeekBoundaryProvider.WEEK_START_DAY)
    }

    @Test
    fun midweekNow_mapsBackToSaturdayMidnight() {
        // Wed 2026-09-23 12:00 UTC -> Sat 2026-09-19 00:00 UTC
        assertEquals(
            instant(utc, 2026, 9, 19),
            utcWeeks.startOfWeek(instant(utc, 2026, 9, 23, hour = 12)),
        )
    }

    @Test
    fun sundayBelongsToTheWeekThatStartedYesterday() {
        // Sun 2026-09-20 09:00 UTC -> Sat 2026-09-19
        assertEquals(
            instant(utc, 2026, 9, 19),
            utcWeeks.startOfWeek(instant(utc, 2026, 9, 20, hour = 9)),
        )
    }

    @Test
    fun fridayLastMillisecond_staysInCurrentWeek() {
        // Fri 2026-09-25 23:59:59.999 UTC -> Sat 2026-09-19
        assertEquals(
            instant(utc, 2026, 9, 19),
            utcWeeks.startOfWeek(instant(utc, 2026, 9, 25, hour = 23, minute = 59, second = 59, millis = 999)),
        )
    }

    @Test
    fun saturdayMidnightSharp_startsTheNewWeek() {
        // Sat 2026-09-26 00:00:00.000 and +1 ms -> itself
        assertEquals(
            instant(utc, 2026, 9, 26),
            utcWeeks.startOfWeek(instant(utc, 2026, 9, 26)),
        )
        assertEquals(
            instant(utc, 2026, 9, 26),
            utcWeeks.startOfWeek(instant(utc, 2026, 9, 26, millis = 1)),
        )
    }

    @Test
    fun weekNeighbors_endExclusiveAndPreviousWeekStart() {
        val now = instant(utc, 2026, 9, 23, hour = 12)
        val start = utcWeeks.startOfWeek(now)
        assertEquals(instant(utc, 2026, 9, 19), start)
        assertEquals(instant(utc, 2026, 9, 26), utcWeeks.endOfWeekExclusive(now))
        assertEquals(instant(utc, 2026, 9, 26), utcWeeks.startOfNextWeek(start))
        assertEquals(instant(utc, 2026, 9, 12), utcWeeks.startOfPreviousWeek(start))
    }

    @Test
    fun tehranZone_usesLocalSaturdayNotTheUtcDay() {
        // Sat 2026-09-26 00:30 Asia/Tehran (+3:30, no DST) = Fri 2026-09-25 21:00 UTC.
        val tehran = TimeZone.getTimeZone("Asia/Tehran")
        val tehranWeeks = WeekBoundaryProvider(tehran)
        val now = instant(tehran, 2026, 9, 26, minute = 30)

        assertEquals(instant(utc, 2026, 9, 25, hour = 21), now) // sanity: offset is +3:30
        // Tehran already lives in the new week:
        assertEquals(instant(tehran, 2026, 9, 26), tehranWeeks.startOfWeek(now))
        // The very same instant is still Friday in UTC:
        assertEquals(instant(utc, 2026, 9, 19), utcWeeks.startOfWeek(now))
    }

    @Test
    fun dstSpringForward_weekIsSevenLocalDaysNot168Hours() {
        // Europe/Berlin: DST starts Sun 2026-03-29, 02:00 CET -> 03:00 CEST.
        val berlin = TimeZone.getTimeZone("Europe/Berlin")
        val weeks = WeekBoundaryProvider(berlin)

        val start = weeks.startOfWeek(instant(berlin, 2026, 3, 29, hour = 12))
        assertEquals(instant(berlin, 2026, 3, 28), start) // Sat 00:00 CET
        assertEquals(instant(utc, 2026, 3, 27, hour = 23), start)

        val next = weeks.startOfNextWeek(start)
        assertEquals(instant(berlin, 2026, 4, 4), next) // Sat 00:00 CEST
        assertEquals(instant(utc, 2026, 4, 3, hour = 22), next)

        // Elapsed epoch time is 6d 23h — still exactly 7 local (wall-clock) days.
        assertEquals(6 * 86_400_000L + 23 * 3_600_000L, next - start)
    }

    @Test
    fun dstFallBack_weekIsSevenLocalDaysNot168Hours() {
        // Europe/Berlin: DST ends Sun 2026-10-25, 03:00 CEST -> 02:00 CET.
        val berlin = TimeZone.getTimeZone("Europe/Berlin")
        val weeks = WeekBoundaryProvider(berlin)

        val start = weeks.startOfWeek(instant(berlin, 2026, 10, 25, hour = 12))
        assertEquals(instant(utc, 2026, 10, 23, hour = 22), start) // Sat 00:00 CEST

        val next = weeks.startOfNextWeek(start)
        assertEquals(instant(utc, 2026, 10, 30, hour = 23), next) // Sat 00:00 CET
        assertEquals(7 * 86_400_000L + 3_600_000L, next - start)
    }
}
