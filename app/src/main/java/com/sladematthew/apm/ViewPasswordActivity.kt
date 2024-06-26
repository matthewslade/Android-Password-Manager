package com.sladematthew.apm

import android.R.attr.label
import android.R.attr.text
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.sladematthew.apm.databinding.ActivityViewPasswordBinding
import com.sladematthew.apm.model.Password


class ViewPasswordActivity : APMActivity(), View.OnClickListener
{
    override fun onClick(p0: View?) {
        if (p0 is TextView) {
            val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("password", p0.text.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.copied_to_clipboard,p0.text.toString()), Toast.LENGTH_SHORT).show()
        }
    }

    var password: Password?=null

    lateinit var binding: ActivityViewPasswordBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.title_view_password)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityViewPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        password = Gson().fromJson(intent.getStringExtra(Constants.IntentKey.PASSWORD)!!)
        password?.also {
            if(it.algorithm == Constants.Misc.ALGORITHM)
                binding.passwordTextView.text = authenticationManager.generatePassword2(it)
            else
                binding.passwordTextView.text = authenticationManager.generatePassword(it)

            binding.usernameTextView.text = it.username
            binding.passwordTextView.setOnClickListener(this)
            binding.usernameTextView.setOnClickListener(this)
        }
    }

}