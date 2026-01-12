package com.sladematthew.apm

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.sladematthew.apm.model.Password
import com.sladematthew.apm.ui.screens.EditPasswordScreen
import com.sladematthew.apm.ui.theme.PasswordManagerTheme
import com.sladematthew.apm.viewmodel.EditPasswordViewModel
import com.sladematthew.apm.viewmodel.PasswordOperationState

class EditPasswordActivity : APMActivity() {

    var password: Password? = null

    private val viewModel: EditPasswordViewModel by viewModels {
        (application as APMApplication).viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        if (intent.hasExtra(Constants.IntentKey.PASSWORD)) {
            password = Gson().fromJson(intent.getStringExtra(Constants.IntentKey.PASSWORD)!!)
            password?.let { viewModel.setPassword(it) }
        }

        setContent {
            PasswordManagerTheme {
                val generatedPassword by viewModel.generatedPassword.collectAsState()
                val operationState by viewModel.operationState.collectAsState()

                // Handle operation state
                LaunchedEffect(operationState) {
                    when (operationState) {
                        is PasswordOperationState.Success -> {
                            finish()
                        }
                        is PasswordOperationState.Error -> {
                            Toast.makeText(
                                this@EditPasswordActivity,
                                (operationState as PasswordOperationState.Error).message,
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.resetOperationState()
                        }
                        else -> {
                            // Idle or Loading states
                        }
                    }
                }

                EditPasswordScreen(
                    password = password,
                    generatedPassword = generatedPassword,
                    onSaveClick = { label, username, version, length, prefix ->
                        // Preserve algorithm from existing password, or use ARGON2ID for new ones
                        val algorithm = password?.algorithm ?: Constants.Misc.ALGORITHM
                        val newPassword = Password(
                            algorithm,
                            label.lowercase().trim().replace(" ", ""),
                            version,
                            length,
                            prefix,
                            username
                        )
                        viewModel.generatePassword(newPassword)
                        viewModel.addOrUpdatePassword(newPassword)
                    },
                    onDeleteClick = {
                        password?.let { viewModel.deletePassword(it) }
                    },
                    onVersionIncrement = {
                        // Version increment is handled in the UI
                    },
                    onBackClick = {
                        finish()
                    }
                )
            }
        }
    }
}
