package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class SaleFS(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val profit: Double = 0.0,
    val saleDate: Timestamp? = null,
    val userId: String = ""
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "productId" to productId, "productName" to productName,
        "quantity" to quantity, "unitPrice" to unitPrice,
        "purchasePrice" to purchasePrice, "totalAmount" to totalAmount,
        "profit" to profit, "saleDate" to (saleDate ?: Timestamp.now()),
        "userId" to userId
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): SaleFS = SaleFS(
            id = id,
            productId = map["productId"] as? String ?: "",
            productName = map["productName"] as? String ?: "",
            quantity = (map["quantity"] as? Number)?.toInt() ?: 0,
            unitPrice = (map["unitPrice"] as? Number)?.toDouble() ?: 0.0,
            purchasePrice = (map["purchasePrice"] as? Number)?.toDouble() ?: 0.0,
            totalAmount = (map["totalAmount"] as? Number)?.toDouble() ?: 0.0,
            profit = (map["profit"] as? Number)?.toDouble() ?: 0.0,
            saleDate = map["saleDate"] as? Timestamp,
            userId = map["userId"] as? String ?: ""
        )
    }
}
