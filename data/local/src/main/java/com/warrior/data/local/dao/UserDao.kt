package com.warrior.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.warrior.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/** Database Design v4 §13. */
@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: UserEntity): Long

    @Query(
        """
        SELECT * FROM users
        WHERE id = :id
        """,
    )
    suspend fun getById(id: Long): UserEntity?

    @Query(
        """
        SELECT * FROM users
        WHERE username = :username
        """,
    )
    suspend fun getByUsername(username: String): UserEntity?

    /** Identity-only update (Phase 9): password hash/salt are never touched here. */
    @Query(
        """
        UPDATE users
        SET displayName = :displayName, username = :username, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateProfile(id: Long, displayName: String, username: String, updatedAt: Long): Int

    @Query(
        """
        SELECT * FROM users
        """,
    )
    fun observeAll(): Flow<List<UserEntity>>

    @Delete
    suspend fun delete(user: UserEntity)

    /** Full-table read for backup export (Season 2 / Phase 12). */
    @Query("SELECT * FROM users")
    suspend fun getAllForExport(): List<UserEntity>

    @Query("DELETE FROM users")
    suspend fun deleteAll(): Int
}
