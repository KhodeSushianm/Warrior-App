package com.warrior.feature.workout

import androidx.lifecycle.SavedStateHandle
import com.warrior.domain.auth.LocalSession
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.training.TrainingRepository
import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutActivity
import com.warrior.domain.training.model.WorkoutType
import com.warrior.domain.training.usecase.CreateTrainingSession
import com.warrior.domain.training.usecase.DeleteTrainingSession
import com.warrior.domain.training.usecase.GetTrainingSession
import com.warrior.domain.training.usecase.UpdateTrainingSession
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class WorkoutLoggingViewModelTest {

    private lateinit var repository: FakeTrainingRepository
    private lateinit var session: FakeLocalSession

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeTrainingRepository()
        session = FakeLocalSession()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(sessionId: Long? = null): WorkoutLoggingViewModel =
        WorkoutLoggingViewModel(
            createTrainingSession = CreateTrainingSession(repository),
            updateTrainingSession = UpdateTrainingSession(repository),
            deleteTrainingSession = DeleteTrainingSession(repository),
            getTrainingSession = GetTrainingSession(repository),
            observeSession = ObserveSession(session),
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to sessionId)),
        )

    @Test
    fun addActivity_defaultsRoundsForRoundBasedTypes() {
        val vm = newViewModel()
        vm.onAddActivity()
        val activity = vm.state.value.activities[0]
        assertEquals(WorkoutType.HEAVY_BAG, activity.type)
        assertEquals(4, activity.rounds.size)
        assertEquals(listOf(1, 2, 3, 4), activity.rounds.map { it.roundNumber })

        vm.onActivityTypeChange(0, WorkoutType.CARDIO)
        assertTrue(vm.state.value.activities[0].rounds.isEmpty())

        vm.onActivityTypeChange(0, WorkoutType.SPARRING)
        assertEquals(4, vm.state.value.activities[0].rounds.size)
    }

    @Test
    fun removeRound_renumbersRemaining() {
        val vm = newViewModel()
        vm.onAddActivity()
        vm.onAddRound(0)
        assertEquals(5, vm.state.value.activities[0].rounds.size)
        vm.onRemoveRound(0, 1)
        val rounds = vm.state.value.activities[0].rounds
        assertEquals(listOf(1, 2, 3, 4), rounds.map { it.roundNumber })
    }

    @Test
    fun save_createsSessionThroughSingleTransactionalEntry() = runTest {
        session.start(7)
        val vm = newViewModel()
        vm.onAddActivity()
        vm.onIntensityChange(9)
        vm.onFeelingChange(Feeling.EXCELLENT)
        vm.onSave()

        assertTrue(vm.state.value.isSaved)
        val stored = repository.observeSessions(7).first()
        assertEquals(1, stored.size)
        assertEquals(7L, stored[0].userId)
        assertEquals(9, stored[0].overallIntensity)
        assertEquals(Feeling.EXCELLENT, stored[0].overallFeeling)
        assertEquals(4, stored[0].activities[0].rounds.size)
        assertEquals(15.minutes, stored[0].totalDuration)
    }

    @Test
    fun save_withoutActivities_surfacesValidationError() = runTest {
        session.start(7)
        val vm = newViewModel()
        vm.onSave()
        assertFalse(vm.state.value.isSaved)
        assertTrue(vm.state.value.errorCodes.contains(com.warrior.domain.training.validation.TrainingErrorCode.NO_ACTIVITIES))
        assertTrue(repository.observeSessions(7).first().isEmpty())
    }

    @Test
    fun editMode_loadsExistingAndUpdatesInPlace() = runTest {
        session.start(7)
        val created = CreateTrainingSession(repository)(
            7,
            TrainingSession(
                userId = 7,
                date = 5,
                overallIntensity = 6,
                overallFeeling = Feeling.OKAY,
                activities = listOf(
                    WorkoutActivity(
                        type = WorkoutType.CARDIO,
                        duration = 20.minutes,
                        intensity = 5,
                        focusArea = FocusArea.CONDITIONING,
                    ),
                ),
            ),
        ).getOrThrow()

        val vm = newViewModel(created)
        assertEquals(created, vm.state.value.sessionId)
        assertEquals(6, vm.state.value.overallIntensity)
        assertEquals(1, vm.state.value.activities.size)

        vm.onIntensityChange(10)
        vm.onSave()
        assertTrue(vm.state.value.isSaved)

        val stored = repository.observeSessions(7).first()
        assertEquals(1, stored.size)
        assertEquals(created, stored[0].id)
        assertEquals(10, stored[0].overallIntensity)
    }

    @Test
    fun delete_removesSessionAfterConfirm() = runTest {
        session.start(7)
        val vm = newViewModel()
        vm.onAddActivity()
        vm.onSave()
        val id = repository.observeSessions(7).first()[0].id

        val editVm = newViewModel(id)
        editVm.onDeleteConfirmRequest(true)
        assertTrue(editVm.state.value.showDeleteConfirm)
        editVm.onDeleteSession()
        assertTrue(editVm.state.value.isDeleted)
        assertTrue(repository.observeSessions(7).first().isEmpty())
    }

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

/** In-memory repository fake (mirrors the domain-module test fake). */
class FakeTrainingRepository : TrainingRepository {

    private val sessions = mutableMapOf<Long, TrainingSession>()
    private val state = MutableStateFlow<List<TrainingSession>>(emptyList())
    private var nextId = 1L

    override fun observeSessions(userId: Long): Flow<List<TrainingSession>> =
        state.map { all -> all.filter { it.userId == userId }.sortedByDescending { it.date } }

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
