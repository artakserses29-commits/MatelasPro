package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class FournisseurFS(
    val id: String = "",
    val name: String = "",
    val createdAt: Timestamp? = null
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "name" to name, "createdAt" to (createdAt ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): FournisseurFS = FournisseurFS(
            id = id,
            name = map["name"] as? String ?: "",
            createdAt = map["createdAt"] as? Timestamp
        )
    }
}
