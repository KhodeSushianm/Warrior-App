package com.warrior.core.common.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** JalaliCalendar (Season 2 / Phase 13) — hand-verified anchors + roundtrip. */
class JalaliCalendarTest {

    @Test
    fun knownAnchors_gregorianToJalali() {
        // Today in the sandbox era: Sun 2026-09-27 = 1405/07/05.
        assertEquals(JalaliCalendar.JalaliDate(1405, 7, 5), JalaliCalendar.gregorianToJalali(2026, 9, 27))
        // Nowruz anchors.
        assertEquals(JalaliCalendar.JalaliDate(1405, 1, 1), JalaliCalendar.gregorianToJalali(2026, 3, 21))
        assertEquals(JalaliCalendar.JalaliDate(1404, 1, 1), JalaliCalendar.gregorianToJalali(2025, 3, 21))
        // Leap years per reference impl: 1403 leap (2025-03-20 = 1403-12-30), 1404 not.
        assertEquals(JalaliCalendar.JalaliDate(1403, 12, 30), JalaliCalendar.gregorianToJalali(2025, 3, 20))
        assertEquals(JalaliCalendar.JalaliDate(1404, 12, 29), JalaliCalendar.gregorianToJalali(2026, 3, 20))
        // Astronomical Nowruz anchors (equinox before/after Tehran noon):
        // 1403 started on 2024-03-20; 1399 was leap (Esfand 30 = 2021-03-20).
        assertEquals(JalaliCalendar.JalaliDate(1403, 1, 1), JalaliCalendar.gregorianToJalali(2024, 3, 20))
        assertEquals(JalaliCalendar.JalaliDate(1403, 12, 30), JalaliCalendar.gregorianToJalali(2025, 3, 20))
        assertEquals(JalaliCalendar.JalaliDate(1399, 12, 30), JalaliCalendar.gregorianToJalali(2021, 3, 20))
    }

    @Test
    fun knownAnchors_jalaliToGregorian() {
        assertEquals(Triple(2026, 9, 27), JalaliCalendar.jalaliToGregorian(1405, 7, 5))
        assertEquals(Triple(2026, 3, 21), JalaliCalendar.jalaliToGregorian(1405, 1, 1))
        assertEquals(Triple(2021, 3, 20), JalaliCalendar.jalaliToGregorian(1399, 12, 30))
    }

    @Test
    fun leapYears_matchAstronomicalCalendar() {
        assertTrue(JalaliCalendar.isJalaliLeap(1399)) // Esfand 30 = 2021-03-20
        assertTrue(JalaliCalendar.isJalaliLeap(1403))
        assertTrue(!JalaliCalendar.isJalaliLeap(1404))
        assertTrue(JalaliCalendar.isJalaliLeap(1408))
        assertEquals(30, JalaliCalendar.jalaliMonthLength(1403, 12))
        assertEquals(29, JalaliCalendar.jalaliMonthLength(1404, 12))
        assertEquals(31, JalaliCalendar.jalaliMonthLength(1404, 1))
        assertEquals(30, JalaliCalendar.jalaliMonthLength(1404, 7))
    }

    @Test
    fun roundtrip_tenYearsOfDays() {
        // 1400-01-01 .. 1410-12-29: every day survives gregorian->jalali->gregorian.
        var jy = 1400
        var jm = 1
        var jd = 1
        var count = 0
        while (jy < 1410) {
            val (gy, gm, gd) = JalaliCalendar.jalaliToGregorian(jy, jm, jd)
            val back = JalaliCalendar.gregorianToJalali(gy, gm, gd)
            assertEquals(JalaliCalendar.JalaliDate(jy, jm, jd), back)
            count++
            // advance one day
            jd++
            if (jd > JalaliCalendar.jalaliMonthLength(jy, jm)) {
                jd = 1
                jm++
                if (jm > 12) {
                    jm = 1
                    jy++
                }
            }
        }
        assertTrue(count > 3600)
    }

    @Test
    fun persianDigits_andMonthNames() {
        assertEquals("۱۴۰۵/۰۷/۰۵", JalaliCalendar.toPersianDigits("1405/07/05"))
        assertEquals("مهر", JalaliCalendar.monthNameFa(7))
        assertEquals("فروردین", JalaliCalendar.monthNameFa(1))
        assertEquals("اسفند", JalaliCalendar.monthNameFa(12))
    }
}
