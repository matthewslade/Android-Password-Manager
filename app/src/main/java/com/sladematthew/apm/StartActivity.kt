package com.sladematthew.apm

import android.Manifest
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sladematthew.apm.databinding.ActivityStartBinding
import com.sladematthew.apm.viewmodel.AuthState
import com.sladematthew.apm.viewmodel.StartViewModel
import kotlinx.coroutines.launch

class StartActivity : APMActivity() {

    val MY_PERMISSIONS_REQUEST_STORAGE = 12
    val REQUEST_CODE_GOOGLE_SIGN_IN = 14
    val REQUEST_CODE_GOOGLE_DRIVE_AUTH = 15

    private lateinit var binding: ActivityStartBinding

    private val viewModel: StartViewModel by viewModels {
        (application as APMApplication).viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.title_login)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.dropbox.setOnClickListener {
            viewModel.initiateGoogleSignIn()
        }
        binding.login.setOnClickListener { onLoginButtonClicked() }

        setupObservers()
        viewModel.checkAuthStatus()
        checkPermissions()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.authState.collect { state ->
                        handleAuthState(state)
                    }
                }

                launch {
                    viewModel.hasMasterPassword.collect { hasMasterPassword ->
                        binding.confirmPassword.visibility = if (hasMasterPassword) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.hasDriveAccess.collect { hasDriveAccess ->
                        binding.dropbox.visibility = if (hasDriveAccess) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    private fun handleAuthState(state: AuthState) {
        when (state) {
            is AuthState.GoogleSignInPending -> {
                try {
                    startIntentSenderForResult(
                        state.intentSender,
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
            is AuthState.GoogleDriveAuthPending -> {
                try {
                    startIntentSenderForResult(
                        state.intentSender,
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
            is AuthState.GoogleSignedIn -> {
                Toast.makeText(this, "Signed in as ${state.email}", Toast.LENGTH_LONG).show()
            }
            is AuthState.DriveLoaded -> {
                Toast.makeText(this, "Drive loaded successfully", Toast.LENGTH_LONG).show()
            }
            is AuthState.Error -> {
                Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetAuthState()
            }
            else -> {
                // Idle or Loading states
            }
        }
    }

    fun onLoginButtonClicked() {
        if (viewModel.hasMasterPassword.value) {
            if (viewModel.checkMasterPassword(binding.password.text.toString())) {
                startActivity(Intent(this, MainActivity::class.java))
                return
            }
            Toast.makeText(this, R.string.error_password_wrong, Toast.LENGTH_LONG).show()
        } else {
            if (binding.confirmPassword.text.toString() == binding.password.text.toString()) {
                viewModel.setMasterPassword(binding.password.text.toString())
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
        binding.password.setText("")
        binding.confirmPassword.setText("")

        // Check auth status and Drive connection without triggering a new load
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
