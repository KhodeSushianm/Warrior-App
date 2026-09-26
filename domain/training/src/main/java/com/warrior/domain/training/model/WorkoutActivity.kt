package com.warrior.domain.training.model

import kotlin.time.Duration

/**
 * One workout activity inside a session (Architecture v2.1 §11.3).
 * Durations are domain-level [Duration] values; entities store Long millis.
 */
data class WorkoutActivity(
    val id: Long = 0,
    val sessionId: Long = 0,
    val type: WorkoutType,
    val duration: Duration,
    val intensity: Int,
    val focusArea: FocusArea,
    val notes: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val rounds: List<Round> = emptyList(),
)
