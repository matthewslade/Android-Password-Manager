package com.sladematthew.apm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.Toast
import com.dropbox.core.android.Auth
import kotlinx.android.synthetic.main.activity_start.*

class StartActivity : APMActivity() {

    val MY_PERMISSIONS_REQUEST_STORAGE = 12

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.title_login)
        setContentView(R.layout.activity_start)
        dropbox.setOnClickListener{ Auth.startOAuth2Authentication(this,Constants.Credentials.APP_KEY)}
        login.setOnClickListener{onLoginButtonClicked()}

        if(PreferenceManager.getDefaultSharedPreferences(this).contains(Constants.SharedPrefs.ACCESS_TOKEN)) {
            dropbox.visibility = View.GONE
        }

        if(PreferenceManager.getDefaultSharedPreferences(this).contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH))
            confirmPassword.visibility = View.GONE
        checkPermissions()
    }

    fun onLoginButtonClicked() {
        if(PreferenceManager.getDefaultSharedPreferences(this).contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH)) {
            if(authenticationManager!!.checkMasterPassword(password.text.toString())) {
                startActivity(Intent(this,MainActivity::class.java))
                return
            }
            Toast.makeText(this,R.string.error_password_wrong,Toast.LENGTH_LONG).show()
        }
        else {
            if(confirmPassword.text.toString() == password.text.toString()) {
                authenticationManager!!.setMasterPassword(password.text.toString())
                startActivity(Intent(this,MainActivity::class.java))
                return
            }
            Toast.makeText(this,R.string.error_password_mismatch,Toast.LENGTH_LONG).show()
        }

    }

    fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE),MY_PERMISSIONS_REQUEST_STORAGE);
            }
        }
    }

    override fun onResume() {
        super.onResume()
        authenticationManager!!.clearMasterPassword()
        password.setText("")
        confirmPassword.setText("")

        if(PreferenceManager.getDefaultSharedPreferences(this).contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH))
            confirmPassword.visibility = View.GONE

        if(PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.SharedPrefs.ACCESS_TOKEN, null) != null)
        {
            dropbox.visibility = View.GONE
        }
    }
}
