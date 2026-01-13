package com.sladematthew.apm


import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.gson.Gson
import com.sladematthew.apm.model.Password
import com.sladematthew.apm.ui.screens.MainScreen
import com.sladematthew.apm.ui.theme.PasswordManagerTheme
import com.sladematthew.apm.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : APMActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PasswordManagerTheme {
                val passwords by viewModel.passwordList.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val error by viewModel.error.collectAsState()

                // Show error toast
                error?.let {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }

                MainScreen(
                    passwords = passwords,
                    isLoading = isLoading,
                    onPasswordClick = { password ->
                        val intent = Intent(this, ViewPasswordActivity::class.java)
                        intent.putExtra(Constants.IntentKey.PASSWORD, Gson().toJson(password))
                        startActivity(intent)
                    },
                    onPasswordLongClick = { password ->
                        val intent = Intent(this, EditPasswordActivity::class.java)
                        intent.putExtra(Constants.IntentKey.PASSWORD, Gson().toJson(password))
                        startActivity(intent)
                    },
                    onAddPasswordClick = {
                        startActivity(Intent(this, EditPasswordActivity::class.java))
                    }
                )
            }
        }

        loadPasswordList()
    }

    private fun loadPasswordList() {
        viewModel.loadPasswordList()
    }

    override fun onResume() {
        super.onResume()
        loadPasswordList()
    }
}
