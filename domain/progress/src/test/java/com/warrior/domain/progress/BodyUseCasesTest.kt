package com.warrior.domain.progress

import com.warrior.domain.progress.model.BodyErrorCode
import com.warrior.domain.progress.model.BodyMetric
import com.warrior.domain.progress.time.TimeProvider
import com.warrior.domain.progress.usecase.AddBodyMetric
import com.warrior.domain.progress.usecase.BodyValidation
import com.warrior.domain.progress.usecase.BodyValidationException
import com.warrior.domain.progress.usecase.DeleteBodyMetric
import com.warrior.domain.progress.usecase.GetLatestBodyMetric
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Body-metric use cases (Season 2 / Phase 12): validation codes + ownership. */
class BodyUseCasesTest {

    private val now = 1_790_164_800_000L // 2026-09-23T12:00Z
    private val clock = TimeProvider { now }

    private fun metric(weight: Float, height: Float? = null, date: Long = now) = BodyMetric(
        userId = 1,
        date = date,
        weightKg = weight,
        heightCm = height,
    )

    @Test
    fun validation_rangesAndCodes() {
        assertEquals(emptyList<BodyErrorCode>(), BodyValidation.validate(metric(78.5f), now))
        assertEquals(listOf(BodyErrorCode.WEIGHT_RANGE), BodyValidation.validate(metric(19f), now).toList())
        assertEquals(listOf(BodyErrorCode.WEIGHT_RANGE), BodyValidation.validate(metric(401f), now).toList())
        assertEquals(
            listOf(BodyErrorCode.HEIGHT_RANGE),
            BodyValidation.validate(metric(75f, height = 99f), now).toList(),
        )
        // more than a day in the future is a typo
        assertEquals(
            listOf(BodyErrorCode.INVALID_DATE),
            BodyValidation.validate(metric(75f, date = now + 2 * 86_400_000L), now).toList(),
        )
        // today-ish is fine
        assertEquals(emptyList<BodyErrorCode>(), BodyValidation.validate(metric(75f, date = now + 3_600_000L), now))
    }

    @Test
    fun add_scopesToUser_stampsTimestamps_andRejectsInvalid() = runTest {
        val repository = FakeBodyRepository()
        val add = AddBodyMetric(repository, clock)

        val id = add(7, metric(80f)).getOrThrow()
        val stored = repository.observeMetrics(7).first().single()
        assertEquals(id, stored.id)
        assertEquals(7, stored.userId)
        assertEquals(now, stored.createdAt)

        val failure = add(7, metric(5f))
        val error = failure.exceptionOrNull()
        assertTrue(error is BodyValidationException)
        assertEquals(listOf(BodyErrorCode.WEIGHT_RANGE), (error as BodyValidationException).codes)
        assertEquals(1, repository.observeMetrics(7).first().size) // nothing added
    }

    @Test
    fun delete_ownershipEnforced_andLatestFollowsHistory() = runTest {
        val repository = FakeBodyRepository()
        val add = AddBodyMetric(repository, clock)
        val delete = DeleteBodyMetric(repository)
        val latest = GetLatestBodyMetric(repository)

        val first = add(1, metric(80f, date = now - 86_400_000L)).getOrThrow()
        add(1, metric(79f, date = now)).getOrThrow()
        assertEquals(79f, latest(1)?.weightKg)

        // other user's delete attempt fails
        assertEquals(false, delete(2, first).getOrThrow())
        // owner deletes the newest -> latest falls back
        val newest = repository.observeMetrics(1).first().first().id
        assertEquals(true, delete(1, newest).getOrThrow())
        assertEquals(80f, latest(1)?.weightKg)
    }

    private class FakeBodyRepository : BodyRepository {
        private val items = mutableMapOf<Long, BodyMetric>()
        private val state = MutableStateFlow<List<BodyMetric>>(emptyList())
        private var nextId = 1L

        override fun observeMetrics(userId: Long) = state.map { all ->
            all.filter { it.userId == userId }
                .sortedWith(compareByDescending<BodyMetric> { it.date }.thenByDescending { it.id })
        }

        override suspend fun addMetric(metric: BodyMetric): Long {
            val id = nextId++
            items[id] = metric.copy(id = id)
            state.value = items.values.toList()
            return id
        }

        override suspend fun deleteMetric(userId: Long, metricId: Long): Boolean {
            val existing = items[metricId] ?: return false
            if (existing.userId != userId) return false
            items.remove(metricId)
            state.value = items.values.toList()
            return true
        }

        override suspend fun latest(userId: Long): BodyMetric? = observeMetrics(userId).first().firstOrNull()
    }
}
