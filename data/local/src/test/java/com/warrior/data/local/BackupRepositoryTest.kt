package com.warrior.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.warrior.data.local.database.WarriorDatabase
import com.warrior.data.local.entity.BodyMetricEntity
import com.warrior.data.local.repository.JsonBackupRepository
import com.warrior.data.local.repository.RoomAuthRepository
import com.warrior.data.local.repository.RoomTrainingRepository
import com.warrior.domain.training.BackupFormatException
import com.warrior.domain.training.model.Feeling
import com.warrior.domain.training.model.FocusArea
import com.warrior.domain.training.model.Round
import com.warrior.domain.training.model.TrainingSession
import com.warrior.domain.training.model.WorkoutActivity
import com.warrior.domain.training.model.WorkoutType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.minutes

/** Season 2 / Phase 12: whole-device JSON backup export/import guarantees. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRepositoryTest {

    private lateinit var db: WarriorDatabase
    private lateinit var backup: JsonBackupRepository
    private lateinit var auth: RoomAuthRepository
    private lateinit var training: RoomTrainingRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WarriorDatabase::class.java).build()
        backup = JsonBackupRepository(db)
        auth = RoomAuthRepository(db.userDao())
        training = RoomTrainingRepository(db.trainingSessionDao(), db.workoutActivityDao(), db.roundDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seed(): Long {
        val userId = auth.register("warrior", "The Warrior", "password123")
        training.createSession(
            TrainingSession(
                userId = userId,
                date = 1_790_121_600_000L,
                overallIntensity = 8,
                overallFeeling = Feeling.GOOD,
                notes = "backup me",
                createdAt = 1,
                updatedAt = 1,
                activities = listOf(
                    WorkoutActivity(
                        type = WorkoutType.HEAVY_BAG,
                        duration = 30.minutes,
                        intensity = 8,
                        focusArea = FocusArea.POWER,
                        rounds = listOf(
                            Round(roundNumber = 1, duration = 3.minutes, restDuration = 1.minutes, intensity = 8),
                            Round(roundNumber = 2, duration = 3.minutes, restDuration = 1.minutes, intensity = 9),
                        ),
                    ),
                ),
            ),
        )
        db.bodyMetricDao().insert(
            BodyMetricEntity(userId = userId, date = 1_790_121_600_000L, weightKg = 78.5f, heightCm = 182f),
        )
        return userId
    }

    @Test
    fun exportProducesVersionedDocument() = runTest {
        seed()
        val json = backup.exportAll()
        assertTrue(json.contains("\"app\": \"WARRIOR\""))
        assertTrue(json.contains("\"formatVersion\": 1"))
        assertTrue(json.contains("backup me"))
    }

    @Test
    fun importReplacesEverythingAtomically_onAFreshDatabase() = runTest {
        seed()
        val json = backup.exportAll()

        // "New device": a brand-new database imports the document.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val freshDb = Room.inMemoryDatabaseBuilder(context, WarriorDatabase::class.java).build()
        try {
            val freshBackup = JsonBackupRepository(freshDb)
            val freshAuth = RoomAuthRepository(freshDb.userDao())
            val freshTraining = RoomTrainingRepository(
                freshDb.trainingSessionDao(),
                freshDb.workoutActivityDao(),
                freshDb.roundDao(),
            )

            val restoredSessions = freshBackup.importAll(json)
            assertEquals(1, restoredSessions)

            // Account + password hash survived: login works on the fresh DB.
            val userId = freshAuth.login("warrior", "password123")
            val sessions = freshTraining.getSession(userId, 1)!!
            assertEquals("backup me", sessions.notes)
            assertEquals(1, sessions.activities.size)
            assertEquals(2, sessions.activities.single().rounds.size)
            val metric = freshDb.bodyMetricDao().latest(userId)
            assertEquals(78.5f, metric?.weightKg)
        } finally {
            freshDb.close()
        }
    }

    @Test
    fun importOnSameDevice_wipesLaterChanges() = runTest {
        seed()
        val json = backup.exportAll()

        // Mutate after the backup...
        db.bodyMetricDao().insert(BodyMetricEntity(userId = 1, date = 1_790_208_000_000L, weightKg = 90f))
        assertEquals(2, db.bodyMetricDao().getAllForExport().size)

        // ...import restores the exact snapshot.
        backup.importAll(json)
        assertEquals(1, db.bodyMetricDao().getAllForExport().size)
        assertEquals(78.5f, db.bodyMetricDao().latest(1)?.weightKg)
    }

    @Test
    fun importRejectsForeignAndMalformedPayloads_withoutTouchingData() = runTest {
        seed()

        val malformed = runCatching { backup.importAll("this is not json") }.exceptionOrNull()
        assertTrue(malformed is BackupFormatException)

        val foreign = runCatching { backup.importAll("{\"app\":\"OTHER\",\"formatVersion\":1}") }.exceptionOrNull()
        assertTrue(foreign is BackupFormatException)

        val future = runCatching { backup.importAll("{\"app\":\"WARRIOR\",\"formatVersion\":99}") }.exceptionOrNull()
        assertTrue(future is BackupFormatException)

        // Data untouched.
        assertEquals(1, db.userDao().getAllForExport().size)
        assertEquals(1, db.trainingSessionDao().getAllForExport().size)
    }
}
