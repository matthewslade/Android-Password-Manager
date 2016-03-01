package com.sladematthew.apm

import android.app.Application

class APMApplication : Application() {

    var authenticationManager:AuthenticationManager?=null

    override fun onCreate() {
        super.onCreate()
        authenticationManager = AuthenticationManager(this)
    }
}
