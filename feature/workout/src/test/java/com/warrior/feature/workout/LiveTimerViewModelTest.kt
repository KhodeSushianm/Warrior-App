package com.warrior.feature.workout

import com.warrior.domain.auth.LocalSession
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.progress.time.TimeProvider
import com.warrior.domain.training.TimerDefaults
import com.warrior.domain.training.TimerPreferences
import com.warrior.domain.training.TrainingRepository
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutType
import com.warrior.domain.training.usecase.CreateTrainingSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Live timer state machine (Season 2 / Phase 14) — driven entirely by a fake
 * clock: no real delays anywhere.
 */
class LiveTimerViewModelTest {

    private lateinit var repository: FakeTimerRepository
    private lateinit var session: FakeTimerSession
    private lateinit var prefs: FakeTimerPreferences
    private var nowMillis = 1_790_164_800_000L // 2026-09-23T12:00Z

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeTimerRepository()
        session = FakeTimerSession()
        prefs = FakeTimerPreferences()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): LiveTimerViewModel {
        val vm = LiveTimerViewModel(
            observeSession = ObserveSession(session),
            createTrainingSession = CreateTrainingSession(repository),
            timerPreferences = prefs,
            timeProvider = TimeProvider { nowMillis },
        )
        // init{} collects defaults eagerly under the unconfined dispatcher.
        return vm
    }

    private fun advance(millis: Long) {
        nowMillis += millis
    }

    @Test
    fun config_defaultsFromPreferences_andSteppersClamp() = runTest {
        prefs.setDefaults(TimerDefaults(workSeconds = 120, restSeconds = 45, rounds = 4))
        val vm = buildViewModel()

        assertEquals(120, vm.state.value.config.workSeconds)
        assertEquals(45, vm.state.value.config.restSeconds)
        assertEquals(4, vm.state.value.config.rounds)

        vm.onWorkChange(-5)
        assertEquals(115, vm.state.value.config.workSeconds)
        repeat(40) { vm.onWorkChange(-5) } // clamps at 5
        assertEquals(5, vm.state.value.config.workSeconds)
        vm.onRoundsChange(-10) // clamps at 1
        assertEquals(1, vm.state.value.config.rounds)
    }

    @Test
    fun run_countdownAndAutoAdvance_workRestNextRound() = runTest {
        prefs.setDefaults(TimerDefaults(workSeconds = 10, restSeconds = 5, rounds = 3))
        val vm = buildViewModel()

        vm.onStart()
        assertEquals(TimerStatus.RUNNING, vm.state.value.status)
        assertEquals(TimerPhase.WORK, vm.state.value.phase)
        assertEquals(10_000L, vm.state.value.remainingMillis)

        advance(4_000)
        vm.tick()
        assertEquals(6_000L, vm.state.value.remainingMillis)

        // Cross into REST (with 1s overshoot carried over).
        advance(7_000) // total 11s: work ended 1s ago
        vm.tick()
        assertEquals(TimerPhase.REST, vm.state.value.phase)
        assertEquals(4_000L, vm.state.value.remainingMillis) // 5s rest - 1s overshoot
        assertEquals(0, vm.state.value.roundIndex)

        // Cross into round 2 WORK.
        advance(4_000)
        vm.tick()
        assertEquals(TimerPhase.WORK, vm.state.value.phase)
        assertEquals(1, vm.state.value.roundIndex)
        assertEquals(10_000L, vm.state.value.remainingMillis)
        assertEquals(TimerStatus.RUNNING, vm.state.value.status)
    }

    @Test
    fun pauseAndResume_freezesRemaining() = runTest {
        prefs.setDefaults(TimerDefaults(workSeconds = 60, restSeconds = 10, rounds = 2))
        val vm = buildViewModel()
        vm.onStart()

        advance(20_000)
        vm.tick()
        assertEquals(40_000L, vm.state.value.remainingMillis)

        vm.onPause()
        assertEquals(TimerStatus.PAUSED, vm.state.value.status)
        advance(60_000) // wall time passes while paused
        vm.tick() // tick is a no-op when paused
        assertEquals(40_000L, vm.state.value.remainingMillis)

        vm.onResume()
        advance(10_000)
        vm.tick()
        assertEquals(30_000L, vm.state.value.remainingMillis)
    }

    @Test
    fun fullRun_finishesAndSavesCorrectAggregate() = runTest {
        session.start(7)
        prefs.setDefaults(TimerDefaults(workSeconds = 10, restSeconds = 5, rounds = 2))
        val vm = buildViewModel()
        vm.onTypeChange(WorkoutType.SPARRING)
        vm.onStart()

        // round 1: work 10 + rest 5; round 2: work 10 -> finished
        advance(10_000)
        vm.tick()
        advance(5_000)
        vm.tick()
        advance(10_000)
        vm.tick()

        assertEquals(TimerStatus.FINISHED, vm.state.value.status)
        assertEquals(2, vm.state.value.completedRounds)

        vm.onSave()
        assertTrue(vm.state.value.isSaved)

        val saved = repository.observeSessions(7).first().single()

        assertEquals(2, saved.totalRounds)
        val activity = saved.activities.single()
        assertEquals(WorkoutType.SPARRING, activity.type)
        // block time = 2*10 + 1*5 = 25s
        assertEquals(25.seconds, activity.duration)
        assertEquals(10.seconds, activity.rounds[0].duration)
        assertEquals(5.seconds, activity.rounds[0].restDuration)
        assertEquals(listOf(1, 2), activity.rounds.map { it.roundNumber })
    }

    @Test
    fun endEarly_savesStartedRoundsOnly() = runTest {
        session.start(7)
        prefs.setDefaults(TimerDefaults(workSeconds = 60, restSeconds = 30, rounds = 8))
        val vm = buildViewModel()
        vm.onStart()

        advance(60_000) // round 1 work done
        vm.tick() // -> REST of round 1
        assertEquals(TimerPhase.REST, vm.state.value.phase)
        vm.onEndEarly()

        assertEquals(TimerStatus.FINISHED, vm.state.value.status)
        assertEquals(1, vm.state.value.completedRounds)

        vm.onSave()
        assertTrue(vm.state.value.isSaved)
    }

    @Test
    fun start_persistsConfigAsNextDefaults() = runTest {
        val vm = buildViewModel()
        vm.onWorkChange(-30) // 180-30 = 150
        vm.onStart()
        assertEquals(150, prefs.lastSaved?.workSeconds)
        assertEquals(60, prefs.lastSaved?.restSeconds)
        assertEquals(6, prefs.lastSaved?.rounds)
    }

    private class FakeTimerPreferences : TimerPreferences {
        private val state = MutableStateFlow(TimerDefaults())
        var lastSaved: TimerDefaults? = null
        override val defaults: Flow<TimerDefaults> = state
        override suspend fun setDefaults(defaults: TimerDefaults) {
            lastSaved = defaults
            state.value = defaults
        }
    }

    private class FakeTimerSession : LocalSession {
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

/** In-memory repository fake (mirrors the other feature-module fakes). */
internal class FakeTimerRepository : TrainingRepository {

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
