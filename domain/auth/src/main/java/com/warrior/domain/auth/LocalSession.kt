package com.warrior.domain.auth

import kotlinx.coroutines.flow.Flow

/**
 * Local device session (Architecture v2.1 §14.2): keeps currentUserId +
 * sessionCreatedAt in DataStore. Logout clears these values only — user data
 * is never touched.
 */
interface LocalSession {

    val currentUserId: Flow<Long?>

    suspend fun start(userId: Long)

    suspend fun clear()
}
