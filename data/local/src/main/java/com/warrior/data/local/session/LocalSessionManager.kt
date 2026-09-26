package com.warrior.data.local.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.warrior.domain.auth.LocalSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.warriorSessionStore by preferencesDataStore(name = "warrior_session")

/**
 * DataStore-backed local session (Architecture v2.1 §14.2 — DataStore, not
 * SharedPreferences). Logout clears these two keys only.
 */
@Singleton
class LocalSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocalSession {

    override val currentUserId: Flow<Long?> =
        context.warriorSessionStore.data.map { prefs -> prefs[KEY_USER_ID] }

    override suspend fun start(userId: Long) {
        context.warriorSessionStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_CREATED_AT] = System.currentTimeMillis()
        }
    }

    override suspend fun clear() {
        context.warriorSessionStore.edit { it.clear() }
    }

    private companion object {
        val KEY_USER_ID = longPreferencesKey("currentUserId")
        val KEY_CREATED_AT = longPreferencesKey("sessionCreatedAt")
    }
}
