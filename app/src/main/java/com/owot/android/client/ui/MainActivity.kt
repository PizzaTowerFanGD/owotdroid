package com.owot.android.client.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import com.owot.android.client.R
import com.owot.android.client.manager.PreferenceManager
import com.owot.android.client.ui.adapter.WorldListAdapter
import com.owot.android.client.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity for world selection and navigation
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    @Inject
    lateinit var preferenceManager: PreferenceManager
    
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
        
        // Handle deep links
        handleIntent(intent)
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
            // Show/hide input for new world
            val isVisible = worldNameLayout.visibility == android.view.View.VISIBLE
            worldNameLayout.visibility = if (isVisible) android.view.View.GONE else android.view.View.VISIBLE
            
            if (!isVisible) {
                worldNameInput.requestFocus()
            }
        }
        
        connectButton.setOnClickListener {
            val worldName = worldNameInput.text?.toString()?.trim()
            if (worldName != null && worldName.isNotEmpty()) {
                connectToWorld(worldName)
                worldNameInput.text?.clear()
                worldNameLayout.visibility = android.view.View.GONE
            } else {
                worldNameInput.error = "Please enter a world name"
            }
        }
        
        worldNameInput.setOnEditorActionListener { _, _, _ ->
            connectButton.performClick()
            true
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
    
    private fun connectToWorld(worldName: String) {
        // Save last world
        preferenceManager.setLastWorld(worldName)
        
        // Navigate to world
        WorldActivity.start(this, worldName)
    }
    
    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val data = intent.data
                if (data?.scheme == "owot") {
                    val worldName = data.getQueryParameter("world")
                    if (worldName != null) {
                        WorldActivity.start(this, worldName)
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.loadWorlds()
        
        // Auto-connect to last world if enabled
        if (preferenceManager.getAutoConnect()) {
            val lastWorld = preferenceManager.getLastWorld()
            if (lastWorld != null) {
                WorldActivity.start(this, lastWorld)
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                SettingsActivity.start(this)
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            R.id.action_exit -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("OWOT Android Client")
            .setMessage("Version 1.0\n\nA feature-complete native Android client for Our World of Text.\n\nDeveloped with ❤️ for the OWOT community.")
            .setPositiveButton("OK", null)
            .show()
    }
}