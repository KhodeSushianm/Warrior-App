package com.warrior.domain.progress

import com.warrior.domain.progress.model.BodyMetric
import kotlinx.coroutines.flow.Flow

/**
 * Body-metric storage boundary (Season 2 / Phase 12). Ownership-scoped like
 * every other repository; the Room implementation lives in :data:local.
 */
interface BodyRepository {

    fun observeMetrics(userId: Long): Flow<List<BodyMetric>>

    /** Inserts a checkpoint; returns the new id. */
    suspend fun addMetric(metric: BodyMetric): Long

    /** Returns false when the metric does not belong to [userId] or is absent. */
    suspend fun deleteMetric(userId: Long, metricId: Long): Boolean

    suspend fun latest(userId: Long): BodyMetric?
}
