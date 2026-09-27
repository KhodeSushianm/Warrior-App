package com.warrior.domain.progress.week

import java.util.Calendar
import java.util.TimeZone

/**
 * The single central week rule of the whole app (Architecture v2.1 §13.2):
 * weeks run **Saturday → Friday**, following the Iranian calendar convention
 * of the MVP audience. No feature may compute its own boundary — everything
 * goes through this class. For an international release, switching
 * [WEEK_START_DAY] to [Calendar.MONDAY] (ISO 8601) is a one-line change and
 * nothing else is touched.
 *
 * Implementation is [Calendar]-based (no desugaring, minSdk 24): all
 * arithmetic preserves *local wall-clock midnight*, so a week always spans
 * exactly 7 local days even across DST transitions — its epoch-millis length
 * may then be 167h or 169h, which is the correct behavior.
 *
 * Edge case: in zones where midnight itself does not exist on the target day
 * (e.g. DST springs forward at 00:00), [Calendar] resolves to the first
 * existing instant of that local day. Accepted for MVP.
 */
class WeekBoundaryProvider(
    private val zone: TimeZone = TimeZone.getDefault(),
) {

    /** Local midnight (UTC epoch millis) of the week-start day containing [nowMillis]. */
    fun startOfWeek(nowMillis: Long): Long {
        val calendar = localMidnight(nowMillis)
        val daysSinceWeekStart =
            ((calendar.get(Calendar.DAY_OF_WEEK) - WEEK_START_DAY) + DAYS_PER_WEEK) % DAYS_PER_WEEK
        calendar.add(Calendar.DAY_OF_YEAR, -daysSinceWeekStart)
        return calendar.timeInMillis
    }

    /** Start of the week *after* the one beginning at [weekStartMillis]. */
    fun startOfNextWeek(weekStartMillis: Long): Long = shiftDays(weekStartMillis, DAYS_PER_WEEK)

    /** Start of the week *before* the one beginning at [weekStartMillis]. */
    fun startOfPreviousWeek(weekStartMillis: Long): Long = shiftDays(weekStartMillis, -DAYS_PER_WEEK)

    /** Exclusive end of the week containing [nowMillis] (= start of the next week). */
    fun endOfWeekExclusive(nowMillis: Long): Long = startOfNextWeek(startOfWeek(nowMillis))

    private fun shiftDays(millis: Long, days: Int): Long =
        Calendar.getInstance(zone)
            .apply {
                timeInMillis = millis
                add(Calendar.DAY_OF_YEAR, days)
            }.timeInMillis

    private fun localMidnight(millis: Long): Calendar =
        Calendar.getInstance(zone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    companion object {
        /** Architecture v2.1 §13.2 — the one and only week rule. */
        const val WEEK_START_DAY = Calendar.SATURDAY
        private const val DAYS_PER_WEEK = 7
    }
}
