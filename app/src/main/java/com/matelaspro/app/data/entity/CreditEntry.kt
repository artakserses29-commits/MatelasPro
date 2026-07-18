package com.matelaspro.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_entries")
data class CreditEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fournisseur: String = "",
    val productName: String,
    val category: String = "",
    val epaisseur: Double = 0.0,
    val longueur: Double = 0.0,
    val description: String = "",
    val quantity: Int = 0,
    val sellingPrice: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val productId: Long? = null,
    val isOverridden: Boolean = false,
    val correctionCount: Int = 0
)
