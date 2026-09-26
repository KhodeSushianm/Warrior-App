package com.warrior.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.warrior.data.local.dao.ActivityWithRounds
import com.warrior.data.local.database.WarriorDatabase
import com.warrior.data.local.entity.RoundEntity
import com.warrior.data.local.entity.TrainingSessionEntity
import com.warrior.data.local.entity.UserEntity
import com.warrior.data.local.entity.WorkoutActivityEntity
import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.WorkoutType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 2 acceptance tests (EXECUTION-PLAN §Phase 2 DoD):
 * atomic rollback, cascade deletes, ownership enforcement, unique round numbers,
 * and entity-level constraint guards.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WarriorDatabaseTest {

    private lateinit var db: WarriorDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WarriorDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private val now = 1_758_000_000_000L

    private suspend fun newUser(username: String): Long =
        db.userDao().insert(
            UserEntity(
                username = username,
                displayName = username.replaceFirstChar { it.uppercase() },
                passwordHash = "hash-$username",
                passwordSalt = "salt-$username",
                createdAt = now,
                updatedAt = now,
            ),
        )

    private fun session(userId: Long, intensity: Int = 7) = TrainingSessionEntity(
        userId = userId,
        date = now,
        startedAt = now,
        endedAt = now + 3_600_000,
        overallIntensity = intensity,
        overallFeeling = Feeling.GOOD,
        notes = null,
        createdAt = now,
        updatedAt = now,
    )

    private fun activity(type: WorkoutType = WorkoutType.HEAVY_BAG, duration: Long = 60_000) =
        WorkoutActivityEntity(
            sessionId = 0,
            type = type,
            duration = duration,
            intensity = 8,
            focusArea = FocusArea.POWER,
            notes = null,
            createdAt = now,
            updatedAt = now,
        )

    private fun round(number: Int, intensity: Int = 8) = RoundEntity(
        activityId = 0,
        roundNumber = number,
        duration = 180_000,
        restDuration = 60_000,
        intensity = intensity,
        notes = null,
        createdAt = now,
        updatedAt = now,
    )

    @Test
    fun insertFullSession_writesEverythingAndDerivesDuration() = runTest {
        val userId = newUser("warrior")
        val sessionId = db.trainingSessionDao().insertFullSession(
            session(userId),
            listOf(
                ActivityWithRounds(activity(), listOf(round(1), round(2), round(3))),
                ActivityWithRounds(activity(WorkoutType.CARDIO, 20_000)),
            ),
        )

        val stored = db.trainingSessionDao().getSession(userId, sessionId)
        assertNotNull(stored)
        assertEquals(Feeling.GOOD, stored!!.overallFeeling)

        val activities = db.workoutActivityDao().getActivities(userId, sessionId)
        assertEquals(2, activities.size)

        val rounds = db.roundDao().getRounds(userId, activities[0].id)
        assertEquals(3, rounds.size)
        assertEquals(listOf(1, 2, 3), rounds.map { it.roundNumber })

        // Derived duration = SUM(activity durations), ownership-enforced.
        assertEquals(
            80_000L,
            db.trainingSessionDao().getSessionTotalDuration(userId, sessionId),
        )
        assertEquals(
            80_000L,
            db.workoutActivityDao().sumDurationBySession(userId, sessionId),
        )
    }

    @Test
    fun insertFullSession_rollsBackEverythingOnFailure() = runTest {
        val userId = newUser("warrior")
        val broken = ActivityWithRounds(
            activity(),
            // duplicate roundNumber -> unique index violation
            listOf(round(1), round(1)),
        )

        val error = runCatching {
            db.trainingSessionDao().insertFullSession(session(userId), listOf(broken))
        }.exceptionOrNull()
        assertTrue(error is SQLiteConstraintException)

        val sessions = db.trainingSessionDao().observeSessionsOnce(userId)
        assertTrue(sessions.isEmpty())
    }

    @Test
    fun deleteSession_cascadesActivitiesAndRounds() = runTest {
        val userId = newUser("warrior")
        val sessionId = db.trainingSessionDao().insertFullSession(
            session(userId),
            listOf(ActivityWithRounds(activity(), listOf(round(1), round(2)))),
        )
        val activityId = db.workoutActivityDao().getActivities(userId, sessionId)[0].id

        db.trainingSessionDao().deleteSession(userId, sessionId)

        assertTrue(db.workoutActivityDao().getActivities(userId, sessionId).isEmpty())
        assertTrue(db.roundDao().getRounds(userId, activityId).isEmpty())
        assertNull(db.trainingSessionDao().getSession(userId, sessionId))
    }

    @Test
    fun deleteUser_cascadesSessions() = runTest {
        val userId = newUser("warrior")
        db.trainingSessionDao().insertFullSession(session(userId), emptyList())
        val user = db.userDao().getById(userId)!!

        db.userDao().delete(user)

        assertTrue(db.trainingSessionDao().observeSessionsOnce(userId).isEmpty())
    }

    @Test
    fun ownership_crossUserAccessIsImpossible() = runTest {
        val userA = newUser("alice")
        val userB = newUser("bob")
        val sessionId = db.trainingSessionDao().insertFullSession(
            session(userA),
            listOf(ActivityWithRounds(activity(), listOf(round(1)))),
        )
        val activityId = db.workoutActivityDao().getActivities(userA, sessionId)[0].id
        val roundId = db.roundDao().getRounds(userA, activityId)[0].id

        // Reads from the wrong owner return nothing.
        assertNull(db.trainingSessionDao().getSession(userB, sessionId))
        assertTrue(db.workoutActivityDao().getActivities(userB, sessionId).isEmpty())
        assertNull(db.workoutActivityDao().getById(userB, activityId))
        assertNull(db.roundDao().getById(userB, roundId))
        assertEquals(0L, db.trainingSessionDao().getSessionTotalDuration(userB, sessionId))

        // Deletes from the wrong owner are no-ops.
        db.workoutActivityDao().delete(userB, activityId)
        db.roundDao().delete(userB, roundId)
        db.trainingSessionDao().deleteSession(userB, sessionId)
        assertNotNull(db.trainingSessionDao().getSession(userA, sessionId))
        assertEquals(1, db.workoutActivityDao().getActivities(userA, sessionId).size)
        assertEquals(1, db.roundDao().getRounds(userA, activityId).size)
    }

    @Test
    fun roundNumber_isUniquePerActivity() = runTest {
        val userId = newUser("warrior")
        val sessionId = db.trainingSessionDao().insertFullSession(
            session(userId),
            listOf(ActivityWithRounds(activity())),
        )
        val activityId = db.workoutActivityDao().getActivities(userId, sessionId)[0].id
        db.roundDao().insert(round(1).copy(activityId = activityId))

        val error = runCatching {
            db.roundDao().insert(round(1).copy(activityId = activityId))
        }.exceptionOrNull()
        assertTrue(error is SQLiteConstraintException)
    }

    @Test
    fun entityGuards_rejectInvalidValues() {
        assertThrows(IllegalArgumentException::class.java) {
            session(1).copy(overallIntensity = 11)
        }
        assertThrows(IllegalArgumentException::class.java) {
            round(1).copy(restDuration = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            round(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            activity(duration = 0)
        }
    }

    private suspend fun com.warrior.data.local.dao.TrainingSessionDao.observeSessionsOnce(
        userId: Long,
    ): List<TrainingSessionEntity> = observeSessions(userId).first()
}
