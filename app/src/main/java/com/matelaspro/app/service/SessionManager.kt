package com.matelaspro.app.service

object SessionManager {
    var currentUserId: String = ""
    var currentUserName: String = ""
    var isAdmin: Boolean = false
    var isLoggedIn: Boolean = false

    fun login(userId: String, name: String, admin: Boolean) {
        currentUserId = userId
        currentUserName = name
        isAdmin = admin
        isLoggedIn = true
    }

    fun logout() {
        currentUserId = ""
        currentUserName = ""
        isAdmin = false
        isLoggedIn = false
    }
}
