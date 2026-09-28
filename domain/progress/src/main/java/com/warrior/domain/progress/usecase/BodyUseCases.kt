package com.warrior.domain.progress.usecase

import com.warrior.domain.progress.BodyRepository
import com.warrior.domain.progress.model.BodyErrorCode
import com.warrior.domain.progress.model.BodyMetric
import com.warrior.domain.progress.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Body-metric validation rules; codes keep the UI string-resource-driven. */
object BodyValidation {

    fun validate(metric: BodyMetric, nowMillis: Long): List<BodyErrorCode> {
        val errors = mutableListOf<BodyErrorCode>()
        if (metric.weightKg !in 20f..400f) errors += BodyErrorCode.WEIGHT_RANGE
        metric.heightCm?.takeIf { it !in 100f..250f }?.let { errors += BodyErrorCode.HEIGHT_RANGE }
        metric.reachCm?.takeIf { it !in 100f..250f }?.let { errors += BodyErrorCode.REACH_RANGE }
        metric.bodyFatPercent?.takeIf { it !in 3f..70f }?.let { errors += BodyErrorCode.BODY_FAT_RANGE }
        metric.restingHeartRate?.takeIf { it !in 30..220 }?.let { errors += BodyErrorCode.HEART_RATE_RANGE }
        // A day in the future (beyond today's local midnight boundary) is a typo.
        if (metric.date > nowMillis + 86_400_000L) errors += BodyErrorCode.INVALID_DATE
        return errors
    }
}

/** Thrown when a body metric violates domain rules. */
class BodyValidationException(val codes: List<BodyErrorCode>) :
    IllegalArgumentException("invalid body metric: $codes")

class ObserveBodyMetrics @Inject constructor(private val repository: BodyRepository) {
    operator fun invoke(userId: Long): Flow<List<BodyMetric>> = repository.observeMetrics(userId)
}

class AddBodyMetric @Inject constructor(
    private val repository: BodyRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(userId: Long, metric: BodyMetric): Result<Long> = runCatching {
        val scoped = metric.copy(userId = userId, id = 0)
        val errors = BodyValidation.validate(scoped, timeProvider.nowMillis())
        if (errors.isNotEmpty()) throw BodyValidationException(errors)
        val now = timeProvider.nowMillis()
        repository.addMetric(scoped.copy(createdAt = now, updatedAt = now))
    }
}

class DeleteBodyMetric @Inject constructor(private val repository: BodyRepository) {
    suspend operator fun invoke(userId: Long, metricId: Long): Result<Boolean> = runCatching {
        repository.deleteMetric(userId, metricId)
    }
}

class GetLatestBodyMetric @Inject constructor(private val repository: BodyRepository) {
    suspend operator fun invoke(userId: Long): BodyMetric? = repository.latest(userId)
}
