package com.sladematthew.apm

import android.content.Intent
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_start.*

class StartActivity : APMActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        dropbox.setOnClickListener{authenticationManager!!.authWithDropbox(this)}
        login.setOnClickListener{onLoginButtonClicked()}

        if(PreferenceManager.getDefaultSharedPreferences(this).contains(Constants.SharedPrefs.ACCESS_TOKEN)) {
            dropbox.visibility = View.GONE
            authenticationManager!!.authWithDropbox(this)
        }

        if(PreferenceManager.getDefaultSharedPreferences(this).contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH))
            confirmPassword.visibility = View.GONE
    }

    fun onLoginButtonClicked()
    {
        if(PreferenceManager.getDefaultSharedPreferences(this).contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH))
        {
            if(authenticationManager!!.checkMasterPassword(password.text.toString()))
            {
                startActivity(Intent(this,MainActivity::class.java))
                return
            }
            Toast.makeText(this,R.string.error_password_wrong,Toast.LENGTH_LONG).show()
        }
        else
        {
            if(confirmPassword.text.toString().equals(password.text.toString()))
            {
                authenticationManager!!.setMasterPassword(password.text.toString())
                startActivity(Intent(this,MainActivity::class.java))
                return
            }
            Toast.makeText(this,R.string.error_password_mismatch,Toast.LENGTH_LONG).show()
        }

    }

    override fun onResume() {
        super.onResume()
        if(authenticationManager!!.authCompleted())
        {
            dropbox.visibility = View.GONE
        }
    }
}
