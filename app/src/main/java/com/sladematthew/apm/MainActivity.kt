package com.sladematthew.apm

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.sladematthew.apm.model.Password
import com.sladematthew.apm.model.PasswordList
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity: APMActivity(),PasswordAdapter.OnItemClickListener
{
    override fun onClick(viewHolder: PasswordAdapter.ViewHolder, item: Password, position: Int) {
        var intent = Intent(this, PasswordActivity::class.java)
        intent.putExtra(Constants.IntentKey.PASSWORD,item)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView.layoutManager = LinearLayoutManager(this)
        addPassword.setOnClickListener{startActivity(Intent(this, PasswordActivity::class.java))}
        fun callback(passwordList: PasswordList)
        {
            var adapter = PasswordAdapter(passwordList,this)
            recyclerView.adapter = adapter;
            adapter.onItemClickListener = this;
        }
        authenticationManager!!.getPasswordList(::callback)
    }

    
}
