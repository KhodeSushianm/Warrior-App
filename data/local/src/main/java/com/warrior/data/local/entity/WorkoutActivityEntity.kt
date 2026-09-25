package com.warrior.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.WorkoutType

/**
 * One workout activity inside a session. See Database Design v4 §3.
 * `duration` is milliseconds and must be > 0.
 */
@Entity(
    tableName = "workout_activities",
    foreignKeys = [
        ForeignKey(
            entity = TrainingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionId"])],
)
data class WorkoutActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val type: WorkoutType,
    val duration: Long,
    val intensity: Int,
    val focusArea: FocusArea,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    init {
        require(duration > 0) { "duration must be > 0, was $duration" }
        require(intensity in 1..10) { "intensity must be in 1..10, was $intensity" }
    }
}
