package com.matelaspro.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alu_products")
data class AluProduct(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val surface: Double = 0.0,
    val prixUnitaire: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
