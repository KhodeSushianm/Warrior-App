package com.warrior.feature.profile

import com.warrior.domain.auth.LocalSession
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.progress.BodyRepository
import com.warrior.domain.progress.model.BodyErrorCode
import com.warrior.domain.progress.model.BodyMetric
import com.warrior.domain.progress.time.TimeProvider
import com.warrior.domain.progress.usecase.AddBodyMetric
import com.warrior.domain.progress.usecase.DeleteBodyMetric
import com.warrior.domain.progress.usecase.ObserveBodyMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Athlete body VM (Season 2 / Phase 12): add/validate/delete + trend math. */
class BodyViewModelTest {

    private lateinit var body: FakeBodyRepository
    private lateinit var session: FakeBodyLocalSession

    // Wed 2026-09-23 12:00 UTC.
    private val now = 1_790_164_800_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        body = FakeBodyRepository()
        session = FakeBodyLocalSession()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = BodyViewModel(
        observeSession = ObserveSession(session),
        observeBodyMetrics = ObserveBodyMetrics(body),
        addBodyMetric = AddBodyMetric(body, TimeProvider { now }),
        deleteBodyMetric = DeleteBodyMetric(body),
        timeProvider = TimeProvider { now },
    )

    @Test
    fun addMetrics_liveListLatestPreviousAndTrend() = runTest {
        session.start(1)
        val viewModel = buildViewModel()

        viewModel.onAddOpen()
        viewModel.onWeightChange("78.5")
        viewModel.onHeightChange("182")
        viewModel.onSave()

        assertEquals(1, viewModel.state.value.metrics.size)
        assertEquals(78.5f, viewModel.state.value.latest?.weightKg)
        assertEquals(182f, viewModel.state.value.latest?.heightCm)
        assertTrue(viewModel.state.value.trendPoints.isEmpty()) // single point -> no chart

        viewModel.onAddOpen()
        viewModel.onWeightChange("77,9") // comma decimal separator accepted
        viewModel.onSave()

        val state = viewModel.state.value
        assertEquals(2, state.metrics.size)
        assertEquals(77.9f, state.latest?.weightKg) // newest first (same date, higher id)
        assertEquals(78.5f, state.previousWeightKg)
        // chronological [78.5, 77.9] normalized -> [1.0, 0.0]
        assertEquals(listOf(1.0f, 0.0f), state.trendPoints)
    }

    @Test
    fun invalidWeight_surfacesCodeAndKeepsDialogOpen() = runTest {
        session.start(1)
        val viewModel = buildViewModel()

        viewModel.onAddOpen()
        viewModel.onWeightChange("abc")
        viewModel.onSave()
        assertEquals(listOf(BodyErrorCode.WEIGHT_RANGE), viewModel.state.value.errorCodes)
        assertTrue(viewModel.state.value.showDialog)

        viewModel.onWeightChange("900") // out of 20..400
        viewModel.onSave()
        assertEquals(listOf(BodyErrorCode.WEIGHT_RANGE), viewModel.state.value.errorCodes)
    }

    @Test
    fun invalidOptional_surfacesSpecificCode() = runTest {
        session.start(1)
        val viewModel = buildViewModel()

        viewModel.onAddOpen()
        viewModel.onWeightChange("75")
        viewModel.onHeightChange("50") // below 100cm floor
        viewModel.onSave()

        assertEquals(listOf(BodyErrorCode.HEIGHT_RANGE), viewModel.state.value.errorCodes)
        assertTrue(viewModel.state.value.showDialog)
    }

    @Test
    fun deleteMetric_removesAfterConfirm() = runTest {
        session.start(1)
        val viewModel = buildViewModel()
        viewModel.onAddOpen()
        viewModel.onWeightChange("80")
        viewModel.onSave()
        val id = viewModel.state.value.metrics.single().id

        viewModel.onDeleteRequest(id)
        assertEquals(id, viewModel.state.value.pendingDelete)
        viewModel.onDeleteConfirm()

        assertTrue(viewModel.state.value.metrics.isEmpty())
        assertEquals(null, viewModel.state.value.pendingDelete)
    }

    @Test
    fun signedOut_noMetrics() = runTest {
        val viewModel = buildViewModel()
        assertTrue(viewModel.state.value.loaded)
        assertTrue(viewModel.state.value.metrics.isEmpty())
    }

    private class FakeBodyRepository : BodyRepository {
        private val items = mutableMapOf<Long, BodyMetric>()
        private val state = MutableStateFlow<List<BodyMetric>>(emptyList())
        private var nextId = 1L

        override fun observeMetrics(userId: Long): Flow<List<BodyMetric>> =
            state.map { all ->
                all.filter { it.userId == userId }
                    .sortedWith(compareByDescending<BodyMetric> { it.date }.thenByDescending { it.id })
            }

        override suspend fun addMetric(metric: BodyMetric): Long {
            val id = nextId++
            items[id] = metric.copy(id = id)
            publish()
            return id
        }

        override suspend fun deleteMetric(userId: Long, metricId: Long): Boolean {
            val existing = items[metricId] ?: return false
            if (existing.userId != userId) return false
            items.remove(metricId)
            publish()
            return true
        }

        override suspend fun latest(userId: Long): BodyMetric? = items.values
            .filter { it.userId == userId }
            .sortedWith(compareByDescending<BodyMetric> { it.date }.thenByDescending { it.id })
            .firstOrNull()

        private fun publish() {
            state.value = items.values.toList()
        }
    }

    private class FakeBodyLocalSession : LocalSession {
        private val state = MutableStateFlow<Long?>(null)
        override val currentUserId: Flow<Long?> = state

        override suspend fun start(userId: Long) {
            state.value = userId
        }

        override suspend fun clear() {
            state.value = null
        }
    }

    @Test
    fun bmi_derivedFromLatestWeightAndLatestKnownHeight() = runTest {
        session.start(1)
        val viewModel = buildViewModel()

        // No height recorded yet -> no BMI.
        viewModel.onAddOpen()
        viewModel.onWeightChange("80")
        viewModel.onSave()
        assertNull(viewModel.state.value.bmi)

        // Height enters the history -> BMI = 77 / 1.80^2.
        viewModel.onAddOpen()
        viewModel.onWeightChange("77")
        viewModel.onHeightChange("180")
        viewModel.onSave()
        assertEquals(77f / (1.8f * 1.8f), viewModel.state.value.bmi!!, 0.01f)

        // A later weight-only measurement reuses the latest known height.
        viewModel.onAddOpen()
        viewModel.onWeightChange("75")
        viewModel.onSave()
        assertEquals(75f / (1.8f * 1.8f), viewModel.state.value.bmi!!, 0.01f)
    }
}
