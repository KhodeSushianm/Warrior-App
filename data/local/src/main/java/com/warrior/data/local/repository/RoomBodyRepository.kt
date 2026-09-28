package com.warrior.data.local.repository

import com.warrior.data.local.dao.BodyMetricDao
import com.warrior.data.local.mapper.toDomain
import com.warrior.data.local.mapper.toEntity
import com.warrior.domain.progress.BodyRepository
import com.warrior.domain.progress.model.BodyMetric
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomBodyRepository @Inject constructor(
    private val bodyMetricDao: BodyMetricDao,
) : BodyRepository {

    override fun observeMetrics(userId: Long): Flow<List<BodyMetric>> =
        bodyMetricDao.observeByUser(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun addMetric(metric: BodyMetric): Long =
        bodyMetricDao.insert(metric.toEntity())

    override suspend fun deleteMetric(userId: Long, metricId: Long): Boolean =
        bodyMetricDao.deleteById(userId, metricId) > 0

    override suspend fun latest(userId: Long): BodyMetric? =
        bodyMetricDao.latest(userId)?.toDomain()
}
