package com.warrior.app

import android.app.Application
import com.warrior.app.locale.AppLocaleHolder
import com.warrior.domain.auth.AppPreferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WarriorApplication : Application() {

    @Inject
    lateinit var appPreferences: AppPreferences

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        // Keep the synchronous locale cache in sync with DataStore (Phase 13).
        applicationScope.launch {
            appPreferences.language.collect { AppLocaleHolder.languageTag = it }
        }
    }
}
