package com.warrior.core.common.time

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Display formatting for v1 (English, Gregorian — owner decision at Phase 6).
 * Storage stays UTC epoch millis of local-day midnight (DB v4 §9); only
 * presentation uses the device timezone.
 */
object DateFormats {

    private const val DAY_MS = 86_400_000L

    private fun fullFormatter() = SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH)

    private fun shortFormatter() = SimpleDateFormat("EEE, MMM d", Locale.ENGLISH)

    /**
     * "Today" / "Yesterday" / "Fri, Sep 25, 2026" — used for history day headers.
     * Phase 9 (i18n-ready): the two relative labels are passed in from UI string
     * resources instead of being hardcoded here; only the absolute date stays a
     * locale-format concern of this utility.
     */
    fun dayHeader(
        dateUtcMillis: Long,
        now: Long = System.currentTimeMillis(),
        zone: TimeZone = TimeZone.getDefault(),
        todayLabel: String,
        yesterdayLabel: String,
    ): String {
        val today = TimeUtils.localDayMidnightUtcMillis(now, zone)
        val day = TimeUtils.localDayMidnightUtcMillis(dateUtcMillis, zone)
        return when (day) {
            today -> todayLabel
            today - DAY_MS -> yesterdayLabel
            else -> {
                val formatter = fullFormatter()
                formatter.timeZone = zone
                formatter.format(Date(day))
            }
        }
    }

    /** "Fri, Sep 25" — compact row labels. */
    fun short(dateUtcMillis: Long, zone: TimeZone = TimeZone.getDefault()): String {
        val formatter = shortFormatter()
        formatter.timeZone = zone
        return formatter.format(Date(dateUtcMillis))
    }

    /** 165 -> "2h 45m"; 45 -> "45m". */
    fun durationLabel(totalMinutes: Long): String =
        if (totalMinutes >= 60) "${totalMinutes / 60}h ${totalMinutes % 60}m" else "${totalMinutes}m"

    /** "Sep" — heatmap month labels. [month] is 1-based, timezone-independent. */
    fun monthLabel(year: Int, month: Int): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, 1)
        }
        val formatter = SimpleDateFormat("MMM", Locale.ENGLISH)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(calendar.timeInMillis))
    }

    /**
     * "Sep 19 – Sep 25" for week headers (THIS WEEK card). [endExclusiveMillis]
     * is the next week's start; formatting the instant *before* it yields the
     * week's last local day even when DST shifted the boundary (Phase 7).
     */
    fun weekRange(
        startMillis: Long,
        endExclusiveMillis: Long,
        zone: TimeZone = TimeZone.getDefault(),
    ): String {
        val formatter = SimpleDateFormat("MMM d", Locale.ENGLISH)
        formatter.timeZone = zone
        return "${formatter.format(Date(startMillis))} – ${formatter.format(Date(endExclusiveMillis - 1))}"
    }
}
