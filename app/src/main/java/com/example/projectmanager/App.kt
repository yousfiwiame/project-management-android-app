package com.example.projectmanager

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize basic Firebase App Check without provider
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            Timber.d("Firebase App Check initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase App Check")
        }
        
        // Initialize Timber for logging if in debug mode
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}