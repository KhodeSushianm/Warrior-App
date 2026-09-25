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

    @Query(
        """
        SELECT * FROM users
        """,
    )
    fun observeAll(): Flow<List<UserEntity>>

    @Delete
    suspend fun delete(user: UserEntity)
}
