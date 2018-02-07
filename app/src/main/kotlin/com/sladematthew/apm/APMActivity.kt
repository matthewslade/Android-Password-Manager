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

    override fun onResume() {
        super.onResume()
        if(this.javaClass.name != StartActivity::class.java.name
                && this.javaClass.name != MainActivity::class.java.name
                && this.javaClass.name == PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.SharedPrefs.LAST_ACTIVITY,null)
                && !authenticationManager!!.isAuthenticated())
            showAuthenticationScreen()

    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(Constants.SharedPrefs.LAST_ACTIVITY,this.javaClass.name).apply()
    }

    fun showAuthenticationScreen() {
        val intent = (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).createConfirmDeviceCredentialIntent(getString(R.string.app_name), null)
        if (intent != null) {
            startActivityForResult(intent, REQUESTCODE)
        }
    }
}
