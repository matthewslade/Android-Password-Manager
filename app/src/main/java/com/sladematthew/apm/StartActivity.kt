package com.sladematthew.apm

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sladematthew.apm.ui.screens.StartScreen
import com.sladematthew.apm.ui.theme.PasswordManagerTheme
import com.sladematthew.apm.viewmodel.AuthState
import com.sladematthew.apm.viewmodel.StartViewModel

class StartActivity : APMActivity() {

    val MY_PERMISSIONS_REQUEST_STORAGE = 12
    val REQUEST_CODE_GOOGLE_SIGN_IN = 14
    val REQUEST_CODE_GOOGLE_DRIVE_AUTH = 15

    private val viewModel: StartViewModel by viewModels {
        (application as APMApplication).viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PasswordManagerTheme {
                val hasMasterPassword by viewModel.hasMasterPassword.collectAsState()
                val hasDriveAccess by viewModel.hasDriveAccess.collectAsState()
                val authState by viewModel.authState.collectAsState()

                // Handle auth state
                when (authState) {
                    is AuthState.GoogleSignInPending -> {
                        handleGoogleSignInPending((authState as AuthState.GoogleSignInPending).intentSender)
                    }
                    is AuthState.GoogleDriveAuthPending -> {
                        handleGoogleDriveAuthPending((authState as AuthState.GoogleDriveAuthPending).intentSender)
                    }
                    is AuthState.GoogleSignedIn -> {
                        Toast.makeText(
                            this,
                            "Signed in as ${(authState as AuthState.GoogleSignedIn).email}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is AuthState.DriveLoaded -> {
                        Toast.makeText(this, "Drive loaded successfully", Toast.LENGTH_LONG).show()
                    }
                    is AuthState.Error -> {
                        Toast.makeText(
                            this,
                            (authState as AuthState.Error).message,
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.resetAuthState()
                    }
                    else -> {
                        // Idle or Loading states
                    }
                }

                StartScreen(
                    hasMasterPassword = hasMasterPassword,
                    hasDriveAccess = hasDriveAccess,
                    authState = authState,
                    onLoginClick = { password, confirmPassword ->
                        onLoginButtonClicked(password, confirmPassword)
                    },
                    onGoogleSignInClick = {
                        viewModel.initiateGoogleSignIn()
                    }
                )
            }
        }

        viewModel.checkAuthStatus()
        checkPermissions()
    }

    private fun handleGoogleSignInPending(intentSender: IntentSender) {
        try {
            startIntentSenderForResult(
                intentSender,
                REQUEST_CODE_GOOGLE_SIGN_IN,
                null,
                0,
                0,
                0,
                null
            )
        } catch (e: SendIntentException) {
            Log.e("APM", "Google Sign-in failed", e)
            Toast.makeText(this, "Google Sign-in failed", Toast.LENGTH_SHORT).show()
        }
        viewModel.resetAuthState()
    }

    private fun handleGoogleDriveAuthPending(intentSender: IntentSender) {
        try {
            startIntentSenderForResult(
                intentSender,
                REQUEST_CODE_GOOGLE_DRIVE_AUTH,
                null,
                0,
                0,
                0,
                null
            )
        } catch (e: SendIntentException) {
            Log.e("APM", "Google Drive auth failed", e)
            Toast.makeText(this, "Google Drive auth failed", Toast.LENGTH_SHORT).show()
        }
        viewModel.resetAuthState()
    }

    private fun onLoginButtonClicked(password: String, confirmPassword: String) {
        if (viewModel.hasMasterPassword.value) {
            if (viewModel.checkMasterPassword(password)) {
                startActivity(Intent(this, MainActivity::class.java))
                return
            }
            Toast.makeText(this, R.string.error_password_wrong, Toast.LENGTH_LONG).show()
        } else {
            if (confirmPassword == password) {
                viewModel.setMasterPassword(password)
                startActivity(Intent(this, MainActivity::class.java))
                return
            }
            Toast.makeText(this, R.string.error_password_mismatch, Toast.LENGTH_LONG).show()
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
        viewModel.clearMasterPassword()
        viewModel.checkAuthStatus()
    }

    public override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_GOOGLE_SIGN_IN -> {
                    data?.let { viewModel.handleGoogleSignInResult(it) }
                }
                REQUEST_CODE_GOOGLE_DRIVE_AUTH -> {
                    data?.let { viewModel.handleGoogleDriveAuthResult(it) }
                }
            }
        }
    }
}
