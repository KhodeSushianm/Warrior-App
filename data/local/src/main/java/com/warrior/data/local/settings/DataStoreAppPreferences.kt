package com.warrior.data.local.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.warrior.domain.auth.AppPreferences
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
) : AppPreferences {

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

    private companion object {
        val KEY_LANGUAGE = stringPreferencesKey("displayLanguage")
        val KEY_CALENDAR = stringPreferencesKey("displayCalendar")
    }
}
