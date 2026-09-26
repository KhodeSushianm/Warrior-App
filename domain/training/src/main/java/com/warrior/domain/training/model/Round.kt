package com.warrior.domain.training.model

import kotlin.time.Duration

/**
 * One round of a round-based activity (Architecture v2.1 §11.4).
 */
data class Round(
    val id: Long = 0,
    val activityId: Long = 0,
    val roundNumber: Int,
    val duration: Duration,
    val restDuration: Duration,
    val intensity: Int,
    val notes: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
