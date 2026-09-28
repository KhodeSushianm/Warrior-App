package com.warrior.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.warrior.domain.training.model.Feeling
import kotlinx.serialization.Serializable

/**
 * One training session. See Database Design v4 §2.
 *
 * NOTE: there is intentionally no `totalDuration` column — session duration is
 * derived as SUM(workout_activities.duration) (Architecture v2.1 §11.2, DB v4 §18).
 *
 * `date` is the local training day: local midnight stored as UTC epoch millis.
 */
@Serializable
@Entity(
    tableName = "training_sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "date"]),
    ],
)
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val date: Long,
    val startedAt: Long?,
    val endedAt: Long?,
    val overallIntensity: Int,
    val overallFeeling: Feeling,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    init {
        require(overallIntensity in 1..10) { "overallIntensity must be in 1..10, was $overallIntensity" }
    }
}
