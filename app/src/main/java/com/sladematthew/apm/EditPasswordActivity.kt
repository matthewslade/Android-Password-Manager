package com.sladematthew.apm

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.sladematthew.apm.databinding.ActivityEditPasswordBinding
import com.sladematthew.apm.model.Password
import com.sladematthew.apm.viewmodel.EditPasswordViewModel
import com.sladematthew.apm.viewmodel.PasswordOperationState
import kotlinx.coroutines.launch

class EditPasswordActivity : APMActivity(), View.OnClickListener
{

    var password: Password? = null

    private lateinit var binding: ActivityEditPasswordBinding

    private val viewModel: EditPasswordViewModel by viewModels {
        (application as APMApplication).viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.title_edit_password)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityEditPasswordBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (intent.hasExtra(Constants.IntentKey.PASSWORD)) {
            password = Gson().fromJson(intent.getStringExtra(Constants.IntentKey.PASSWORD)!!)
            password?.let { viewModel.setPassword(it) }
        }

        binding.updateButton.setOnClickListener { onUpdateButtonClicked() }
        binding.deleteButton.setOnClickListener { onDeleteClicked() }
        binding.plus.setOnClickListener { onPlusClicked() }
        binding.passwordTextView.setOnClickListener(this)

        setupObservers()
        initializeFields()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.generatedPassword.collect { generatedPassword ->
                        binding.passwordTextView.text = generatedPassword
                    }
                }

                launch {
                    viewModel.operationState.collect { state ->
                        when (state) {
                            is PasswordOperationState.Success -> {
                                finish()
                            }
                            is PasswordOperationState.Error -> {
                                Toast.makeText(this@EditPasswordActivity, state.message, Toast.LENGTH_SHORT).show()
                                viewModel.resetOperationState()
                            }
                            else -> {
                                // Idle or Loading states
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initializeFields() {
        if (password != null) {
            binding.labelTextView.setText(password!!.label)
            binding.usernameTextView.setText(password!!.username)
            binding.lengthTextView.setText(password!!.length.toString())
            binding.prefixTextView.setText(password!!.prefix)
            binding.versionTextView.setText(password!!.version.toString())
        } else {
            binding.prefixTextView.setText(Constants.Misc.DEFAULT_PREFIX)
            binding.lengthTextView.setText(Constants.Misc.DEFAULT_LENGTH.toString())
            binding.versionTextView.setText(1.toString())
        }
    }

    private fun onPlusClicked() {
        binding.versionTextView.setText((binding.versionTextView.text.toString().toInt()+1).toString())
    }

    private fun onDeleteClicked() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> deletePassword() }
            .setNegativeButton(android.R.string.cancel) { dialogInterface, _ -> dialogInterface.dismiss() }
            .show()
    }

    private fun deletePassword() {
        password?.let { viewModel.deletePassword(it) }
    }

    private fun onUpdateButtonClicked() {
        val version = binding.versionTextView.text.toString().toInt()
        val length = binding.lengthTextView.text.toString().toInt()

        if (version !in 1..9999) {
            Toast.makeText(this, R.string.error_version, Toast.LENGTH_LONG).show()
            return
        }

        if (length !in 4..24) {
            Toast.makeText(this, R.string.error_length, Toast.LENGTH_LONG).show()
            return
        }

        password = Password(
            Constants.Misc.ALGORITHM,
            binding.labelTextView.text.toString().toLowerCase().trim().replace(" ", ""),
            version,
            length,
            binding.prefixTextView.text.toString(),
            binding.usernameTextView.text.toString()
        )
        viewModel.generatePassword(password!!)
        viewModel.addOrUpdatePassword(password!!)
    }

    override fun onClick(p0: View?) {
        if (p0 is TextView) {
            val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("password", p0.text.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.copied_to_clipboard,p0.text.toString()), Toast.LENGTH_SHORT).show()
        }
    }

}
