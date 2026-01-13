package com.sladematthew.apm

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.sladematthew.apm.model.Password
import com.sladematthew.apm.ui.screens.ViewPasswordScreen
import com.sladematthew.apm.ui.theme.PasswordManagerTheme
import com.sladematthew.apm.viewmodel.ViewPasswordViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ViewPasswordActivity : APMActivity() {

    var password: Password? = null

    private val viewModel: ViewPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        password = Gson().fromJson(intent.getStringExtra(Constants.IntentKey.PASSWORD)!!)
        password?.also {
            viewModel.setPassword(it)
        }

        setContent {
            PasswordManagerTheme {
                val pwd by viewModel.password.collectAsState()
                val generatedPassword by viewModel.generatedPassword.collectAsState()

                pwd?.let { password ->
                    ViewPasswordScreen(
                        password = password,
                        generatedPassword = generatedPassword,
                        onBackClick = {
                            finish()
                        },
                        onCopySuccess = { message ->
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

}