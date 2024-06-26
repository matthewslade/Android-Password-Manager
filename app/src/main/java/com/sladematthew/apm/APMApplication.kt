package com.sladematthew.apm

import android.app.Application

class APMApplication : Application() {

    lateinit var authenticationManager:AuthenticationManager

    override fun onCreate() {
        super.onCreate()
        authenticationManager = AuthenticationManager(this)
    }
}
