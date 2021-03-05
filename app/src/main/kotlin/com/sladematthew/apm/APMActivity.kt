package com.sladematthew.apm

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity


open class APMActivity : AppCompatActivity() {

    protected val REQUESTCODE = 4566

    protected var authenticationManager: AuthenticationManager?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticationManager = (application as APMApplication).authenticationManager
    }
}
