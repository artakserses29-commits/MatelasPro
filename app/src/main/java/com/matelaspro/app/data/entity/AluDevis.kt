package com.matelaspro.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alu_devis")
data class AluDevis(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientName: String,
    val clientAddress: String,
    val clientPhone: String,
    val items: String,
    val totalAmount: Double,
    val createdAt: Long = System.currentTimeMillis()
)
