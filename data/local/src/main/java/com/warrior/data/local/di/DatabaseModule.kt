package com.warrior.data.local.di

import android.content.Context
import androidx.room.Room
import com.warrior.data.local.BuildConfig
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
            // Architecture Rule 11 (wired in Phase 10): destructive fallback only
            // in Debug behind the module's BuildConfig flag — release builds fail
            // loudly on a missing migration instead of destroying user data.
            .apply {
                if (BuildConfig.ALLOW_DESTRUCTIVE_MIGRATION) {
                    fallbackToDestructiveMigration()
                }
            }
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
