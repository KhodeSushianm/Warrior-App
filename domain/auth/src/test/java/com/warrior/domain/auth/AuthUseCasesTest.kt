package com.warrior.domain.auth

import com.warrior.domain.auth.usecase.CreateLocalAccount
import com.warrior.domain.auth.usecase.GetAccount
import com.warrior.domain.auth.usecase.Login
import com.warrior.domain.auth.usecase.Logout
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.auth.validation.AuthValidationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthUseCasesTest {

    private lateinit var repository: FakeAuthRepository
    private lateinit var session: FakeLocalSession
    private lateinit var register: CreateLocalAccount
    private lateinit var login: Login
    private lateinit var logout: Logout

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        session = FakeLocalSession()
        register = CreateLocalAccount(repository, session)
        login = Login(repository, session)
        logout = Logout(session)
    }

    @Test
    fun register_normalizesUsernameAndStartsSession() = runTest {
        val id = register("  WarRior_1 ", "The Warrior", "password123").getOrThrow()
        assertEquals(id, session.currentUserId.first())
        assertEquals("warrior_1", GetAccount(repository)(id)?.username)
        assertEquals(1, session.startCount)
    }

    @Test
    fun register_invalidInput_failsWithoutSideEffects() = runTest {
        val result = register("ok_1", "Name", "short")
        assertTrue(result.exceptionOrNull() is AuthValidationException)
        assertNull(session.currentUserId.first())
        assertEquals(0, session.startCount)
    }

    @Test
    fun register_duplicateUsername_fails() = runTest {
        register("warrior", "One", "password123").getOrThrow()
        val second = register("warrior", "Two", "password123")
        assertTrue(second.exceptionOrNull() is DuplicateUsernameException)
    }

    @Test
    fun login_wrongPassword_failsAndNoSession() = runTest {
        register("warrior", "One", "password123").getOrThrow()
        session.clear() // drop the registration session; login must start its own
        val result = login("warrior", "wrong-password")
        assertTrue(result.exceptionOrNull() is InvalidCredentialsException)
        assertNull(session.currentUserId.first())
    }

    @Test
    fun login_success_startsSession() = runTest {
        val id = register("warrior", "One", "password123").getOrThrow()
        session.clear()
        assertEquals(id, login("WARRIOR", "password123").getOrThrow())
        assertEquals(id, session.currentUserId.first())
    }

    @Test
    fun logout_clearsSessionOnly() = runTest {
        val id = register("warrior", "One", "password123").getOrThrow()
        logout().getOrThrow()
        assertNull(session.currentUserId.first())
        assertEquals(1, session.clearCount)
        // account still exists
        assertEquals("warrior", GetAccount(repository)(id)?.username)
    }

    @Test
    fun observeSession_emitsNullInitially() = runTest {
        assertNull(ObserveSession(session)().first())
    }
}
