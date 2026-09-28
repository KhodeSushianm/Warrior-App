package com.warrior.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.warrior.data.local.converter.Converters
import com.warrior.data.local.dao.BodyMetricDao
import com.warrior.data.local.dao.RoundDao
import com.warrior.data.local.dao.SessionTagDao
import com.warrior.data.local.dao.TrainingSessionDao
import com.warrior.data.local.dao.UserDao
import com.warrior.data.local.dao.WorkoutActivityDao
import com.warrior.data.local.entity.BodyMetricEntity
import com.warrior.data.local.entity.RoundEntity
import com.warrior.data.local.entity.SessionTagEntity
import com.warrior.data.local.entity.TrainingSessionEntity
import com.warrior.data.local.entity.UserEntity
import com.warrior.data.local.entity.WorkoutActivityEntity

/**
 * WARRIOR local database.
 *
 * v1 (FROZEN at Phase 2): users, training_sessions, workout_activities, rounds.
 * v2 (Season 2 / Phase 12): + body_metrics, session_tags — additive only, via
 * the tested [MIGRATION_1_2].
 *
 * exportSchema = true: schema JSON files are committed under data/local/schemas
 * so every migration is testable (Architecture Rule 11, DB v4 §21).
 * fallbackToDestructiveMigration() must never be enabled in release builds.
 */
@Database(
    entities = [
        UserEntity::class,
        TrainingSessionEntity::class,
        WorkoutActivityEntity::class,
        RoundEntity::class,
        BodyMetricEntity::class,
        SessionTagEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WarriorDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun trainingSessionDao(): TrainingSessionDao

    abstract fun workoutActivityDao(): WorkoutActivityDao

    abstract fun roundDao(): RoundDao

    abstract fun bodyMetricDao(): BodyMetricDao

    abstract fun sessionTagDao(): SessionTagDao

    companion object {
        const val NAME = "warrior.db"
    }
}
