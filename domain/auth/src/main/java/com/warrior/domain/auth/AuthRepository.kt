package com.warrior.domain.auth

/**
 * Boundary for local identity (Architecture v2.1 §14).
 * Implementations handle hashing (core:security) and storage (Room);
 * plain-text passwords must never cross this boundary into storage.
 */
interface AuthRepository {

    /** Creates the account with a hashed password; returns the new user id. */
    suspend fun register(username: String, displayName: String, password: String): Long

    /** Verifies credentials; returns the user id. */
    suspend fun login(username: String, password: String): Long

    suspend fun getAccount(userId: Long): LocalAccount?

    /**
     * Updates the identity fields of an existing account (Phase 9).
     * Throws [com.warrior.domain.auth.DuplicateUsernameException] when the new
     * username belongs to a different local account, [NoSuchElementException]
     * when [userId] does not exist. Password is never touched here.
     */
    suspend fun updateAccount(userId: Long, displayName: String, username: String)
}
