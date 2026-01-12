package com.sladematthew.apm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sladematthew.apm.AuthRepository
import com.sladematthew.apm.repository.PasswordRepository

class ViewModelFactory(
    private val passwordRepository: PasswordRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(passwordRepository) as T
            }
            modelClass.isAssignableFrom(StartViewModel::class.java) -> {
                StartViewModel(passwordRepository, authRepository) as T
            }
            modelClass.isAssignableFrom(EditPasswordViewModel::class.java) -> {
                EditPasswordViewModel(passwordRepository) as T
            }
            modelClass.isAssignableFrom(ViewPasswordViewModel::class.java) -> {
                ViewPasswordViewModel(passwordRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
