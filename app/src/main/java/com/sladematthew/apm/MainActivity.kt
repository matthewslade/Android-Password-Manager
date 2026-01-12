package com.sladematthew.apm


import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import com.sladematthew.apm.model.Password
import java.util.*
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.sladematthew.apm.databinding.ActivityMainBinding
import com.sladematthew.apm.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity: APMActivity(),PasswordAdapter.OnItemClickListener, TextWatcher
{
    private var adapter:PasswordAdapter?=null

    private var nextIntent: Intent? = null

    private val viewModel: MainViewModel by viewModels {
        (application as APMApplication).viewModelFactory
    }

    override fun afterTextChanged(s: Editable?) {
        adapter?.filter?.filter(s)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    override fun onLongClick(viewHolder: PasswordAdapter.ViewHolder, item: Password, position: Int) {
        nextIntent = Intent(this, EditPasswordActivity::class.java)
        nextIntent?.putExtra(Constants.IntentKey.PASSWORD,Gson().toJson(item))
        startActivity(nextIntent)
    }

    override fun onClick(viewHolder: PasswordAdapter.ViewHolder, item: Password, position: Int) {
        nextIntent = Intent(this, ViewPasswordActivity::class.java)
        nextIntent?.putExtra(Constants.IntentKey.PASSWORD, Gson().toJson(item))
        startActivity(nextIntent)
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticationManager?.createKey()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.searchBox.addTextChangedListener(this)
        binding.addPassword.setOnClickListener{startActivity(Intent(this, EditPasswordActivity::class.java))}

        setupObservers()
        loadPasswordList()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.passwordList.collect { passwords ->
                        adapter = PasswordAdapter(ArrayList(passwords), this@MainActivity)
                        binding.recyclerView.adapter = adapter
                        adapter?.onItemClickListener = this@MainActivity
                    }
                }

                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun loadPasswordList() {
        viewModel.loadPasswordList()
    }

    override fun onResume() {
        super.onResume()
        loadPasswordList()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, getString(R.string.authentication_failed), Toast.LENGTH_SHORT).show()
            return
        }

        if(requestCode == REQUESTCODE)
            startActivity(nextIntent)
    }
}
