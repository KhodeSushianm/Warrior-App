package com.warrior.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One round of a round-based activity. See Database Design v4 §4.
 * Durations are milliseconds; (activityId, roundNumber) is unique.
 */
@Entity(
    tableName = "rounds",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["activityId"]),
        Index(value = ["activityId", "roundNumber"], unique = true),
    ],
)
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: Long,
    val roundNumber: Int,
    val duration: Long,
    val restDuration: Long,
    val intensity: Int,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    init {
        require(roundNumber > 0) { "roundNumber must be > 0, was $roundNumber" }
        require(duration > 0) { "duration must be > 0, was $duration" }
        require(restDuration >= 0) { "restDuration must be >= 0, was $restDuration" }
        require(intensity in 1..10) { "intensity must be in 1..10, was $intensity" }
    }
}
