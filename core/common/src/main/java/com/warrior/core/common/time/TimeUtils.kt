package com.warrior.core.common.time

import java.util.Calendar
import java.util.TimeZone

/**
 * Time helpers based on java.util.Calendar so minSdk 24 needs no core-library
 * desugaring. All stored values remain UTC epoch millis (DB v4 §9).
 */
object TimeUtils {

    /** Local-day midnight of [now] expressed as UTC epoch millis (DB v4 §9 `date`). */
    fun localDayMidnightUtcMillis(now: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val calendar = Calendar.getInstance(zone)
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun todayLocalMidnightUtcMillis(): Long =
        localDayMidnightUtcMillis(System.currentTimeMillis())
}
