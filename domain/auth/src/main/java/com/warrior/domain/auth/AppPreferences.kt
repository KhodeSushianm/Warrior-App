package com.warrior.domain.auth

import kotlinx.coroutines.flow.Flow

/**
 * Device-level display preferences (Season 2 / Phase 13): language and
 * display calendar. Stored in DataStore (Architecture §14.2 — never
 * SharedPreferences); storage/dates remain UTC millis regardless of these.
 *
 * Values: language ∈ {"system", "en", "fa"}; calendar ∈ {"gregorian", "jalali"}.
 */
interface AppPreferences {

    val language: Flow<String>

    val calendar: Flow<String>

    suspend fun setLanguage(tag: String)

    suspend fun setCalendar(id: String)

    companion object {
        const val LANG_SYSTEM = "system"
        const val LANG_EN = "en"
        const val LANG_FA = "fa"
        const val CALENDAR_GREGORIAN = "gregorian"
        const val CALENDAR_JALALI = "jalali"
    }
}
