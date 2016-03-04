package com.sladematthew.apm

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.sladematthew.apm.model.Password
import kotlinx.android.synthetic.main.activity_view_password.*


class ViewPasswordActivity : APMActivity()
{
    var password: Password?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_password)
        if(intent.hasExtra(Constants.IntentKey.PASSWORD))
            password = intent.getSerializableExtra(Constants.IntentKey.PASSWORD) as Password;

        if(password!=null)
        {
            passwordTextView.text = authenticationManager!!.generatePassword(password!!)
            usernameTextView.text = password!!.username;
        }
        showButton.setOnClickListener{passwordTextView.visibility= View.VISIBLE}
        copyButton.setOnClickListener{onCopyClicked()}
    }

    fun onCopyClicked()
    {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = ClipData.newPlainText("password", passwordTextView.text.toString());
        Toast.makeText(this,R.string.copied_to_clipboard,Toast.LENGTH_SHORT).show();
    }
}