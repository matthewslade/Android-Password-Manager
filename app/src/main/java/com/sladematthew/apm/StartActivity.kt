package com.sladematthew.apm

import android.Manifest
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sladematthew.apm.databinding.ActivityStartBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class StartActivity : APMActivity() {

    val MY_PERMISSIONS_REQUEST_STORAGE = 12
    val REQUEST_CODE_GOOGLE_SIGN_IN = 14
    val REQUEST_CODE_GOOGLE_DRIVE_AUTH = 15

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.title_login)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.dropbox.setOnClickListener{
            GlobalScope.launch(Dispatchers.IO){
                val pendingIntent = authenticationManager.authRepository.signInGoogle()
                runOnUiThread{
                    try {
                        this@StartActivity.startIntentSenderForResult(
                            pendingIntent,
                            REQUEST_CODE_GOOGLE_SIGN_IN,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    } catch (e: SendIntentException) {
                        Log.e("APM", "Google Sign-in failed")
                    }
                }
            }
        }
        binding.login.setOnClickListener{ onLoginButtonClicked() }

        if(authenticationManager.sharedPreferences.contains(Constants.SharedPrefs.ACCESS_TOKEN)) {
            binding.dropbox.visibility = View.GONE
        }

        if(authenticationManager.sharedPreferences.contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH))
            binding.confirmPassword.visibility = View.GONE
        checkPermissions()
    }

    fun onLoginButtonClicked() {
        if(authenticationManager.sharedPreferences.contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH)) {
            if(authenticationManager.checkMasterPassword(binding.password.text.toString())) {
                startActivity(Intent(this,MainActivity::class.java))
                return
            }
            Toast.makeText(this,R.string.error_password_wrong,Toast.LENGTH_LONG).show()
        }
        else {
            if(binding.confirmPassword.text.toString() == binding.password.text.toString()) {
                authenticationManager.setMasterPassword(binding.password.text.toString())
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
        authenticationManager.clearMasterPassword()
        binding.password.setText("")
        binding.confirmPassword.setText("")

        GlobalScope.launch(Dispatchers.IO) {
            authenticationManager.loadDrive()
            runOnUiThread {
                if(authenticationManager.sharedPreferences.contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH))
                    binding.confirmPassword.visibility = View.GONE

                if(authenticationManager.drivePasswords.drive!=null)
                {
                    binding.dropbox.visibility = View.GONE
                }
            }
        }

        if(authenticationManager.sharedPreferences.contains(Constants.SharedPrefs.MASTER_PASSWORD_HASH))
            binding.confirmPassword.visibility = View.GONE

        if(authenticationManager.sharedPreferences.getString(Constants.SharedPrefs.ACCESS_TOKEN, null) != null)
        {
            binding.dropbox.visibility = View.GONE
        }
    }

    public override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_GOOGLE_SIGN_IN) {
                GlobalScope.launch(Dispatchers.IO) {
                    data?.also {
                        val result = authenticationManager.authRepository.getSignInResult(it)
                        runOnUiThread { Toast.makeText(this@StartActivity,"Signed in as ${result.email}",Toast.LENGTH_LONG).show()}
                        authenticationManager.authRepository.authorizeGoogleDrive().pendingIntent?.intentSender?.also {
                            runOnUiThread {
                                try {
                                    this@StartActivity.startIntentSenderForResult(
                                        it,
                                        REQUEST_CODE_GOOGLE_DRIVE_AUTH,
                                        null,
                                        0,
                                        0,
                                        0,
                                        null
                                    )
                                } catch (e: SendIntentException) {
                                    Log.e("APM", "Google Sign-in failed")
                                }
                            }
                        }?: run { authenticationManager.loadDrive() }
                    }
                }
            }
            if (requestCode == REQUEST_CODE_GOOGLE_DRIVE_AUTH) {
                GlobalScope.launch(Dispatchers.IO) {
                    data?.let { authenticationManager.authRepository.authorizeGoogleDriveResult(it)}?.accessToken?.also {
                        authenticationManager.loadDrive()
                        runOnUiThread { Toast.makeText(this@StartActivity,"load drive with access token $it",Toast.LENGTH_LONG).show()}
                    }
                }
            }
        }
    }
}
