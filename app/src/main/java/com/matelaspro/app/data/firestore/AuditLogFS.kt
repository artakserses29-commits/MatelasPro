package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class AuditLogFS(
    val id: String = "",
    val action: String = "",
    val tableName: String = "",
    val recordId: String = "",
    val detail: String = "",
    val createdAt: Timestamp? = null
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "action" to action, "tableName" to tableName,
        "recordId" to recordId, "detail" to detail,
        "createdAt" to (createdAt ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AuditLogFS = AuditLogFS(
            id = id,
            action = map["action"] as? String ?: "",
            tableName = map["tableName"] as? String ?: "",
            recordId = map["recordId"] as? String ?: "",
            detail = map["detail"] as? String ?: "",
            createdAt = map["createdAt"] as? Timestamp
        )
    }
}
