package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class PaymentFS(
    val id: String = "",
    val amount: Double = 0.0,
    val type: String = "VERSEMENT",
    val description: String = "",
    val date: Timestamp? = null
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "amount" to amount, "type" to type,
        "description" to description, "date" to (date ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): PaymentFS = PaymentFS(
            id = id,
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            type = map["type"] as? String ?: "VERSEMENT",
            description = map["description"] as? String ?: "",
            date = map["date"] as? Timestamp
        )
    }
}
