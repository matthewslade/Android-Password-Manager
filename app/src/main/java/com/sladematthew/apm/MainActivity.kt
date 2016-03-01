package com.sladematthew.apm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.sladematthew.apm.model.Password
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity: AppCompatActivity(),PasswordAdapter.OnItemClickListener
{
    override fun onClick(viewHolder: PasswordAdapter.ViewHolder, item: Password, position: Int) {

    }

    var authenticationManager:AuthenticationManager?=null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticationManager = AuthenticationManager(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        var adapter = PasswordAdapter(authenticationManager!!.getPasswordList(),this)
        recyclerView.adapter = adapter;
    }
}
