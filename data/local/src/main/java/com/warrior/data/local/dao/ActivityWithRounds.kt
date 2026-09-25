package com.warrior.data.local.dao

import com.warrior.data.local.entity.RoundEntity
import com.warrior.data.local.entity.WorkoutActivityEntity

/**
 * Pairs one activity with its rounds for atomic session creation, replacing the
 * earlier index-keyed Map contract (Architecture v2.1 §15.2 / DB v4 §14).
 */
data class ActivityWithRounds(
    val activity: WorkoutActivityEntity,
    val rounds: List<RoundEntity> = emptyList(),
)
