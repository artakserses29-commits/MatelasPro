package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class UserStockFS(
    val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val quantity: Int = 0,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "userId" to userId, "productId" to productId,
        "quantity" to quantity, "updatedAt" to (updatedAt ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): UserStockFS = UserStockFS(
            id = id,
            userId = map["userId"] as? String ?: "",
            productId = map["productId"] as? String ?: "",
            quantity = (map["quantity"] as? Number)?.toInt() ?: 0,
            updatedAt = map["updatedAt"] as? Timestamp
        )
    }
}
