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
}
