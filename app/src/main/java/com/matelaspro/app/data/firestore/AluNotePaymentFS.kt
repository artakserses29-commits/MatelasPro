package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class AluNotePaymentFS(
    val id: String = "",
    val noteId: String = "",
    val montant: Double = 0.0,
    val createdAt: Timestamp? = null
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "noteId" to noteId, "montant" to montant,
        "createdAt" to (createdAt ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AluNotePaymentFS = AluNotePaymentFS(
            id = id,
            noteId = map["noteId"] as? String ?: "",
            montant = (map["montant"] as? Number)?.toDouble() ?: 0.0,
            createdAt = map["createdAt"] as? Timestamp
        )
    }
}
