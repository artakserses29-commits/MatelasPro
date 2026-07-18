package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class FournisseurPaiementFS(
    val id: String = "",
    val fournisseurId: String = "",
    val montant: Double = 0.0,
    val createdAt: Timestamp? = null
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "fournisseurId" to fournisseurId, "montant" to montant,
        "createdAt" to (createdAt ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): FournisseurPaiementFS = FournisseurPaiementFS(
            id = id,
            fournisseurId = map["fournisseurId"] as? String ?: "",
            montant = (map["montant"] as? Number)?.toDouble() ?: 0.0,
            createdAt = map["createdAt"] as? Timestamp
        )
    }
}
