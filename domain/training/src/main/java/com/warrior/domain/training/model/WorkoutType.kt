package com.warrior.domain.training.model

/**
 * Workout types. MVP ships the first four values; the rest are reserved for
 * post-MVP releases. See Database Design v4 §8 (Enum Stability Policy):
 * released values are never renamed or removed.
 */
enum class WorkoutType {
    CARDIO,
    HEAVY_BAG,
    MITT_WORK,
    SPARRING,
}
