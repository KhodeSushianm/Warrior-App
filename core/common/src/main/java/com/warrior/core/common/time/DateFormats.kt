package com.warrior.core.common.time

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Display calendar choice (Season 2 / Phase 13); storage is always UTC millis. */
enum class DisplayCalendar { GREGORIAN, JALALI }

/**
 * Display formatting. v1 shipped English+Gregorian (owner decision, Phase 6);
 * Phase 13 adds fa + Jalali presentation on top of the SAME storage
 * (UTC epoch millis of local-day midnight, DB v4 §9). English/Gregorian
 * behavior is untouched; Jalali labels follow the jalaali reference calendar.
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
        calendar: DisplayCalendar = DisplayCalendar.GREGORIAN,
        locale: Locale = Locale.ENGLISH,
    ): String {
        val today = TimeUtils.localDayMidnightUtcMillis(now, zone)
        val day = TimeUtils.localDayMidnightUtcMillis(dateUtcMillis, zone)
        return when (day) {
            today -> todayLabel
            today - DAY_MS -> yesterdayLabel
            else -> when (calendar) {
                DisplayCalendar.GREGORIAN -> {
                    val formatter = SimpleDateFormat("EEE, MMM d, yyyy", locale)
                    formatter.timeZone = zone
                    formatter.format(Date(day))
                }
                DisplayCalendar.JALALI -> fullJalali(day, zone, locale)
            }
        }
    }

    private fun fullJalali(dayMillis: Long, zone: TimeZone, locale: Locale): String {
        val gregorian = Calendar.getInstance(zone).apply { timeInMillis = dayMillis }
        val jalali = JalaliCalendar.gregorianToJalali(
            gregorian.get(Calendar.YEAR),
            gregorian.get(Calendar.MONTH) + 1,
            gregorian.get(Calendar.DAY_OF_MONTH),
        )
        val weekday = JalaliCalendar.weekDayNameFa(gregorian.get(Calendar.DAY_OF_WEEK))
        val text = "$weekday ${jalali.day} ${JalaliCalendar.monthNameFa(jalali.month)} ${jalali.year}"
        return if (locale.language == "fa") JalaliCalendar.toPersianDigits(text) else text
    }

    /** "Fri, Sep 25" — compact row labels. */
    fun short(
        dateUtcMillis: Long,
        zone: TimeZone = TimeZone.getDefault(),
        calendar: DisplayCalendar = DisplayCalendar.GREGORIAN,
        locale: Locale = Locale.ENGLISH,
    ): String {
        if (calendar == DisplayCalendar.JALALI) {
            val gregorian = Calendar.getInstance(zone).apply { timeInMillis = dateUtcMillis }
            val jalali = JalaliCalendar.gregorianToJalali(
                gregorian.get(Calendar.YEAR),
                gregorian.get(Calendar.MONTH) + 1,
                gregorian.get(Calendar.DAY_OF_MONTH),
            )
            val weekday = JalaliCalendar.WEEK_DAY_NAMES_SHORT_FA[gregorian.get(Calendar.DAY_OF_WEEK) - 1]
            val text = "$weekday ${jalali.day} ${JalaliCalendar.monthNameFa(jalali.month)}"
            return if (locale.language == "fa") JalaliCalendar.toPersianDigits(text) else text
        }
        val formatter = SimpleDateFormat("EEE, MMM d", locale)
        formatter.timeZone = zone
        return formatter.format(Date(dateUtcMillis))
    }

    /** 165 -> "2h 45m"; 45 -> "45m". */
    fun durationLabel(totalMinutes: Long): String =
        if (totalMinutes >= 60) "${totalMinutes / 60}h ${totalMinutes % 60}m" else "${totalMinutes}m"

    /**
     * "Sep 19 – Sep 25" for week headers (THIS WEEK card). [endExclusiveMillis]
     * is the next week's start; formatting the instant *before* it yields the
     * week's last local day even when DST shifted the boundary (Phase 7).
     */
    fun weekRange(
        startMillis: Long,
        endExclusiveMillis: Long,
        zone: TimeZone = TimeZone.getDefault(),
        calendar: DisplayCalendar = DisplayCalendar.GREGORIAN,
        locale: Locale = Locale.ENGLISH,
    ): String {
        val last = endExclusiveMillis - 1
        if (calendar == DisplayCalendar.JALALI) {
            val s = jalaliParts(startMillis, zone)
            val e = jalaliParts(last, zone)
            val text = if (s.year == e.year) {
                "${s.day} ${JalaliCalendar.monthNameFa(s.month)} – ${e.day} ${JalaliCalendar.monthNameFa(e.month)}"
            } else {
                "${s.day} ${JalaliCalendar.monthNameFa(s.month)} ${s.year} – ${e.day} ${JalaliCalendar.monthNameFa(e.month)} ${e.year}"
            }
            return if (locale.language == "fa") JalaliCalendar.toPersianDigits(text) else text
        }
        val formatter = SimpleDateFormat("MMM d", locale)
        formatter.timeZone = zone
        return "${formatter.format(Date(startMillis))} – ${formatter.format(Date(last))}"
    }

    /** "Sep" — heatmap month labels. [month] is 1-based, timezone-independent. */
    fun monthLabel(year: Int, month: Int, calendar: DisplayCalendar = DisplayCalendar.GREGORIAN, locale: Locale = Locale.ENGLISH): String {
        if (calendar == DisplayCalendar.JALALI) {
            return JalaliCalendar.monthNameFa(month)
        }
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, 1)
        }
        val formatter = SimpleDateFormat("MMM", locale)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(cal.timeInMillis))
    }

    private fun jalaliParts(millis: Long, zone: TimeZone): JalaliCalendar.JalaliDate {
        val gregorian = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return JalaliCalendar.gregorianToJalali(
            gregorian.get(Calendar.YEAR),
            gregorian.get(Calendar.MONTH) + 1,
            gregorian.get(Calendar.DAY_OF_MONTH),
        )
    }
}
