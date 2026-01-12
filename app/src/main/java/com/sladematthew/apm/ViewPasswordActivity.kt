package com.sladematthew.apm

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
import com.sladematthew.apm.databinding.ActivityViewPasswordBinding
import com.sladematthew.apm.model.Password
import com.sladematthew.apm.viewmodel.ViewPasswordViewModel
import kotlinx.coroutines.launch


class ViewPasswordActivity : APMActivity(), View.OnClickListener
{
    override fun onClick(p0: View?) {
        if (p0 is TextView) {
            val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("password", p0.text.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.copied_to_clipboard, p0.text.toString()), Toast.LENGTH_SHORT).show()
        }
    }

    var password: Password? = null

    lateinit var binding: ActivityViewPasswordBinding

    private val viewModel: ViewPasswordViewModel by viewModels {
        (application as APMApplication).viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.title_view_password)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityViewPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        password = Gson().fromJson(intent.getStringExtra(Constants.IntentKey.PASSWORD)!!)
        password?.also {
            viewModel.setPassword(it, it.algorithm)
        }

        binding.passwordTextView.setOnClickListener(this)
        binding.usernameTextView.setOnClickListener(this)

        setupObservers()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.password.collect { pwd ->
                        pwd?.let {
                            binding.usernameTextView.text = it.username
                        }
                    }
                }

                launch {
                    viewModel.generatedPassword.collect { generatedPassword ->
                        binding.passwordTextView.text = generatedPassword
                    }
                }
            }
        }
    }

}