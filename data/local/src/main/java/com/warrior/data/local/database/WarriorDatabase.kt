package com.warrior.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.warrior.data.local.converter.Converters
import com.warrior.data.local.dao.RoundDao
import com.warrior.data.local.dao.TrainingSessionDao
import com.warrior.data.local.dao.UserDao
import com.warrior.data.local.dao.WorkoutActivityDao
import com.warrior.data.local.entity.RoundEntity
import com.warrior.data.local.entity.TrainingSessionEntity
import com.warrior.data.local.entity.UserEntity
import com.warrior.data.local.entity.WorkoutActivityEntity

/**
 * WARRIOR local database — Schema v1 (FROZEN at Phase 2).
 *
 * exportSchema = true: schema JSON files are committed under data/local/schemas
 * so every future migration is testable (Architecture Rule 11, DB v4 §21).
 * fallbackToDestructiveMigration() must never be enabled in release builds.
 */
@Database(
    entities = [
        UserEntity::class,
        TrainingSessionEntity::class,
        WorkoutActivityEntity::class,
        RoundEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WarriorDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun trainingSessionDao(): TrainingSessionDao

    abstract fun workoutActivityDao(): WorkoutActivityDao

    abstract fun roundDao(): RoundDao

    companion object {
        const val NAME = "warrior.db"
    }
}
