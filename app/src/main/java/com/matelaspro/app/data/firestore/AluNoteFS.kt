package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class AluNoteFS(
    val id: String = "",
    val clientName: String = "",
    val description: String = "",
    val montantTotal: Double = 0.0,
    val montantPaye: Double = 0.0,
    val resteAPaye: Double = 0.0,
    val createdAt: Timestamp? = null
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "clientName" to clientName, "description" to description,
        "montantTotal" to montantTotal, "montantPaye" to montantPaye,
        "resteAPaye" to resteAPaye, "createdAt" to (createdAt ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AluNoteFS = AluNoteFS(
            id = id,
            clientName = map["clientName"] as? String ?: "",
            description = map["description"] as? String ?: "",
            montantTotal = (map["montantTotal"] as? Number)?.toDouble() ?: 0.0,
            montantPaye = (map["montantPaye"] as? Number)?.toDouble() ?: 0.0,
            resteAPaye = (map["resteAPaye"] as? Number)?.toDouble() ?: 0.0,
            createdAt = map["createdAt"] as? Timestamp
        )
    }
}
