package com.warrior.domain.training

import kotlinx.coroutines.flow.Flow

/** Persisted defaults for the live round timer (Season 2 / Phase 14). */
data class TimerDefaults(
    val workSeconds: Int = 180,
    val restSeconds: Int = 60,
    val rounds: Int = 6,
)

interface TimerPreferences {
    val defaults: Flow<TimerDefaults>
    suspend fun setDefaults(defaults: TimerDefaults)
}
