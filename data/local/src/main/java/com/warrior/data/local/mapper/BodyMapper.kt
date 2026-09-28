package com.warrior.data.local.mapper

import com.warrior.data.local.entity.BodyMetricEntity
import com.warrior.domain.progress.model.BodyMetric

fun BodyMetricEntity.toDomain(): BodyMetric = BodyMetric(
    id = id,
    userId = userId,
    date = date,
    weightKg = weightKg,
    heightCm = heightCm,
    reachCm = reachCm,
    bodyFatPercent = bodyFatPercent,
    restingHeartRate = restingHeartRate,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun BodyMetric.toEntity(): BodyMetricEntity = BodyMetricEntity(
    id = id,
    userId = userId,
    date = date,
    weightKg = weightKg,
    heightCm = heightCm,
    reachCm = reachCm,
    bodyFatPercent = bodyFatPercent,
    restingHeartRate = restingHeartRate,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
