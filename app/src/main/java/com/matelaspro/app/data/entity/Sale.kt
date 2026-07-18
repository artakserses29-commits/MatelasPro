package com.matelaspro.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId")]
)
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String = "",
    val quantity: Int,
    val unitPrice: Double,
    val purchasePrice: Double = 0.0,
    val totalAmount: Double,
    val profit: Double = 0.0,
    val saleDate: Long = System.currentTimeMillis(),
    val userId: Long = 0
)
