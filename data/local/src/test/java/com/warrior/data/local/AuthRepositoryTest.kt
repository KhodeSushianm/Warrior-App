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

    @Test
    fun updateAccount_persistsIdentityAndKeepsPassword() = runTest {
        val id = repository.register("warrior", "The Warrior", "password123")

        repository.updateAccount(id, "Renamed", "new_name")

        val account = repository.getAccount(id)!!
        assertEquals("Renamed", account.displayName)
        assertEquals("new_name", account.username)
        // password untouched: old credentials still verify, with the new username
        assertEquals(id, repository.login("new_name", "password123"))
    }

    @Test
    fun updateAccount_usernameClash_fails() = runTest {
        val first = repository.register("alpha", "A", "password123")
        repository.register("beta", "B", "password123")

        val result = runCatching { repository.updateAccount(first, "A", "BETA") }
        assertTrue(result.exceptionOrNull() is DuplicateUsernameException)
        assertEquals("alpha", repository.getAccount(first)?.username)
    }

    @Test
    fun updateAccount_sameUserKeepsOwnUsername() = runTest {
        val id = repository.register("warrior", "Old", "password123")
        repository.updateAccount(id, "New", "WARRIOR") // case-normalized to itself
        assertEquals("New", repository.getAccount(id)?.displayName)
        assertEquals("warrior", repository.getAccount(id)?.username)
    }

    @Test
    fun updateAccount_unknownUser_fails() = runTest {
        val result = runCatching { repository.updateAccount(999, "Name", "ok_1") }
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }
}
