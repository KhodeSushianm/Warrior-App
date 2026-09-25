package com.warrior.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local account. See Database Design v4 §1.
 * All timestamps are UTC epoch millis.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val passwordHash: String,
    val passwordSalt: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    init {
        require(username.isNotBlank()) { "username must not be blank" }
        require(displayName.isNotBlank()) { "displayName must not be blank" }
        require(passwordHash.isNotBlank()) { "passwordHash must not be blank" }
        require(passwordSalt.isNotBlank()) { "passwordSalt must not be blank" }
    }
}
