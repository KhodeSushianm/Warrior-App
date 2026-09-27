package com.warrior.feature.profile

import com.warrior.domain.auth.AuthRepository
import com.warrior.domain.auth.DuplicateUsernameException
import com.warrior.domain.auth.LocalAccount
import com.warrior.domain.auth.LocalSession
import com.warrior.domain.auth.usecase.GetAccount
import com.warrior.domain.auth.usecase.Logout
import com.warrior.domain.auth.usecase.ObserveSession
import com.warrior.domain.auth.usecase.UpdateAccount
import com.warrior.domain.auth.validation.AuthErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Profile VM (Phase 9): identity loading, edit flow with codes, logout. */
class ProfileViewModelTest {

    private lateinit var repository: FakeAuthRepository
    private lateinit var session: FakeLocalSession

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeAuthRepository()
        session = FakeLocalSession()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = ProfileViewModel(
        observeSession = ObserveSession(session),
        getAccount = GetAccount(repository),
        updateAccount = UpdateAccount(repository),
        logout = Logout(session),
    )

    @Test
    fun loadsAccount_forSignedInUser() = runTest {
        val id = repository.register("warrior", "The Warrior", "password123")
        session.start(id)

        val state = buildViewModel().state.value
        assertEquals("The Warrior", state.account?.displayName)
        assertEquals("warrior", state.account?.username)
    }

    @Test
    fun editDialog_prefillsSavesAndRefreshesIdentity() = runTest {
        val id = repository.register("warrior", "The Warrior", "password123")
        session.start(id)
        val viewModel = buildViewModel()

        viewModel.onEditOpen()
        assertEquals("The Warrior", viewModel.state.value.editDisplayName)
        assertEquals("warrior", viewModel.state.value.editUsername)

        viewModel.onEditDisplayNameChange("Renamed")
        viewModel.onEditUsernameChange("New_Name")
        viewModel.onSaveProfile()

        val state = viewModel.state.value
        assertFalse(state.showEditDialog)
        assertEquals("Renamed", state.account?.displayName)
        assertEquals("new_name", state.account?.username) // normalized
        assertTrue(state.editErrorCodes.isEmpty())
    }

    @Test
    fun save_duplicateUsername_surfacesCodeAndKeepsDialogOpen() = runTest {
        val first = repository.register("alpha", "A", "password123")
        repository.register("beta", "B", "password123")
        session.start(first)
        val viewModel = buildViewModel()

        viewModel.onEditOpen()
        viewModel.onEditUsernameChange("beta")
        viewModel.onSaveProfile()

        val state = viewModel.state.value
        assertTrue(state.showEditDialog)
        assertEquals(listOf(AuthErrorCode.DUPLICATE_USERNAME), state.editErrorCodes)
        assertEquals("alpha", state.account?.username) // unchanged
    }

    @Test
    fun save_validationErrors_surfaceCodes() = runTest {
        val id = repository.register("warrior", "Name", "password123")
        session.start(id)
        val viewModel = buildViewModel()

        viewModel.onEditOpen()
        viewModel.onEditDisplayNameChange("   ")
        viewModel.onEditUsernameChange("x")
        viewModel.onSaveProfile()

        val codes = viewModel.state.value.editErrorCodes
        assertTrue(codes.contains(AuthErrorCode.DISPLAY_NAME_INVALID))
        assertTrue(codes.contains(AuthErrorCode.USERNAME_FORMAT))
        assertTrue(viewModel.state.value.showEditDialog)
    }

    @Test
    fun logout_clearsSessionAndAccount() = runTest {
        val id = repository.register("warrior", "Name", "password123")
        session.start(id)
        val viewModel = buildViewModel()
        assertEquals(id, viewModel.state.value.account?.id)

        viewModel.onLogout()

        assertNull(session.currentUserId.first())
        assertNull(viewModel.state.value.account)
    }

    @Test
    fun aboutDialog_toggles() = runTest {
        val id = repository.register("warrior", "Name", "password123")
        session.start(id)
        val viewModel = buildViewModel()

        assertFalse(viewModel.state.value.showAbout)
        viewModel.onAboutOpen()
        assertTrue(viewModel.state.value.showAbout)
        viewModel.onAboutClose()
        assertFalse(viewModel.state.value.showAbout)
    }
}

/** In-memory auth repository fake (mirrors the domain-module fake). */
internal class FakeAuthRepository : AuthRepository {
    data class Row(val id: Long, val username: String, val displayName: String, val password: String)

    private val rows = mutableMapOf<Long, Row>()
    private var nextId = 1L

    override suspend fun register(username: String, displayName: String, password: String): Long {
        val normalized = username.trim().lowercase()
        if (rows.values.any { it.username == normalized }) throw DuplicateUsernameException(normalized)
        val id = nextId++
        rows[id] = Row(id, normalized, displayName, password)
        return id
    }

    override suspend fun login(username: String, password: String): Long {
        val normalized = username.trim().lowercase()
        val row = rows.values.firstOrNull { it.username == normalized && it.password == password }
            ?: throw com.warrior.domain.auth.InvalidCredentialsException()
        return row.id
    }

    override suspend fun getAccount(userId: Long): LocalAccount? =
        rows[userId]?.let { LocalAccount(it.id, it.username, it.displayName) }

    override suspend fun updateAccount(userId: Long, displayName: String, username: String) {
        val current = rows[userId] ?: throw NoSuchElementException("user $userId not found")
        val clash = rows.values.firstOrNull { it.username == username && it.id != userId }
        if (clash != null) throw DuplicateUsernameException(username)
        rows[userId] = current.copy(displayName = displayName, username = username)
    }
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
