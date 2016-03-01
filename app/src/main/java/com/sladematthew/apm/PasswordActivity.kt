package com.sladematthew.apm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.sladematthew.apm.model.Password
import kotlinx.android.synthetic.main.activity_password.*

class PasswordActivity : APMActivity()
{

    var password:Password?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password)
        if(intent.hasExtra(Constants.IntentKey.PASSWORD))
            password = intent.getSerializableExtra(Constants.IntentKey.PASSWORD) as Password;
        updateButton.setOnClickListener{onUpdateButtonClicked()}
        if(password!=null)
        {
            labelTextView.setText(password!!.label.toString())
            versionTextView.setText(password!!.version.toString())
            passwordTextView.text = authenticationManager!!.generatePassword(password!!)
        }
    }

    fun onUpdateButtonClicked()
    {
        password = Password(labelTextView.text.toString(),versionTextView.text.toString().toInt())
        passwordTextView.text = authenticationManager!!.generatePassword(password!!)
        authenticationManager!!.addorUpdatePassword(password!!)
    }
}
