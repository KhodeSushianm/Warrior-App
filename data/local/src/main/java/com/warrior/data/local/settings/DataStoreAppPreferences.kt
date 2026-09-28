package com.warrior.data.local.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.warrior.domain.auth.AppPreferences
import com.warrior.domain.training.TimerDefaults
import com.warrior.domain.training.TimerPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.warriorSettingsStore by preferencesDataStore(name = "warrior_settings")

/** DataStore-backed display preferences (Season 2 / Phase 13). */
@Singleton
class DataStoreAppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppPreferences, TimerPreferences {

    override val language: Flow<String> =
        context.warriorSettingsStore.data.map { it[KEY_LANGUAGE] ?: AppPreferences.LANG_SYSTEM }

    override val calendar: Flow<String> =
        context.warriorSettingsStore.data.map { it[KEY_CALENDAR] ?: AppPreferences.CALENDAR_GREGORIAN }

    override suspend fun setLanguage(tag: String) {
        context.warriorSettingsStore.edit { it[KEY_LANGUAGE] = tag }
    }

    override suspend fun setCalendar(id: String) {
        context.warriorSettingsStore.edit { it[KEY_CALENDAR] = id }
    }

    // ---- round timer defaults (Phase 14) ----

    override val defaults: Flow<TimerDefaults> =
        context.warriorSettingsStore.data.map { prefs ->
            TimerDefaults(
                workSeconds = prefs[KEY_TIMER_WORK] ?: 180,
                restSeconds = prefs[KEY_TIMER_REST] ?: 60,
                rounds = prefs[KEY_TIMER_ROUNDS] ?: 6,
            )
        }

    override suspend fun setDefaults(defaults: TimerDefaults) {
        context.warriorSettingsStore.edit { prefs ->
            prefs[KEY_TIMER_WORK] = defaults.workSeconds
            prefs[KEY_TIMER_REST] = defaults.restSeconds
            prefs[KEY_TIMER_ROUNDS] = defaults.rounds
        }
    }

    private companion object {
        val KEY_LANGUAGE = stringPreferencesKey("displayLanguage")
        val KEY_CALENDAR = stringPreferencesKey("displayCalendar")
        val KEY_TIMER_WORK = intPreferencesKey("timerWorkSeconds")
        val KEY_TIMER_REST = intPreferencesKey("timerRestSeconds")
        val KEY_TIMER_ROUNDS = intPreferencesKey("timerRounds")
    }
}
