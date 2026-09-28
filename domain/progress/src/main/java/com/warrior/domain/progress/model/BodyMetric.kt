package com.warrior.domain.progress.model

/**
 * One body-metric checkpoint (Season 2 / Phase 12). Kilograms/centimeters are
 * the canonical units; display formatting is a UI concern. Only [weightKg] is
 * mandatory — a quick weigh-in must stay frictionless.
 */
data class BodyMetric(
    val id: Long = 0,
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

/** Stable codes for body-metric validation errors (i18n-ready, like auth/training). */
enum class BodyErrorCode {
    WEIGHT_RANGE,
    HEIGHT_RANGE,
    REACH_RANGE,
    BODY_FAT_RANGE,
    HEART_RATE_RANGE,
    INVALID_DATE,
    UNEXPECTED,
}
