package com.sladematthew.apm

import android.app.Activity
import android.os.Bundle


open class APMActivity : Activity() {

    protected val REQUESTCODE = 4566

    protected lateinit var authenticationManager: AuthenticationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticationManager = (application as APMApplication).authenticationManager
    }
}
