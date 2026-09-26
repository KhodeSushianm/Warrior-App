package com.warrior.domain.training.model

/** English display labels (v1 language = EN). UI layers must use these, never enum names. */
val WorkoutType.label: String
    get() = when (this) {
        WorkoutType.CARDIO -> "Cardio"
        WorkoutType.HEAVY_BAG -> "Heavy Bag"
        WorkoutType.MITT_WORK -> "Mitt Work"
        WorkoutType.SPARRING -> "Sparring"
    }

val FocusArea.label: String
    get() = when (this) {
        FocusArea.JAB -> "Jab"
        FocusArea.FOOTWORK -> "Footwork"
        FocusArea.DEFENSE -> "Defense"
        FocusArea.HEAD_MOVEMENT -> "Head Movement"
        FocusArea.COMBINATIONS -> "Combinations"
        FocusArea.POWER -> "Power"
        FocusArea.SPEED -> "Speed"
        FocusArea.CONDITIONING -> "Conditioning"
        FocusArea.TIMING -> "Timing"
        FocusArea.DISTANCE -> "Distance"
    }

val Feeling.label: String
    get() = when (this) {
        Feeling.EXCELLENT -> "Excellent"
        Feeling.GOOD -> "Good"
        Feeling.OKAY -> "Okay"
        Feeling.TIRED -> "Tired"
        Feeling.EXHAUSTED -> "Exhausted"
    }

/** Round-based workout types carry rounds (DB v4 / preview behavior). */
val WorkoutType.isRoundBased: Boolean
    get() = this != WorkoutType.CARDIO
