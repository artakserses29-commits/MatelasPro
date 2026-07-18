package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class AluDevisFS(
    val id: String = "",
    val clientName: String = "",
    val clientAddress: String = "",
    val clientPhone: String = "",
    val items: String = "",
    val totalAmount: Double = 0.0,
    val createdAt: Timestamp? = null
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "clientName" to clientName, "clientAddress" to clientAddress,
        "clientPhone" to clientPhone, "items" to items,
        "totalAmount" to totalAmount, "createdAt" to (createdAt ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AluDevisFS = AluDevisFS(
            id = id,
            clientName = map["clientName"] as? String ?: "",
            clientAddress = map["clientAddress"] as? String ?: "",
            clientPhone = map["clientPhone"] as? String ?: "",
            items = map["items"] as? String ?: "",
            totalAmount = (map["totalAmount"] as? Number)?.toDouble() ?: 0.0,
            createdAt = map["createdAt"] as? Timestamp
        )
    }
}
