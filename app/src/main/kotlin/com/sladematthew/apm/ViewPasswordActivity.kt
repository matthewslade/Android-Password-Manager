package com.sladematthew.apm

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.sladematthew.apm.model.Password
import kotlinx.android.synthetic.main.activity_view_password.*


class ViewPasswordActivity : APMActivity(), View.OnClickListener
{
    override fun onClick(p0: View?) {
        if (p0 is TextView) {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = ClipData.newPlainText("password", p0.text.toString());
            Toast.makeText(this, getString(R.string.copied_to_clipboard,p0.text.toString()), Toast.LENGTH_SHORT).show()
        }
    }

    var password: Password?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_view_password)
        if(intent.hasExtra(Constants.IntentKey.PASSWORD))
            password = intent.getSerializableExtra(Constants.IntentKey.PASSWORD) as Password

        if(password!=null)
        {
            passwordTextView.text = authenticationManager!!.generatePassword(password!!)
            password2TextView.text = authenticationManager!!.generatePassword2(password!!)
            usernameTextView.text = password!!.username

            passwordTextView.setOnClickListener(this)
            password2TextView.setOnClickListener(this)
            usernameTextView.setOnClickListener(this)
        }
    }

}