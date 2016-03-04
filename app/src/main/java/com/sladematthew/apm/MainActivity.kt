package com.sladematthew.apm

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import com.sladematthew.apm.model.Password
import com.sladematthew.apm.model.PasswordList
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity: APMActivity(),PasswordAdapter.OnItemClickListener
{
    override fun onLongClick(viewHolder: PasswordAdapter.ViewHolder, item: Password, position: Int) {
        var intent = Intent(this, EditPasswordActivity::class.java)
        intent.putExtra(Constants.IntentKey.PASSWORD,item)
        startActivity(intent)
    }

    override fun onClick(viewHolder: PasswordAdapter.ViewHolder, item: Password, position: Int) {
        var intent = Intent(this, ViewPasswordActivity::class.java)
        intent.putExtra(Constants.IntentKey.PASSWORD,item)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(DividerDecoration(this))
        addPassword.setOnClickListener{startActivity(Intent(this, EditPasswordActivity::class.java))}
        loadPasswordList()
    }

    fun loadPasswordList()
    {
        fun callback()
        {
            var adapter = PasswordAdapter(ArrayList(authenticationManager!!.passwordList!!.passwords.values),this)
            recyclerView.adapter = adapter;
            adapter.onItemClickListener = this;
        }
        authenticationManager!!.loadPasswordList(::callback)
    }

    override fun onResume() {
        super.onResume()
        loadPasswordList()
    }
}
