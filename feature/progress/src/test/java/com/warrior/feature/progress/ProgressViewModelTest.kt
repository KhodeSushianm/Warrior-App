package com.warrior.feature.progress

import com.warrior.domain.auth.LocalSession
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.progress.ProgressCalculator
import com.warrior.domain.progress.time.TimeProvider
import com.warrior.domain.progress.usecase.ObserveProgress
import com.warrior.domain.progress.week.WeekBoundaryProvider
import com.warrior.domain.training.TrainingRepository
import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutActivity
import com.warrior.domain.training.model.WorkoutType
import com.warrior.domain.training.usecase.CreateTrainingSession
import com.warrior.domain.training.usecase.GetTrainingHistory
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
import java.util.Calendar
import java.util.TimeZone
import kotlin.time.Duration.Companion.minutes

/**
 * Progress screen VM (Phase 8): charts render from the derived flow; volume
 * fractions are normalized against the max week (hand-computed fixtures).
 */
class ProgressViewModelTest {

    private lateinit var repository: FakeTrainingRepository
    private lateinit var session: FakeLocalSession
    private lateinit var defaultZone: TimeZone

    // Wed 2026-09-23 12:00 UTC -> current week is Sat Sep 19 .. Fri Sep 25.
    private val now = utcMillis(2026, 9, 23, hour = 12)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        defaultZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        repository = FakeTrainingRepository()
        session = FakeLocalSession()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultZone)
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = ProgressViewModel(
        ObserveSession(session),
        ObserveProgress(
            getTrainingHistory = GetTrainingHistory(repository),
            calculator = ProgressCalculator(WeekBoundaryProvider(TimeZone.getTimeZone("UTC"))),
            timeProvider = TimeProvider { now },
        ),
    )

    private fun workout(date: Long, type: WorkoutType, minutes: Long, focus: FocusArea, overall: Int) =
        TrainingSession(
            userId = 1,
            date = date,
            overallIntensity = overall,
            overallFeeling = Feeling.GOOD,
            activities = listOf(
                WorkoutActivity(
                    type = type,
                    duration = minutes.minutes,
                    intensity = overall,
                    focusArea = focus,
                ),
            ),
        )

    @Test
    fun snapshot_comparisonLabelVolumeFractionsAndDistributions() = runTest {
        session.start(1)
        val create = CreateTrainingSession(repository)
        create(1, workout(utcMillis(2026, 9, 19), WorkoutType.HEAVY_BAG, 50, FocusArea.POWER, overall = 9))
        create(1, workout(utcMillis(2026, 9, 21), WorkoutType.CARDIO, 40, FocusArea.CONDITIONING, overall = 7))
        create(1, workout(utcMillis(2026, 9, 12), WorkoutType.HEAVY_BAG, 45, FocusArea.POWER, overall = 8))

        val state = buildViewModel().state.value

        assertTrue(state.loaded)
        assertEquals("Sep 19 – Sep 25", state.weekRangeLabel)

        val snapshot = state.snapshot!!
        assertEquals(2, snapshot.thisWeek.sessionCount)
        assertEquals(90L, snapshot.thisWeek.trainingMinutes) // 50 + 40
        assertEquals(45L, snapshot.lastWeek.trainingMinutes)

        // 8 bars (Aug 1 .. Sep 19): max = 90 (NOW) -> W-1 (Sep 12, 45m) = 0.5.
        assertEquals(8, state.volumeFractions.size)
        assertEquals(0f, state.volumeFractions[5], 0.0001f)
        assertEquals(0.5f, state.volumeFractions[6], 0.0001f)
        assertEquals(1.0f, state.volumeFractions[7], 0.0001f)

        // All-time distributions (enum order for types).
        assertEquals(
            listOf(WorkoutType.CARDIO, WorkoutType.HEAVY_BAG),
            snapshot.workoutDistribution.keys.toList(),
        )
        assertEquals(listOf(40L, 95L), snapshot.workoutDistribution.values.toList())
        assertEquals(
            listOf(FocusArea.POWER to 95L, FocusArea.CONDITIONING to 40L),
            snapshot.topFocusAreas.map { it.focus to it.minutes },
        )
    }

    @Test
    fun newSession_reshapesVolumeFractionsLive() = runTest {
        session.start(1)
        val create = CreateTrainingSession(repository)
        create(1, workout(utcMillis(2026, 9, 12), WorkoutType.HEAVY_BAG, 60, FocusArea.POWER, overall = 8))

        val viewModel = buildViewModel()
        // Only W-1 has data -> it is the max (1.0) and NOW is 0.
        assertEquals(1.0f, viewModel.state.value.volumeFractions[6], 0.0001f)
        assertEquals(0f, viewModel.state.value.volumeFractions[7], 0.0001f)

        create(1, workout(utcMillis(2026, 9, 21), WorkoutType.CARDIO, 30, FocusArea.CONDITIONING, overall = 7))
        // NOW = 30, W-1 = 60 -> max 60: fractions 0.5 (NOW) and 1.0 (W-1).
        assertEquals(1.0f, viewModel.state.value.volumeFractions[6], 0.0001f)
        assertEquals(0.5f, viewModel.state.value.volumeFractions[7], 0.0001f)
        assertEquals(30L, viewModel.state.value.snapshot?.thisWeek?.trainingMinutes)
    }

    @Test
    fun emptyHistory_allZeroFractions() = runTest {
        session.start(1)
        val state = buildViewModel().state.value

        assertTrue(state.loaded)
        assertEquals("Sep 19 – Sep 25", state.weekRangeLabel)
        assertEquals(8, state.volumeFractions.size)
        assertTrue(state.volumeFractions.all { it == 0f })
        assertTrue(state.snapshot!!.workoutDistribution.isEmpty())
        assertTrue(state.snapshot!!.topFocusAreas.isEmpty())
    }

    @Test
    fun signedOut_noSnapshot() = runTest {
        val state = buildViewModel().state.value
        assertTrue(state.loaded)
        assertNull(state.snapshot)
        assertEquals("", state.weekRangeLabel)
        assertTrue(state.volumeFractions.isEmpty())
    }

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int = 0): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            .apply {
                clear()
                set(year, month - 1, day, hour, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

    private class FakeLocalSession : LocalSession {
        private val state = MutableStateFlow<Long?>(null)
        override val currentUserId: Flow<Long?> = state

        override suspend fun start(userId: Long) {
            state.value = userId
        }

        override suspend fun clear() {
            state.value = null
        }
    }
}

/** In-memory repository fake (mirrors the other feature-module test fakes). */
internal class FakeTrainingRepository : TrainingRepository {

    private val sessions = mutableMapOf<Long, TrainingSession>()
    private val state = MutableStateFlow<List<TrainingSession>>(emptyList())
    private var nextId = 1L

    override fun observeSessions(userId: Long): Flow<List<TrainingSession>> =
        state.map { all -> all.filter { it.userId == userId } }

    override suspend fun getSession(userId: Long, sessionId: Long): TrainingSession? =
        sessions[sessionId]?.takeIf { it.userId == userId }

    override suspend fun createSession(session: TrainingSession): Long {
        val id = nextId++
        sessions[id] = session.copy(id = id)
        publish()
        return id
    }

    override suspend fun updateSession(session: TrainingSession) {
        check(sessions.containsKey(session.id)) { "session ${session.id} does not exist" }
        sessions[session.id] = session
        publish()
    }

    override suspend fun deleteSession(userId: Long, sessionId: Long): Boolean {
        val existing = sessions[sessionId] ?: return false
        if (existing.userId != userId) return false
        sessions.remove(sessionId)
        publish()
        return true
    }

    private fun publish() {
        state.value = sessions.values.toList()
    }
}
