package com.sladematthew.apm

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.sladematthew.apm.model.Password
import kotlinx.android.synthetic.main.activity_edit_password.*

class EditPasswordActivity : APMActivity(), View.OnClickListener
{

    var password:Password?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.title_edit_password)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_edit_password)
        if(intent.hasExtra(Constants.IntentKey.PASSWORD))
            password = intent.getSerializableExtra(Constants.IntentKey.PASSWORD) as Password
        updateButton.setOnClickListener{onUpdateButtonClicked()}
        deleteButton.setOnClickListener{onDeleteClicked()}
        plus.setOnClickListener{onPlusClicked()}
        passwordTextView.setOnClickListener(this)
        if(password!=null) {
            labelTextView.setText(password!!.label)
            usernameTextView.setText(password!!.username)
            lengthTextView.setText(password!!.length.toString())
            prefixTextView.setText(password!!.prefix)
            versionTextView.setText(password!!.version.toString())
            passwordTextView.text = authenticationManager!!.generatePassword2(password!!)
        } else {
            prefixTextView.setText(Constants.Misc.DEFAULT_PREFIX)
            lengthTextView.setText(Constants.Misc.DEFAULT_LENGTH.toString())
            versionTextView.setText(1.toString())
        }
    }

    private fun onPlusClicked() {
        versionTextView.setText((versionTextView.text.toString().toInt()+1).toString())
    }

    private fun onDeleteClicked() {
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(android.R.string.ok) { _, _ -> deletePassword()}
                .setNegativeButton(android.R.string.cancel) { dialogInterface, _ -> dialogInterface.dismiss()}
                .show()
    }

    private fun deletePassword() {
        authenticationManager!!.deletePassword(password!!) {finish()}
    }

    private fun onUpdateButtonClicked() {
        val version = versionTextView.text.toString().toInt()
        val length = lengthTextView.text.toString().toInt()

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

        password = Password(Constants.Misc.ALGORITHM,labelTextView.text.toString().toLowerCase().trim().replace(" ",""),version,length,prefixTextView.text.toString(),usernameTextView.text.toString())
        passwordTextView.text = authenticationManager!!.generatePassword2(password!!)
        authenticationManager!!.addOrUpdatePassword(password!!)
    }

    override fun onClick(p0: View?) {
        if (p0 is TextView) {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = ClipData.newPlainText("password", p0.text.toString());
            Toast.makeText(this, getString(R.string.copied_to_clipboard,p0.text.toString()), Toast.LENGTH_SHORT).show()
        }
    }

}
