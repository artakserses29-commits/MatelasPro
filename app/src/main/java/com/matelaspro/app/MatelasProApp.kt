package com.matelaspro.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.matelaspro.app.service.FirestoreService

class MatelasProApp : Application() {
    val firestoreService: FirestoreService by lazy { FirestoreService() }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
