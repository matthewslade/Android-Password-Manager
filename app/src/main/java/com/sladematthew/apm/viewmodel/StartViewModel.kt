package com.sladematthew.apm.viewmodel

import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sladematthew.apm.AuthRepository
import com.sladematthew.apm.repository.PasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class GoogleSignInPending(val intentSender: IntentSender) : AuthState()
    data class GoogleDriveAuthPending(val intentSender: IntentSender) : AuthState()
    data class GoogleSignedIn(val email: String) : AuthState()
    data class DriveLoaded(val hasAccessToken: Boolean) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class StartViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _hasMasterPassword = MutableStateFlow(false)
    val hasMasterPassword: StateFlow<Boolean> = _hasMasterPassword.asStateFlow()

    private val _hasDriveAccess = MutableStateFlow(false)
    val hasDriveAccess: StateFlow<Boolean> = _hasDriveAccess.asStateFlow()

    // Password field state for clearing on resume
    private val _passwordFieldsCleared = MutableStateFlow(false)
    val passwordFieldsCleared: StateFlow<Boolean> = _passwordFieldsCleared.asStateFlow()

    fun clearPasswordFields() {
        _passwordFieldsCleared.value = true
    }

    fun resetPasswordFieldsClearedState() {
        _passwordFieldsCleared.value = false
    }

    fun checkAuthStatus() {
        _hasMasterPassword.value = passwordRepository.hasMasterPassword()
        // Check drive connection in a coroutine since it's now suspend
        viewModelScope.launch {
            _hasDriveAccess.value = passwordRepository.hasDriveConnection()
        }
    }

    fun clearMasterPassword() {
        passwordRepository.clearMasterPassword()
    }

    fun setMasterPassword(password: String) {
        passwordRepository.setMasterPassword(password)
        _hasMasterPassword.value = true
    }

    fun checkMasterPassword(password: String): Boolean {
        return passwordRepository.checkMasterPassword(password)
    }

    fun initiateGoogleSignIn() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val pendingIntent = authRepository.signInGoogle()
                _authState.value = AuthState.GoogleSignInPending(pendingIntent)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google sign-in failed")
            }
        }
    }

    fun handleGoogleSignInResult(data: Intent) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = authRepository.getSignInResult(data)
                _authState.value = AuthState.GoogleSignedIn(result.email ?: "Unknown")

                val authResult = authRepository.authorizeGoogleDrive()
                authResult.pendingIntent?.intentSender?.let {
                    _authState.value = AuthState.GoogleDriveAuthPending(it)
                } ?: run {
                    loadDrive()
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to process sign-in")
            }
        }
    }

    fun handleGoogleDriveAuthResult(data: Intent) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val accessToken = authRepository.authorizeGoogleDriveResult(data)?.accessToken
                accessToken?.let {
                    loadDrive()
                    _authState.value = AuthState.DriveLoaded(true)
                } ?: run {
                    _authState.value = AuthState.Error("Failed to get access token")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Drive authorization failed")
            }
        }
    }

    fun loadDrive() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val isConnected = passwordRepository.loadDrive()
                _hasDriveAccess.value = isConnected
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to load Drive")
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}
