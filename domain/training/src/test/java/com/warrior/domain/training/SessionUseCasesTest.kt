package com.warrior.domain.training

import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.Round
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutActivity
import com.warrior.domain.training.model.WorkoutType
import com.warrior.domain.training.usecase.AddRound
import com.warrior.domain.training.usecase.AddWorkoutActivity
import com.warrior.domain.training.usecase.CreateTrainingSession
import com.warrior.domain.training.usecase.DeleteTrainingSession
import com.warrior.domain.training.usecase.GetTrainingHistory
import com.warrior.domain.training.usecase.GetTrainingSession
import com.warrior.domain.training.usecase.UpdateTrainingSession
import com.warrior.domain.training.validation.ValidationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class SessionUseCasesTest {

    private lateinit var repository: FakeTrainingRepository
    private lateinit var create: CreateTrainingSession
    private lateinit var update: UpdateTrainingSession
    private lateinit var delete: DeleteTrainingSession
    private lateinit var get: GetTrainingSession
    private lateinit var history: GetTrainingHistory
    private lateinit var addActivity: AddWorkoutActivity
    private lateinit var addRound: AddRound

    @Before
    fun setUp() {
        repository = FakeTrainingRepository()
        create = CreateTrainingSession(repository)
        update = UpdateTrainingSession(repository)
        delete = DeleteTrainingSession(repository)
        get = GetTrainingSession(repository)
        history = GetTrainingHistory(repository)
        addActivity = AddWorkoutActivity(repository)
        addRound = AddRound(repository)
    }

    private fun session(intensity: Int = 7) = TrainingSession(
        userId = 1,
        date = 100,
        overallIntensity = intensity,
        overallFeeling = Feeling.GOOD,
        activities = listOf(
            WorkoutActivity(
                type = WorkoutType.HEAVY_BAG,
                duration = 10.minutes,
                intensity = 8,
                focusArea = FocusArea.POWER,
            ),
        ),
    )

    @Test
    fun create_validSession_returnsIdAndStores() = runTest {
        val result = create(userId = 1, session = session())
        val id = result.getOrThrow()
        val stored = get(1, id)
        assertEquals(id, stored?.id)
        assertEquals(1L, stored?.userId)
        assertEquals(10.minutes, stored?.totalDuration)
    }

    @Test
    fun create_invalidSession_failsWithValidationException() = runTest {
        val result = create(userId = 1, session = session(intensity = 11))
        assertTrue(result.exceptionOrNull() is ValidationException)
        assertNull(get(1, 1))
    }

    @Test
    fun create_scopesSessionToRequestingUser() = runTest {
        val id = create(userId = 42, session = session().copy(userId = 999)).getOrThrow()
        assertEquals(42L, get(42, id)?.userId)
        assertNull(get(999, id))
    }

    @Test
    fun update_missingSession_fails() = runTest {
        val result = update(userId = 1, session = session().copy(id = 123))
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun update_crossUser_fails() = runTest {
        val id = create(1, session()).getOrThrow()
        val result = update(userId = 2, session = session().copy(id = id))
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun delete_wrongUser_returnsFalse() = runTest {
        val id = create(1, session()).getOrThrow()
        assertEquals(false, delete(2, id).getOrThrow())
        assertEquals(true, delete(1, id).getOrThrow())
        assertNull(get(1, id))
    }

    @Test
    fun history_isScopedAndSortedByDateDesc() = runTest {
        create(1, session().copy(date = 100)).getOrThrow()
        create(1, session().copy(date = 300)).getOrThrow()
        create(2, session().copy(date = 200)).getOrThrow()

        val list = history(1).first()
        assertEquals(listOf(300L, 100L), list.map { it.date })
        assertEquals(1, history(2).first().size)
    }

    @Test
    fun addActivity_appendsAndValidates() = runTest {
        val id = create(1, session()).getOrThrow()
        addActivity(
            1,
            id,
            WorkoutActivity(type = WorkoutType.CARDIO, duration = 5.minutes, intensity = 6, focusArea = FocusArea.CONDITIONING),
        ).getOrThrow()

        assertEquals(2, get(1, id)?.activities?.size)

        val invalid = addActivity(
            1,
            id,
            WorkoutActivity(type = WorkoutType.CARDIO, duration = 0.minutes, intensity = 6, focusArea = FocusArea.CONDITIONING),
        )
        assertTrue(invalid.exceptionOrNull() is ValidationException)
        assertEquals(2, get(1, id)?.activities?.size)
    }

    @Test
    fun addRound_assignsNextRoundNumber() = runTest {
        val id = create(1, session()).getOrThrow()
        val activityId = get(1, id)!!.activities[0].id

        addRound(1, id, activityId, Round(roundNumber = 0, duration = 3.minutes, restDuration = 1.minutes, intensity = 8)).getOrThrow()
        addRound(1, id, activityId, Round(roundNumber = 0, duration = 3.minutes, restDuration = 1.minutes, intensity = 8)).getOrThrow()

        val rounds = get(1, id)!!.activities[0].rounds
        assertEquals(listOf(1, 2), rounds.map { it.roundNumber })
    }

    @Test
    fun addRound_missingActivity_fails() = runTest {
        val id = create(1, session()).getOrThrow()
        val result = addRound(1, id, 999, Round(roundNumber = 1, duration = 3.minutes, restDuration = 1.minutes, intensity = 8))
        assertTrue(result.isFailure)
    }
}
