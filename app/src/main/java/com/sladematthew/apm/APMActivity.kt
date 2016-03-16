package com.sladematthew.apm

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity

open class APMActivity : AppCompatActivity() {

    protected var authenticationManager: AuthenticationManager?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticationManager = (application as APMApplication).authenticationManager
    }

    override fun onResume() {
        super.onResume()
        if(!this.javaClass.name.equals(StartActivity::class.java.name) && this.javaClass.name.equals(PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.SharedPrefs.LAST_ACTIVITY,null)))
        {
            var intent = Intent(this, StartActivity::class.java);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(Constants.SharedPrefs.LAST_ACTIVITY,this.javaClass.name).commit()
    }
}
