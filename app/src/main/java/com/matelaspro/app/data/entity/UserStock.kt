package com.matelaspro.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_stock",
    indices = [Index("userId"), Index("productId"), Index("userId", "productId", unique = true)]
)
data class UserStock(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val productId: Long,
    val quantity: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
