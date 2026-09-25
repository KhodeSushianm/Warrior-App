package com.warrior.data.local.converter

import androidx.room.TypeConverter
import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.WorkoutType

/**
 * Enum ↔ String converters (Database Design v4 §10).
 *
 * Timestamps and durations are stored as plain Long (UTC epoch millis /
 * milliseconds) per the schema tables; Instant/Duration mapping happens in the
 * domain mappers (Phase 3), keeping entities schema-exact.
 */
class Converters {

    @TypeConverter
    fun workoutTypeToString(value: WorkoutType): String = value.name

    @TypeConverter
    fun stringToWorkoutType(value: String): WorkoutType = WorkoutType.valueOf(value)

    @TypeConverter
    fun focusAreaToString(value: FocusArea): String = value.name

    @TypeConverter
    fun stringToFocusArea(value: String): FocusArea = FocusArea.valueOf(value)

    @TypeConverter
    fun feelingToString(value: Feeling): String = value.name

    @TypeConverter
    fun stringToFeeling(value: String): Feeling = Feeling.valueOf(value)
}
