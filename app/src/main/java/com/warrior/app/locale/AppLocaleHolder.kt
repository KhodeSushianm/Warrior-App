package com.warrior.app.locale

import android.content.Context
import android.content.res.Configuration
import com.warrior.domain.auth.AppPreferences
import java.util.Locale

/**
 * In-memory display-language cache (Season 2 / Phase 13). DataStore is async,
 * but `attachBaseContext` must be synchronous — so the app process keeps the
 * current choice here: loaded once at Application start, updated instantly on
 * every change, and applied by wrapping the base Context (no appcompat /
 * per-app-locales API needed; works on minSdk 24).
 *
 * RTL: `Configuration.setLayoutDirection(locale)` makes Compose mirror the
 * whole tree automatically for fa.
 */
object AppLocaleHolder {

    @Volatile
    var languageTag: String = AppPreferences.LANG_SYSTEM

    fun wrap(base: Context): Context {
        if (languageTag == AppPreferences.LANG_SYSTEM) return base
        val locale = Locale.forLanguageTag(languageTag)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
