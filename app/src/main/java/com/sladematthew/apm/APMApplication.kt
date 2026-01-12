package com.sladematthew.apm

import android.app.Application
import com.sladematthew.apm.repository.PasswordRepository
import com.sladematthew.apm.viewmodel.ViewModelFactory

class APMApplication : Application() {

    // Keep for backward compatibility during migration
    lateinit var authenticationManager: AuthenticationManager

    // New repositories
    lateinit var passwordRepository: PasswordRepository
    lateinit var authRepository: AuthRepository
    lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate() {
        super.onCreate()
        // Initialize legacy manager for backward compatibility
        authenticationManager = AuthenticationManager(this)

        // Initialize new repositories
        passwordRepository = PasswordRepository(this)
        authRepository = AuthRepository(this)
        viewModelFactory = ViewModelFactory(passwordRepository, authRepository)
    }
}
