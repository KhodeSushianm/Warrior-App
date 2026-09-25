package com.warrior.data.local.di

import android.content.Context
import androidx.room.Room
import com.warrior.data.local.dao.RoundDao
import com.warrior.data.local.dao.TrainingSessionDao
import com.warrior.data.local.dao.UserDao
import com.warrior.data.local.dao.WorkoutActivityDao
import com.warrior.data.local.database.WarriorDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWarriorDatabase(@ApplicationContext context: Context): WarriorDatabase =
        Room.databaseBuilder(context, WarriorDatabase::class.java, WarriorDatabase.NAME)
            // Architecture Rule 11: destructive fallback is Debug-only, behind a
            // BuildConfig flag, and is wired in Phase 10 (release hardening).
            .build()

    @Provides
    fun provideUserDao(db: WarriorDatabase): UserDao = db.userDao()

    @Provides
    fun provideTrainingSessionDao(db: WarriorDatabase): TrainingSessionDao = db.trainingSessionDao()

    @Provides
    fun provideWorkoutActivityDao(db: WarriorDatabase): WorkoutActivityDao = db.workoutActivityDao()

    @Provides
    fun provideRoundDao(db: WarriorDatabase): RoundDao = db.roundDao()
}
