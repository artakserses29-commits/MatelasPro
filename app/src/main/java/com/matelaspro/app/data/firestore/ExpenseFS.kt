package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class ExpenseFS(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val createdAt: Timestamp? = null
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "userId" to userId, "amount" to amount,
        "description" to description, "createdAt" to (createdAt ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): ExpenseFS = ExpenseFS(
            id = id,
            userId = map["userId"] as? String ?: "",
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            description = map["description"] as? String ?: "",
            createdAt = map["createdAt"] as? Timestamp
        )
    }
}
