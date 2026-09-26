package com.warrior.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.warrior.data.local.database.WarriorDatabase
import com.warrior.data.local.repository.RoomAuthRepository
import com.warrior.domain.auth.DuplicateUsernameException
import com.warrior.domain.auth.InvalidCredentialsException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Phase 4: repository-level auth guarantees on real Room + PBKDF2. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthRepositoryTest {

    private lateinit var db: WarriorDatabase
    private lateinit var repository: RoomAuthRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WarriorDatabase::class.java).build()
        repository = RoomAuthRepository(db.userDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun registerThenLogin_roundTrips() = runTest {
        val id = repository.register("warrior", "The Warrior", "password123")
        assertEquals(id, repository.login("warrior", "password123"))
        assertEquals("The Warrior", repository.getAccount(id)?.displayName)
    }

    @Test
    fun register_storesHashNotPlainText() = runTest {
        repository.register("warrior", "Name", "password123")
        val user = db.userDao().getByUsername("warrior")!!
        assertNotEquals("password123", user.passwordHash)
        assertEquals(64, user.passwordHash.length) // 32 bytes hex
        assertEquals(32, user.passwordSalt.length) // 16 bytes hex
        assertTrue(user.passwordSalt.isNotBlank())
    }

    @Test
    fun register_duplicateUsernameCaseInsensitive_fails() = runTest {
        repository.register("Warrior", "One", "password123")
        val error = runCatching { repository.register("warrior", "Two", "password123") }.exceptionOrNull()
        assertTrue(error is DuplicateUsernameException)
    }

    @Test
    fun login_wrongPassword_orUnknownUser_fails() = runTest {
        repository.register("warrior", "One", "password123")
        assertTrue(
            runCatching { repository.login("warrior", "password124") }.exceptionOrNull() is InvalidCredentialsException,
        )
        assertTrue(
            runCatching { repository.login("ghost", "password123") }.exceptionOrNull() is InvalidCredentialsException,
        )
    }
}
