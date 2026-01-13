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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sladematthew.apm.ui.screens.StartScreen
import com.sladematthew.apm.ui.theme.PasswordManagerTheme
import com.sladematthew.apm.viewmodel.AuthState
import com.sladematthew.apm.viewmodel.StartViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StartActivity : APMActivity() {

    val MY_PERMISSIONS_REQUEST_STORAGE = 12

    private val viewModel: StartViewModel by viewModels()

    // Modern Activity Result API launcher for Google Sign-In
    private val googleSignInLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { viewModel.handleGoogleSignInResult(it) }
            }
        }

    // Modern Activity Result API launcher for Google Drive authorization
    private val googleDriveAuthLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { viewModel.handleGoogleDriveAuthResult(it) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PasswordManagerTheme {
                val hasMasterPassword by viewModel.hasMasterPassword.collectAsState()
                val hasDriveAccess by viewModel.hasDriveAccess.collectAsState()
                val authState by viewModel.authState.collectAsState()
                val passwordFieldsCleared by viewModel.passwordFieldsCleared.collectAsState()

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
                    passwordFieldsCleared = passwordFieldsCleared,
                    onPasswordFieldsClearedAcknowledged = {
                        viewModel.resetPasswordFieldsClearedState()
                    },
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
            val request = IntentSenderRequest.Builder(intentSender).build()
            googleSignInLauncher.launch(request)
        } catch (e: SendIntentException) {
            Log.e("APM", "Google Sign-in failed", e)
            Toast.makeText(this, "Google Sign-in failed", Toast.LENGTH_SHORT).show()
        }
        viewModel.resetAuthState()
    }

    private fun handleGoogleDriveAuthPending(intentSender: IntentSender) {
        try {
            val request = IntentSenderRequest.Builder(intentSender).build()
            googleDriveAuthLauncher.launch(request)
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
        viewModel.clearPasswordFields()
        viewModel.checkAuthStatus()
    }
}
