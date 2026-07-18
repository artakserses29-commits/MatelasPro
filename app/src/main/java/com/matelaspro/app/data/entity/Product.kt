package com.matelaspro.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "",
    val epaisseur: Double = 0.0,
    val prixUnitaireCm: Double = 0.0,
    val longueur: Double = 0.0,
    val description: String = "",
    val quantity: Int = 0,
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val fournisseur: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun calculerPrixTotal(): Double = epaisseur * quantity * prixUnitaireCm
}
