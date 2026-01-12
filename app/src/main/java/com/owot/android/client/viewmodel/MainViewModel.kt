package com.owot.android.client.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.owot.android.client.OWOTApplication
import com.owot.android.client.data.models.WorldInfo
import com.owot.android.client.data.models.WorldListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the main activity showing world list
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val application = getApplication<OWOTApplication>()
    
    private val _worlds = MutableLiveData<List<WorldListItem>>()
    val worlds: LiveData<List<WorldListItem>> = _worlds
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    /**
     * Load worlds from database
     */
    fun loadWorlds() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val worlds = withContext(Dispatchers.IO) {
                    // In a real implementation, this would load from database
                    // For now, return some example worlds
                    listOf(
                        WorldListItem("test", "Test World", "A test world", 5, System.currentTimeMillis()),
                        WorldListItem("main", "Main World", "The main OWOT world", 42, System.currentTimeMillis()),
                        WorldListItem("sandbox", "Sandbox", "Experimental world", 2, System.currentTimeMillis())
                    )
                }
                
                _worlds.value = worlds
            } catch (e: Exception) {
                _error.value = "Failed to load worlds: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh worlds from server
     */
    fun refreshWorlds() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // In a real implementation, this would fetch from server
                loadWorlds()
                
            } catch (e: Exception) {
                _error.value = "Failed to refresh worlds: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Add a new world
     */
    fun addWorld(worldName: String) {
        viewModelScope.launch {
            try {
                // Validate world name
                if (worldName.isBlank()) {
                    _error.value = "World name cannot be empty"
                    return@launch
                }
                
                // Check if world already exists
                val currentWorlds = _worlds.value ?: emptyList()
                if (currentWorlds.any { it.name.equals(worldName, ignoreCase = true) }) {
                    _error.value = "World already exists"
                    return@launch
                }
                
                // Add to list (in real implementation, would save to database)
                val newWorld = WorldListItem(
                    name = worldName,
                    title = worldName,
                    description = "",
                    userCount = 0,
                    lastVisited = System.currentTimeMillis()
                )
                
                val updatedWorlds = currentWorlds + newWorld
                _worlds.value = updatedWorlds
                
            } catch (e: Exception) {
                _error.value = "Failed to add world: ${e.message}"
            }
        }
    }
    
    /**
     * Remove a world
     */
    fun removeWorld(worldName: String) {
        viewModelScope.launch {
            try {
                val currentWorlds = _worlds.value ?: emptyList()
                val updatedWorlds = currentWorlds.filterNot { it.name == worldName }
                _worlds.value = updatedWorlds
                
                // In real implementation, would remove from database
                
            } catch (e: Exception) {
                _error.value = "Failed to remove world: ${e.message}"
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cleanup if needed
    }
}