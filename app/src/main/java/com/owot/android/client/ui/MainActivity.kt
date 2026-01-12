package com.owot.android.client.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import com.owot.android.client.R
import com.owot.android.client.ui.adapter.WorldListAdapter
import com.owot.android.client.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for world selection and navigation
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var viewModel: MainViewModel
    private lateinit var worldListAdapter: WorldListAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var addWorldFab: FloatingActionButton
    private lateinit var worldNameInput: TextInputEditText
    private lateinit var worldNameLayout: TextInputLayout
    private lateinit var connectButton: MaterialButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupViews()
        setupViewModel()
        setupRecyclerView()
        setupListeners()
        observeData()
    }
    
    private fun setupViews() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        recyclerView = findViewById(R.id.recycler_view_worlds)
        addWorldFab = findViewById(R.id.fab_add_world)
        worldNameInput = findViewById(R.id.input_world_name)
        worldNameLayout = findViewById(R.id.layout_world_name)
        connectButton = findViewById(R.id.button_connect)
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        worldListAdapter = WorldListAdapter { worldName ->
            // Navigate to world
            WorldActivity.start(this, worldName)
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = worldListAdapter
        }
        
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshWorlds()
        }
    }
    
    private fun setupListeners() {
        addWorldFab.setOnClickListener {
            // Show input for new world
            worldNameLayout.visibility = 
                if (worldNameLayout.visibility == android.view.View.GONE) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
        }
        
        connectButton.setOnClickListener {
            val worldName = worldNameInput.text?.toString()?.trim()
            if (worldName != null && worldName.isNotEmpty()) {
                WorldActivity.start(this, worldName)
                worldNameInput.text?.clear()
                worldNameLayout.visibility = android.view.View.GONE
            } else {
                worldNameInput.error = "Please enter a world name"
            }
        }
    }
    
    private fun observeData() {
        viewModel.worlds.observe(this) { worlds ->
            worldListAdapter.submitList(worlds)
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            swipeRefreshLayout.isRefreshing = isLoading
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.loadWorlds()
    }
}