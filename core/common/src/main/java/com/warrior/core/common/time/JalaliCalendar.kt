package com.warrior.core.common.time

/**
 * Jalali (Shamsi/Persian) calendar conversions — pure Kotlin, no external
 * library (Season 2 / Phase 13). Display-only: storage stays UTC epoch millis
 * of local midnight (DB v4 §9) and the week rule stays Saturday→Friday
 * (which already matches the Iranian week).
 *
 * Algorithm: the jalaali-js "breaks" method (v2 conversion) — matches the
 * astronomical vernal-equinox rule for Nowruz across the whole modern range
 * (validated in tests against equinox-derived anchors, incl. leap 1399/1403).
 */
object JalaliCalendar {

    /** Jalali date; [month] is 1..12 (Farvardin..Esfand). */
    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    val MONTH_NAMES_FA = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    /** Index = java.util.Calendar.SUNDAY(1)..SATURDAY(7) - 1. */
    val WEEK_DAY_NAMES_FA = listOf(
        "یکشنبه",
        "دوشنبه",
        "سه‌شنبه",
        "چهارشنبه",
        "پنجشنبه",
        "جمعه",
        "شنبه",
    )

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
        1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate = julianDayToJalali(gregorianToJulianDay(gy, gm, gd))

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jdn = jalaliToJulianDay(jy, jm, jd)
        return julianDayToGregorian(jdn)
    }

    fun isJalaliLeap(jy: Int): Boolean = jalCal(jy).leap == 0

    fun jalaliMonthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isJalaliLeap(jy)) 30 else 29
    }

    fun monthNameFa(jm: Int): String = MONTH_NAMES_FA[jm - 1]

    /** Common one-letter Persian weekday abbreviations (index = Calendar.SUNDAY-1 .. SATURDAY-1). */
    val WEEK_DAY_NAMES_SHORT_FA = listOf("ی", "د", "س", "چ", "پ", "ج", "ش")

    fun weekDayNameFa(javaDayOfWeek: Int): String = WEEK_DAY_NAMES_FA[javaDayOfWeek - 1]

    /** Converts ASCII digits to Persian digits (used for fa/Jalali labels). */
    fun toPersianDigits(input: String): String = buildString(input.length) {
        input.forEach { c ->
            append(if (c in '0'..'9') ('۰' + (c - '0')) else c)
        }
    }

    // ---------- jalaali-js core ----------

    private data class JalCal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): JalCal {
        val bl = BREAKS.size
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        var jump = 0
        for (i in 1 until bl) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ = leapJ + floorDiv(jump, 33) * 8 + floorDiv(mod(jump, 33), 4)
            jp = jm
        }
        var n = jy - jp
        leapJ = leapJ + floorDiv(n, 33) * 8 + floorDiv(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ++
        val leapG = floorDiv(gy, 4) - floorDiv((floorDiv(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + floorDiv(jump + 4, 33) * 33
        var leap = mod(mod(n + 1, 33) - 1, 4)
        if (leap == -1) leap = 4
        return JalCal(leap, gy, march)
    }

    private fun jalaliToJulianDay(jy: Int, jm: Int, jd: Int): Long {
        val r = jalCal(jy)
        return gregorianToJulianDay(r.gy, 3, r.march) + (jm - 1) * 31 - floorDiv(jm, 7) * (jm - 7) + jd - 1
    }

    private fun julianDayToJalali(jdn: Long): JalaliDate {
        val gy = julianDayToGregorian(jdn).first
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = gregorianToJulianDay(gy, 3, r.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                return JalaliDate(jy, 1 + floorDiv(k.toInt(), 31), mod(k.toInt(), 31) + 1)
            }
            k -= 186
        } else {
            jy--
            k += 179
            if (r.leap == 1) k++
        }
        return JalaliDate(jy, 7 + floorDiv(k.toInt(), 30), mod(k.toInt(), 30) + 1)
    }

    // ---------- Julian day number helpers ----------

    private fun gregorianToJulianDay(gy: Int, gm: Int, gd: Int): Long {
        // Standard proleptic-Gregorian JDN; JDN(0001-01-01) = 1721426.
        return 1_721_426L + (gy - 1) * 365L + floorDiv(gy - 1, 4) - floorDiv(gy - 1, 100) +
            floorDiv(gy - 1, 400) + floorDiv(367 * gm - 362, 12) +
            (if (gm > 2) (if (isGregorianLeap(gy)) -1 else -2) else 0) + (gd - 1)
    }

    private fun julianDayToGregorian(jdn: Long): Triple<Int, Int, Int> {
        // Fliegel–Van Flandern (exact transcription).
        var l = jdn + 68569
        val n = Math.floorDiv(4 * l, 146097)
        l = l - Math.floorDiv(146097 * n + 3, 4)
        val i = Math.floorDiv(4000 * (l + 1), 1461001)
        l = l - Math.floorDiv(1461 * i, 4) + 31
        val j = Math.floorDiv(80 * l, 2447)
        val k = l - Math.floorDiv(2447 * j, 80)
        l = Math.floorDiv(j, 11)
        val month = j + 2 - 12 * l
        val year = 100 * (n - 49) + i + l
        return Triple(year.toInt(), month.toInt(), k.toInt())
    }

    private fun isGregorianLeap(gy: Int): Boolean =
        (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0

    private fun mod(a: Int, b: Int): Int = ((a % b) + b) % b

    private fun mod(a: Long, b: Int): Int = (((a % b) + b) % b).toInt()

    private fun floorDiv(a: Int, b: Int): Int = Math.floorDiv(a, b)

    private fun floorDiv(a: Long, b: Int): Long = Math.floorDiv(a, b.toLong())

    private fun floorDiv(a: Long, b: Long): Long = Math.floorDiv(a, b)
}
