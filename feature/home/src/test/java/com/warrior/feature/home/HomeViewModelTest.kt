package com.warrior.feature.home

import com.warrior.domain.auth.LocalSession
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.progress.ProgressCalculator
import com.warrior.domain.progress.time.TimeProvider
import com.warrior.domain.progress.usecase.ObserveHomeProgress
import com.warrior.domain.progress.week.WeekBoundaryProvider
import com.warrior.domain.training.TrainingRepository
import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.Round
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
 * Home dashboard VM (Phase 7): the snapshot is live-derived, week framing is
 * Sat→Fri, and labels use the device zone (UTC pinned here for determinism).
 */
class HomeViewModelTest {

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

    private fun buildViewModel() = HomeViewModel(
        ObserveSession(session),
        ObserveHomeProgress(
            getTrainingHistory = GetTrainingHistory(repository),
            calculator = ProgressCalculator(WeekBoundaryProvider(TimeZone.getTimeZone("UTC"))),
            timeProvider = TimeProvider { now },
        ),
    )

    private fun heavyBagSession(date: Long, minutes: Long, rounds: Int, overall: Int) = TrainingSession(
        userId = 1,
        date = date,
        overallIntensity = overall,
        overallFeeling = Feeling.GOOD,
        activities = listOf(
            WorkoutActivity(
                type = WorkoutType.HEAVY_BAG,
                duration = minutes.minutes,
                intensity = overall,
                focusArea = FocusArea.POWER,
                rounds = (1..rounds).map { n ->
                    Round(
                        roundNumber = n,
                        duration = 3.minutes,
                        restDuration = 1.minutes,
                        intensity = overall,
                    )
                },
            ),
        ),
    )

    @Test
    fun dashboardSnapshot_weekStatsRecordsAndLabels() = runTest {
        session.start(1)
        val create = CreateTrainingSession(repository)
        create(1, heavyBagSession(date = utcMillis(2026, 9, 19), minutes = 50, rounds = 5, overall = 9))
        create(1, heavyBagSession(date = utcMillis(2026, 9, 21), minutes = 40, rounds = 0, overall = 7))

        val state = buildViewModel().state.value

        assertTrue(state.loaded)
        assertEquals("Sep 19 – Sep 25", state.weekRangeLabel)
        val progress = state.progress!!
        assertEquals(2, progress.thisWeek.sessionCount)
        assertEquals(90L, progress.thisWeek.trainingMinutes)
        assertEquals(5, progress.thisWeek.totalRounds)
        assertEquals(8, progress.thisWeek.averageIntensity) // (9+7)/2 = 8
        assertEquals(1, progress.streakWeeks) // only the current week trained
        assertEquals(50L, progress.records.longestSessionMinutes)
        // Recent: newest date first (ids assigned in creation order).
        assertEquals(listOf(2L, 1L), progress.recentSessions.map { it.id })
    }

    @Test
    fun newSessionFlowsIntoTheDashboardImmediately() = runTest {
        session.start(1)
        val create = CreateTrainingSession(repository)
        create(1, heavyBagSession(date = utcMillis(2026, 9, 19), minutes = 30, rounds = 3, overall = 8))

        val viewModel = buildViewModel()
        assertEquals(1, viewModel.state.value.progress?.thisWeek?.sessionCount)

        create(1, heavyBagSession(date = utcMillis(2026, 9, 21), minutes = 45, rounds = 4, overall = 8))
        val progress = viewModel.state.value.progress!!
        assertEquals(2, progress.thisWeek.sessionCount)
        assertEquals(75L, progress.thisWeek.trainingMinutes)
        assertEquals(45L, progress.records.longestSessionMinutes)
    }

    @Test
    fun emptyHistory_showsEmptyButLoadedDashboard() = runTest {
        session.start(1)
        val state = buildViewModel().state.value

        assertTrue(state.loaded)
        assertEquals("Sep 19 – Sep 25", state.weekRangeLabel)
        assertEquals(0, state.progress?.thisWeek?.sessionCount)
        assertTrue(state.progress!!.records.isEmpty)
        assertEquals(0, state.progress!!.streakWeeks)
        assertTrue(state.progress!!.recentSessions.isEmpty())
    }

    @Test
    fun signedOut_noProgressSnapshot() = runTest {
        val state = buildViewModel().state.value
        assertTrue(state.loaded)
        assertNull(state.progress)
        assertEquals("", state.weekRangeLabel)
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
