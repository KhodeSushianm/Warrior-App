package com.warrior.domain.training.model

/**
 * Technical focus areas for boxing training. In MVP each activity carries
 * exactly one focus; the domain keeps room for multi-focus later.
 * See Database Design v4 §8 (Enum Stability Policy).
 */
enum class FocusArea {
    JAB,
    FOOTWORK,
    DEFENSE,
    HEAD_MOVEMENT,
    COMBINATIONS,
    POWER,
    SPEED,
    CONDITIONING,
    TIMING,
    DISTANCE,
}
