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

    fun setPassword(password: Password) {
        _password.value = password
        // Use the unified password generation method which automatically determines
        // the correct algorithm from the password model (backward compatible)
        _generatedPassword.value = repository.generatePasswordFromModel(password)
    }
}
