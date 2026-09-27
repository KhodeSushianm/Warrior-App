package com.warrior.data.local.repository

import com.warrior.core.security.PasswordHasher
import com.warrior.data.local.dao.UserDao
import com.warrior.data.local.entity.UserEntity
import com.warrior.domain.auth.AuthRepository
import com.warrior.domain.auth.DuplicateUsernameException
import com.warrior.domain.auth.InvalidCredentialsException
import com.warrior.domain.auth.LocalAccount
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room + PBKDF2 implementation of [AuthRepository] (Architecture v2.1 §14).
 * Plain-text passwords are hashed here and never reach storage or logs.
 */
@Singleton
class RoomAuthRepository @Inject constructor(
    private val userDao: UserDao,
) : AuthRepository {

    override suspend fun register(username: String, displayName: String, password: String): Long {
        val normalized = username.trim().lowercase()
        if (userDao.getByUsername(normalized) != null) {
            throw DuplicateUsernameException(normalized)
        }
        val now = System.currentTimeMillis()
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hashToHex(password, salt)
        return userDao.insert(
            UserEntity(
                username = normalized,
                displayName = displayName,
                passwordHash = hash,
                passwordSalt = PasswordHasher.saltToHex(salt),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun login(username: String, password: String): Long {
        val normalized = username.trim().lowercase()
        val user = userDao.getByUsername(normalized) ?: throw InvalidCredentialsException()
        val salt = PasswordHasher.saltFromHex(user.passwordSalt)
        if (!PasswordHasher.verify(password, salt, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        return user.id
    }

    override suspend fun getAccount(userId: Long): LocalAccount? =
        userDao.getById(userId)?.let { LocalAccount(id = it.id, username = it.username, displayName = it.displayName) }

    override suspend fun updateAccount(userId: Long, displayName: String, username: String) {
        val normalized = username.trim().lowercase()
        userDao.getByUsername(normalized)?.let { clash ->
            if (clash.id != userId) throw DuplicateUsernameException(normalized)
        }
        userDao.getById(userId) ?: throw NoSuchElementException("user $userId not found")
        userDao.updateProfile(
            id = userId,
            displayName = displayName,
            username = normalized,
            updatedAt = System.currentTimeMillis(),
        )
    }
}
