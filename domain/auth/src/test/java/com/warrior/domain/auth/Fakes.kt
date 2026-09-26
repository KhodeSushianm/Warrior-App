package com.warrior.domain.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLocalSession : LocalSession {
    private val state = MutableStateFlow<Long?>(null)
    override val currentUserId: Flow<Long?> = state
    var startCount = 0
        private set
    var clearCount = 0
        private set

    override suspend fun start(userId: Long) {
        startCount++
        state.value = userId
    }

    override suspend fun clear() {
        clearCount++
        state.value = null
    }
}

/** In-memory auth repository fake: stores plain passwords ONLY for test assertions. */
class FakeAuthRepository : AuthRepository {
    data class Row(val id: Long, val username: String, val displayName: String, val password: String)

    private val rows = mutableMapOf<Long, Row>()
    private var nextId = 1L

    override suspend fun register(username: String, displayName: String, password: String): Long {
        if (rows.values.any { it.username == username }) throw DuplicateUsernameException(username)
        val id = nextId++
        rows[id] = Row(id, username, displayName, password)
        return id
    }

    override suspend fun login(username: String, password: String): Long {
        val row = rows.values.firstOrNull { it.username == username && it.password == password }
            ?: throw InvalidCredentialsException()
        return row.id
    }

    override suspend fun getAccount(userId: Long): LocalAccount? =
        rows[userId]?.let { LocalAccount(it.id, it.username, it.displayName) }
}
