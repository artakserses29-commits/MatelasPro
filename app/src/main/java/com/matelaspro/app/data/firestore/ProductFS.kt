package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class ProductFS(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val epaisseur: Double = 0.0,
    val prixUnitaireCm: Double = 0.0,
    val longueur: Double = 0.0,
    val description: String = "",
    val quantity: Int = 0,
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val fournisseur: String = "",
    val createdAt: Timestamp? = null
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "name" to name, "category" to category, "epaisseur" to epaisseur,
        "prixUnitaireCm" to prixUnitaireCm, "longueur" to longueur,
        "description" to description, "quantity" to quantity,
        "purchasePrice" to purchasePrice, "sellingPrice" to sellingPrice,
        "fournisseur" to fournisseur, "createdAt" to (createdAt ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): ProductFS = ProductFS(
            id = id,
            name = map["name"] as? String ?: "",
            category = map["category"] as? String ?: "",
            epaisseur = (map["epaisseur"] as? Number)?.toDouble() ?: 0.0,
            prixUnitaireCm = (map["prixUnitaireCm"] as? Number)?.toDouble() ?: 0.0,
            longueur = (map["longueur"] as? Number)?.toDouble() ?: 0.0,
            description = map["description"] as? String ?: "",
            quantity = (map["quantity"] as? Number)?.toInt() ?: 0,
            purchasePrice = (map["purchasePrice"] as? Number)?.toDouble() ?: 0.0,
            sellingPrice = (map["sellingPrice"] as? Number)?.toDouble() ?: 0.0,
            fournisseur = map["fournisseur"] as? String ?: "",
            createdAt = map["createdAt"] as? Timestamp
        )
    }
}
