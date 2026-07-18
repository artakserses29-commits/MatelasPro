package com.matelaspro.app.data.firestore

import com.google.firebase.Timestamp

data class UserFS(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isAdmin: Boolean = false,
    val createdAt: Timestamp? = null
) {
    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "name" to name,
        "email" to email,
        "password" to password,
        "isAdmin" to isAdmin,
        "createdAt" to (createdAt ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): UserFS = UserFS(
            id = id,
            name = map["name"] as? String ?: "",
            email = map["email"] as? String ?: "",
            password = map["password"] as? String ?: "",
            isAdmin = map["isAdmin"] as? Boolean ?: false,
            createdAt = map["createdAt"] as? Timestamp
        )
    }
}
