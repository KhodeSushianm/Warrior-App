package com.warrior.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * One body-metric checkpoint (Season 2 / Phase 12, schema v2).
 *
 * `date` follows the sessions convention: the local day as UTC epoch millis of
 * local midnight (DB v4 §9). Only `weightKg` is mandatory; everything else is
 * optional so a quick daily weigh-in stays frictionless.
 */
@Serializable
@Entity(
    tableName = "body_metrics",
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
data class BodyMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val date: Long,
    val weightKg: Float,
    val heightCm: Float? = null,
    val reachCm: Float? = null,
    val bodyFatPercent: Float? = null,
    val restingHeartRate: Int? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
