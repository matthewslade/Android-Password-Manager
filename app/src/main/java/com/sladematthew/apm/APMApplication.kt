package com.sladematthew.apm

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class APMApplication : Application() {

    // Keep for backward compatibility during migration
    lateinit var authenticationManager: AuthenticationManager

    override fun onCreate() {
        super.onCreate()
        // Initialize legacy manager for backward compatibility
        authenticationManager = AuthenticationManager(this)
    }
}
