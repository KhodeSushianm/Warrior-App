package com.warrior.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Free-form tag on a training session (Season 2 / Phase 12, schema v2).
 * Created with the v2 migration ahead of the search/tags feature (Phase 17)
 * so the schema evolves once, with a tested migration, instead of twice.
 * `userId` is denormalized for ownership-scoped queries without a join.
 */
@Serializable
@Entity(
    tableName = "session_tags",
    foreignKeys = [
        ForeignKey(
            entity = TrainingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["userId", "tag"]),
    ],
)
data class SessionTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val userId: Long,
    val tag: String,
    val createdAt: Long = 0,
)
