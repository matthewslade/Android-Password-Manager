package com.sladematthew.apm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

open class APMActivity : AppCompatActivity() {

    protected var authenticationManager: AuthenticationManager?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticationManager = (application as APMApplication).authenticationManager
    }
}
