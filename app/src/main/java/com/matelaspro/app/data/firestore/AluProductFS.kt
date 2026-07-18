package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class AluProductFS(
    val id: String = "",
    val name: String = "",
    val surface: Double = 0.0,
    val prixUnitaire: Double = 0.0,
    val createdAt: Timestamp? = null
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "name" to name, "surface" to surface,
        "prixUnitaire" to prixUnitaire, "createdAt" to (createdAt ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AluProductFS = AluProductFS(
            id = id,
            name = map["name"] as? String ?: "",
            surface = (map["surface"] as? Number)?.toDouble() ?: 0.0,
            prixUnitaire = (map["prixUnitaire"] as? Number)?.toDouble() ?: 0.0,
            createdAt = map["createdAt"] as? Timestamp
        )
    }
}
