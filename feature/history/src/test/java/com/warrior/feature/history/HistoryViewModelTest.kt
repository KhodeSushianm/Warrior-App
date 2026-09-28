package com.warrior.feature.history

import androidx.lifecycle.SavedStateHandle
import com.warrior.domain.auth.AppPreferences
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
import kotlin.time.Duration.Companion.minutes

class HistoryViewModelTest {

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

    private fun sessionAt(date: Long, createdAt: Long = date) = TrainingSession(
        userId = 1,
        date = date,
        createdAt = createdAt,
        overallIntensity = 7,
        overallFeeling = Feeling.GOOD,
        activities = listOf(
            WorkoutActivity(
                type = WorkoutType.HEAVY_BAG,
                duration = 30.minutes,
                intensity = 8,
                focusArea = FocusArea.POWER,
            ),
        ),
    )

    @Test
    fun groupsByDayDescendingWithInDayOrder() = runTest {
        session.start(1)
        val create = CreateTrainingSession(repository)
        create(1, sessionAt(date = 100, createdAt = 1))
        create(1, sessionAt(date = 300, createdAt = 3))
        create(1, sessionAt(date = 300, createdAt = 4))

        val vm = HistoryViewModel(GetTrainingHistory(repository), ObserveSession(session), DeleteTrainingSession(repository), FakeAppPreferences())
        val groups = vm.state.value.groups
        assertEquals(listOf(300L, 100L), groups.map { it.key })
        assertEquals(2, groups[0].sessions.size)
        assertEquals(listOf(4L, 3L), groups[0].sessions.map { it.createdAt })
    }

    @Test
    fun deleteFlow_removesSessionAfterConfirm() = runTest {
        session.start(1)
        val id = CreateTrainingSession(repository)(1, sessionAt(100)).getOrThrow()

        val vm = HistoryViewModel(GetTrainingHistory(repository), ObserveSession(session), DeleteTrainingSession(repository), FakeAppPreferences())
        assertEquals(1, vm.state.value.groups.size)

        vm.onDeleteRequest(id)
        assertEquals(id, vm.state.value.pendingDelete)
        vm.onConfirmDelete()
        assertTrue(vm.state.value.groups.isEmpty())
        assertNull(repository.getSession(1, id))
    }

    @Test
    fun detailViewModel_followsLiveUpdatesAndDeletes() = runTest {
        session.start(1)
        val id = CreateTrainingSession(repository)(1, sessionAt(100)).getOrThrow()

        val detail = HistoryDetailViewModel(
            GetTrainingHistory(repository),
            ObserveSession(session),
            DeleteTrainingSession(repository),
            FakeAppPreferences(),
            SavedStateHandle(mapOf("sessionId" to id)),
        )
        assertEquals(id, detail.state.value.session?.id)

        // live update: repository change flows into detail state
        val stored = repository.getSession(1, id)!!
        repository.updateSession(stored.copy(overallIntensity = 10))
        assertEquals(10, detail.state.value.session?.overallIntensity)

        detail.onConfirmDelete()
        assertTrue(detail.state.value.isDeleted)
        assertNull(repository.getSession(1, id))
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
internal class FakeTrainingRepository : TrainingRepository {

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

private class FakeAppPreferences : AppPreferences {
    private val lang = MutableStateFlow(AppPreferences.LANG_SYSTEM)
    private val cal = MutableStateFlow(AppPreferences.CALENDAR_GREGORIAN)
    override val language: Flow<String> = lang
    override val calendar: Flow<String> = cal
    override suspend fun setLanguage(tag: String) {
        lang.value = tag
    }
    override suspend fun setCalendar(id: String) {
        cal.value = id
    }
}
