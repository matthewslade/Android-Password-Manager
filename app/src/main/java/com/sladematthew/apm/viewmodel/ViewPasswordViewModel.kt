package com.sladematthew.apm.viewmodel

import androidx.lifecycle.ViewModel
import com.sladematthew.apm.model.Password
import com.sladematthew.apm.repository.PasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ViewPasswordViewModel(
    private val repository: PasswordRepository
) : ViewModel() {

    private val _password = MutableStateFlow<Password?>(null)
    val password: StateFlow<Password?> = _password.asStateFlow()

    private val _generatedPassword = MutableStateFlow<String>("")
    val generatedPassword: StateFlow<String> = _generatedPassword.asStateFlow()

    fun setPassword(password: Password, algorithm: String) {
        _password.value = password
        // Use algorithm to determine which generation method to use
        _generatedPassword.value = if (algorithm == "SHA256") {
            repository.generatePassword2(password)
        } else {
            repository.generatePassword(password)
        }
    }
}
