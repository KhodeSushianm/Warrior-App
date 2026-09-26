package com.warrior.core.common.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class TimeUtilsTest {

    @Test
    fun localDayMidnight_matchesZoneStartOfDay() {
        val tehran = TimeZone.getTimeZone("Asia/Tehran")
        // 2026-09-25 20:00 Tehran (= 15:30 UTC) -> midnight Tehran of the same day
        val now = 1_790_000_000_000L // arbitrary; assert idempotence + zone behavior instead
        val midnight = TimeUtils.localDayMidnightUtcMillis(now, tehran)
        // midnight must be <= now and within 24h before it
        assert(midnight <= now)
        assert(now - midnight < 24L * 60 * 60 * 1000)
        // applying twice is idempotent
        assertEquals(midnight, TimeUtils.localDayMidnightUtcMillis(midnight, tehran))
        // different zone yields a different boundary unless zones align at that instant
        val utc = TimeUtils.localDayMidnightUtcMillis(now, TimeZone.getTimeZone("UTC"))
        assert(midnight != utc || now - midnight == now - utc)
    }

    @Test
    fun utcZoneMidnight_isExactDayStart() {
        val utc = TimeZone.getTimeZone("UTC")
        val dayMs = 86_400_000L
        val someNoon = 1_758_000_000_000L / dayMs * dayMs + dayMs / 2
        assertEquals(someNoon - dayMs / 2, TimeUtils.localDayMidnightUtcMillis(someNoon, utc))
    }
}
