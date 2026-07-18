package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class CreditEntryFS(
    val id: String = "",
    val fournisseur: String = "",
    val productName: String = "",
    val category: String = "",
    val epaisseur: Double = 0.0,
    val longueur: Double = 0.0,
    val description: String = "",
    val quantity: Int = 0,
    val sellingPrice: Double = 0.0,
    val createdAt: Timestamp? = null,
    val notes: String = "",
    val productId: String = "",
    val isOverridden: Boolean = false,
    val correctionCount: Int = 0
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "fournisseur" to fournisseur, "productName" to productName,
        "category" to category, "epaisseur" to epaisseur,
        "longueur" to longueur, "description" to description,
        "quantity" to quantity, "sellingPrice" to sellingPrice,
        "createdAt" to (createdAt ?: Timestamp.now()),
        "notes" to notes, "productId" to productId,
        "isOverridden" to isOverridden, "correctionCount" to correctionCount
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): CreditEntryFS = CreditEntryFS(
            id = id,
            fournisseur = map["fournisseur"] as? String ?: "",
            productName = map["productName"] as? String ?: "",
            category = map["category"] as? String ?: "",
            epaisseur = (map["epaisseur"] as? Number)?.toDouble() ?: 0.0,
            longueur = (map["longueur"] as? Number)?.toDouble() ?: 0.0,
            description = map["description"] as? String ?: "",
            quantity = (map["quantity"] as? Number)?.toInt() ?: 0,
            sellingPrice = (map["sellingPrice"] as? Number)?.toDouble() ?: 0.0,
            createdAt = map["createdAt"] as? Timestamp,
            notes = map["notes"] as? String ?: "",
            productId = map["productId"] as? String ?: "",
            isOverridden = map["isOverridden"] as? Boolean ?: false,
            correctionCount = (map["correctionCount"] as? Number)?.toInt() ?: 0
        )
    }
}
