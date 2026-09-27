package com.warrior.domain.auth.usecase

import com.warrior.domain.auth.AuthRepository
import com.warrior.domain.auth.LocalAccount
import com.warrior.domain.auth.LocalSession
import com.warrior.domain.auth.validation.AuthValidation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Registers a local account and starts the device session on success. */
class CreateLocalAccount @Inject constructor(
    private val repository: AuthRepository,
    private val session: LocalSession,
) {
    suspend operator fun invoke(username: String, displayName: String, password: String): Result<Long> =
        runCatching {
            val normalized = username.trim().lowercase()
            AuthValidation.requireRegister(normalized, displayName.trim(), password)
            val id = repository.register(normalized, displayName.trim(), password)
            session.start(id)
            id
        }
}

/** Verifies credentials and starts the device session on success. */
class Login @Inject constructor(
    private val repository: AuthRepository,
    private val session: LocalSession,
) {
    suspend operator fun invoke(username: String, password: String): Result<Long> =
        runCatching {
            val normalized = username.trim().lowercase()
            AuthValidation.requireLogin(normalized, password)
            val id = repository.login(normalized, password)
            session.start(id)
            id
        }
}

/** Clears the device session only — account data stays on device. */
class Logout @Inject constructor(private val session: LocalSession) {
    suspend operator fun invoke(): Result<Unit> =
        runCatching { session.clear() }
}

/** Root navigation observes this to choose Auth vs Main entry point. */
class ObserveSession @Inject constructor(private val session: LocalSession) {
    operator fun invoke(): Flow<Long?> = session.currentUserId
}

/** Loads the display identity for the Profile screen. */
class GetAccount @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(userId: Long): LocalAccount? = repository.getAccount(userId)
}

/**
 * Edits the profile identity (Phase 9): validates like registration's
 * identity rules, normalizes the username, and enforces on-device uniqueness
 * through the repository. Password is never part of this flow.
 */
class UpdateAccount @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(userId: Long, displayName: String, username: String): Result<Unit> =
        runCatching {
            val normalized = username.trim().lowercase()
            AuthValidation.requireProfile(normalized, displayName.trim())
            repository.updateAccount(userId, displayName.trim(), normalized)
        }
}
