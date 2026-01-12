package com.sladematthew.apm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


open class APMActivity : AppCompatActivity() {

    protected val REQUESTCODE = 4566

    protected lateinit var authenticationManager: AuthenticationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticationManager = (application as APMApplication).authenticationManager
    }
}
