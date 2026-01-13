package com.sladematthew.apm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sladematthew.apm.model.Password
import com.sladematthew.apm.repository.PasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PasswordOperationState {
    object Idle : PasswordOperationState()
    object Loading : PasswordOperationState()
    object Success : PasswordOperationState()
    data class Error(val message: String) : PasswordOperationState()
}

@HiltViewModel
class EditPasswordViewModel @Inject constructor(
    private val repository: PasswordRepository
) : ViewModel() {

    private val _password = MutableStateFlow<Password?>(null)
    val password: StateFlow<Password?> = _password.asStateFlow()

    private val _generatedPassword = MutableStateFlow<String>("")
    val generatedPassword: StateFlow<String> = _generatedPassword.asStateFlow()

    private val _operationState = MutableStateFlow<PasswordOperationState>(PasswordOperationState.Idle)
    val operationState: StateFlow<PasswordOperationState> = _operationState.asStateFlow()

    fun setPassword(password: Password) {
        _password.value = password
        _generatedPassword.value = repository.generatePasswordFromModel(password)
    }

    fun generatePassword(password: Password) {
        _generatedPassword.value = repository.generatePasswordFromModel(password)
    }

    fun addOrUpdatePassword(password: Password) {
        viewModelScope.launch {
            try {
                _operationState.value = PasswordOperationState.Loading
                repository.addOrUpdatePassword(password)
                _password.value = password
                _operationState.value = PasswordOperationState.Success
            } catch (e: Exception) {
                _operationState.value = PasswordOperationState.Error(
                    e.message ?: "Failed to save password"
                )
            }
        }
    }

    fun deletePassword(password: Password) {
        viewModelScope.launch {
            try {
                _operationState.value = PasswordOperationState.Loading
                repository.deletePassword(password)
                _operationState.value = PasswordOperationState.Success
            } catch (e: Exception) {
                _operationState.value = PasswordOperationState.Error(
                    e.message ?: "Failed to delete password"
                )
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = PasswordOperationState.Idle
    }
}
