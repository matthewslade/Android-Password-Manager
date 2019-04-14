package com.sladematthew.apm


import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import com.sladematthew.apm.model.Password
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import android.support.v7.widget.DividerItemDecoration
import android.widget.Toast

class MainActivity: APMActivity(),PasswordAdapter.OnItemClickListener, TextWatcher
{
    private var adapter:PasswordAdapter?=null

    private var nextIntent: Intent? = null

    override fun afterTextChanged(s: Editable?) {
        adapter?.filter?.filter(s)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    override fun onLongClick(viewHolder: PasswordAdapter.ViewHolder, item: Password, position: Int) {
        nextIntent = Intent(this, EditPasswordActivity::class.java)
        nextIntent?.putExtra(Constants.IntentKey.PASSWORD,item)
        if(authenticationManager!!.isAuthenticated())
            startActivity(nextIntent)
        else
            showAuthenticationScreen()
    }

    override fun onClick(viewHolder: PasswordAdapter.ViewHolder, item: Password, position: Int) {
        nextIntent = Intent(this, ViewPasswordActivity::class.java)
        nextIntent?.putExtra(Constants.IntentKey.PASSWORD,item)
        if(authenticationManager!!.isAuthenticated())
            startActivity(nextIntent)
        else
            showAuthenticationScreen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticationManager?.createKey()
        setContentView(R.layout.activity_main)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        searchBox.addTextChangedListener(this)
        addPassword.setOnClickListener{startActivity(Intent(this, EditPasswordActivity::class.java))}
        loadPasswordList()
    }

    private fun loadPasswordList() {
        fun callback()
        {
            adapter = PasswordAdapter(ArrayList(authenticationManager!!.passwordList!!.passwords.values),this)
            recyclerView.adapter = adapter;
            adapter?.onItemClickListener = this;
        }
        authenticationManager?.loadPasswordList(::callback)
    }

    override fun onResume() {
        super.onResume()
        loadPasswordList()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, getString(R.string.authentication_failed), Toast.LENGTH_SHORT).show()
            return
        }

        if(requestCode == REQUESTCODE)
            startActivity(nextIntent)
    }
}
