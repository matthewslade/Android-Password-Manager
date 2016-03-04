package com.sladematthew.apm

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.widget.Toast
import com.sladematthew.apm.model.Password
import kotlinx.android.synthetic.main.activity_edit_password.*

class EditPasswordActivity : APMActivity()
{

    var password:Password?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_password)
        if(intent.hasExtra(Constants.IntentKey.PASSWORD))
            password = intent.getSerializableExtra(Constants.IntentKey.PASSWORD) as Password;
        updateButton.setOnClickListener{onUpdateButtonClicked()}
        deleteButton.setOnClickListener{onDeleteClicked()}
        plus.setOnClickListener{onPlusClicked()}
        if(password!=null)
        {
            labelTextView.setText(password!!.label.toString())
            usernameTextView.setText(password!!.username.toString())
            lengthTextView.setText(password!!.length.toString())
            prefixTextView.setText(password!!.prefix.toString())
            versionTextView.setText(password!!.version.toString())
            passwordTextView.text = authenticationManager!!.generatePassword(password!!)
        }
        else
        {
            prefixTextView.setText(Constants.Misc.DEFAULT_PREFIX)
            lengthTextView.setText(Constants.Misc.DEFAULT_LENGTH.toString())
            versionTextView.setText(1.toString())
        }
    }

    fun onPlusClicked()
    {
        versionTextView.setText((versionTextView.text.toString().toInt()+1).toString())
    }

    fun onDeleteClicked()
    {
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(android.R.string.ok,{dialogInterface, i -> deletePassword()})
                .setNegativeButton(android.R.string.cancel,{dialogInterface, i -> dialogInterface.dismiss()})
                .show()
    }

    fun deletePassword()
    {
        authenticationManager!!.deletePassword(password!!,{finish()})
    }

    fun onUpdateButtonClicked()
    {
        var version = versionTextView.text.toString().toInt()
        var length = lengthTextView.text.toString().toInt()

        if(version !in 1..9999)
        {
            Toast.makeText(this,R.string.error_version,Toast.LENGTH_LONG).show()
            return
        }

        if(length !in 4..24)
        {
            Toast.makeText(this,R.string.error_length,Toast.LENGTH_LONG).show()
            return
        }

        password = Password(labelTextView.text.toString().toLowerCase().trim().replace(" ",""),version,length,prefixTextView.text.toString(),usernameTextView.text.toString())
        passwordTextView.text = authenticationManager!!.generatePassword(password!!)
        authenticationManager!!.addorUpdatePassword(password!!,{})
    }


}
