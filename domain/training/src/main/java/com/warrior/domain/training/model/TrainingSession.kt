package com.warrior.domain.training.model

import kotlin.time.Duration

/**
 * Domain aggregate root for one training session (Architecture v2.1 §11.2).
 *
 * `date` is the local training day as UTC epoch millis of local midnight
 * (DB v4 §9). `totalDuration` is always derived from activities — never stored.
 */
data class TrainingSession(
    val id: Long = 0,
    val userId: Long,
    val date: Long,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val overallIntensity: Int,
    val overallFeeling: Feeling,
    val notes: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val activities: List<WorkoutActivity> = emptyList(),
) {
    val totalDuration: Duration
        get() = activities.fold(Duration.ZERO) { acc, activity -> acc + activity.duration }

    val totalRounds: Int
        get() = activities.sumOf { it.rounds.size }
}
