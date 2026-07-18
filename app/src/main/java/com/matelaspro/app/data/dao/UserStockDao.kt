package com.matelaspro.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.matelaspro.app.data.entity.UserStock

@Dao
interface UserStockDao {
    @Query("SELECT * FROM user_stock WHERE userId = :userId")
    suspend fun getByUserId(userId: Long): List<UserStock>

    @Query("SELECT * FROM user_stock WHERE userId = :userId AND productId = :productId")
    suspend fun getByUserAndProduct(userId: Long, productId: Long): UserStock?

    @Query("SELECT COALESCE(quantity, 0) FROM user_stock WHERE userId = :userId AND productId = :productId")
    suspend fun getQuantity(userId: Long, productId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(userStock: UserStock): Long

    @Query("UPDATE user_stock SET quantity = quantity + :delta, updatedAt = :now WHERE userId = :userId AND productId = :productId")
    suspend fun addQuantity(userId: Long, productId: Long, delta: Int, now: Long)

    @Query("DELETE FROM user_stock WHERE userId = :userId AND productId = :productId")
    suspend fun deleteByUserAndProduct(userId: Long, productId: Long)

    @Query("DELETE FROM user_stock WHERE userId = :userId")
    suspend fun deleteByUser(userId: Long)

    @Query("SELECT * FROM user_stock")
    suspend fun getAll(): List<UserStock>
}
