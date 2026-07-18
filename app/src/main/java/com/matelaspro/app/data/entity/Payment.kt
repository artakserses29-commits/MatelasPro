package com.matelaspro.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: String = "VERSEMENT",
    val description: String = "",
    val date: Long = System.currentTimeMillis()
)
