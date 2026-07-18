package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class AluNoteExpenseFS(
    val id: String = "",
    val noteId: String = "",
    val description: String = "",
    val montant: Double = 0.0,
    val createdAt: Timestamp? = null
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "noteId" to noteId, "description" to description,
        "montant" to montant, "createdAt" to (createdAt ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AluNoteExpenseFS = AluNoteExpenseFS(
            id = id,
            noteId = map["noteId"] as? String ?: "",
            description = map["description"] as? String ?: "",
            montant = (map["montant"] as? Number)?.toDouble() ?: 0.0,
            createdAt = map["createdAt"] as? Timestamp
        )
    }
}
