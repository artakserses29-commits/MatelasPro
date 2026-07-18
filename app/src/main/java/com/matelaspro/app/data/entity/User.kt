package com.matelaspro.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val password: String,
    val isAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
