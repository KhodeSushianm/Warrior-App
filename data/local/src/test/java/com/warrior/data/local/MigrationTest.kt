package com.warrior.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.warrior.data.local.database.WarriorDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 10 — MigrationTestHelper infrastructure.
 *
 * Schema v1 is FROZEN and there are no migrations yet; this suite proves the
 * harness works so every future migration (v1→v2, …) is testable per the
 * Architecture Rule "schema JSON files live in VCS":
 *  1. the frozen v1 schema JSON (test assets) creates the exact legacy shape;
 *  2. the production [WarriorDatabase] opens that database (running any
 *     migrations registered in future phases) without data loss.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    private lateinit var helper: MigrationTestHelper

    @Before
    fun setUp() {
        helper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            WarriorDatabase::class.java,
        )
    }

    @Test
    fun schemaV1_hasAllFrozenTables_andReopensWithProductionDatabase() = runTest {
        val dbName = "migration-test-db"

        // 1) Create the database exactly as the frozen v1 schema defines it.
        helper.createDatabase(dbName, 1).apply {
            val cursor = query("SELECT name FROM sqlite_master WHERE type = 'table'")
            val tables = mutableSetOf<String>()
            while (cursor.moveToNext()) tables += cursor.getString(0)
            cursor.close()
            assertTrue(
                "v1 schema must contain all four frozen tables, was $tables",
                tables.containsAll(setOf("users", "training_sessions", "workout_activities", "rounds")),
            )

            // Seed one row through the raw v1 contract.
            execSQL(
                """
                INSERT INTO users (id, username, displayName, passwordHash, passwordSalt, createdAt, updatedAt)
                VALUES (1, 'warrior', 'The Warrior', 'hash', 'salt', 1, 1)
                """.trimIndent(),
            )
            close()
        }

        // 2) Reopen with the production database — future migrations run here.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.databaseBuilder(context, WarriorDatabase::class.java, dbName).build()
        val user = db.userDao().getById(1)
        assertEquals("warrior", user?.username)
        assertEquals("The Warrior", user?.displayName)
        db.close()
    }
}
