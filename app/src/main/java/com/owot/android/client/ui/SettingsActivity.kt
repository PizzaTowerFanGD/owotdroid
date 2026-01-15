package com.owot.android.client.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.owot.android.client.R
import com.owot.android.client.manager.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Settings activity for configuring user preferences and app settings
 */
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    
    @Inject
    lateinit var preferenceManager: PreferenceManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        setupActionBar()
        loadSettings()
    }
    
    private fun setupActionBar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_activity_settings)
        }
    }
    
    private fun loadSettings() {
        // Load current preferences into UI
        val preferences = preferenceManager.getUserPreferences()
        
        // This would populate the settings UI with current values
        // Implementation would populate EditText fields, switches, etc.
        // TODO: Use the loaded preferences to populate the UI
    }
    
    private fun saveSettings() {
        // This would read values from UI and save them
        // Implementation would read from EditText fields, switches, etc.
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_save -> {
                saveSettings()
                finish()
                true
            }
            R.id.action_reset -> {
                resetToDefaults()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun resetToDefaults() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Settings")
            .setMessage("Are you sure you want to reset all settings to defaults?")
            .setPositiveButton("Reset") { _, _ ->
                preferenceManager.resetToDefaults()
                loadSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}