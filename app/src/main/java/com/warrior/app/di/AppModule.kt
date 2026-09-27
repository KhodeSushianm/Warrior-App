package com.warrior.app.di

import com.warrior.domain.progress.time.TimeProvider
import com.warrior.domain.progress.week.WeekBoundaryProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.TimeZone
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Single time source for the app. Replaces the Phase 4 `java.time.Clock`
     * placeholder: java.time needs API 26 and the app ships minSdk 24 without
     * desugaring (Phase 7 deviation note). Injecting this keeps week-boundary
     * logic testable with fixed clocks.
     */
    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = TimeProvider { System.currentTimeMillis() }

    /**
     * The one central week rule (Architecture v2.1 §13.2, Sat→Fri). Device
     * timezone is read once at injection; boundaries stay wall-clock correct
     * across DST because WeekBoundaryProvider is Calendar-based.
     */
    @Provides
    @Singleton
    fun provideWeekBoundaryProvider(): WeekBoundaryProvider =
        WeekBoundaryProvider(TimeZone.getDefault())
}
