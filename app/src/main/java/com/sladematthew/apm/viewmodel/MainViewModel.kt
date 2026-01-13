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

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: PasswordRepository
) : ViewModel() {

    private val _passwordList = MutableStateFlow<List<Password>>(emptyList())
    val passwordList: StateFlow<List<Password>> = _passwordList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadPasswordList() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                repository.loadPasswordList()
                val list = repository.getPasswordList()
                _passwordList.value = list?.passwords?.values?.toList() ?: emptyList()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load passwords"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
