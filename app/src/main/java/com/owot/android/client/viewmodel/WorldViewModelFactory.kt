package com.owot.android.client.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.owot.android.client.OWOTApplication
import com.owot.android.client.network.WebSocketManager

/**
 * Factory for creating WorldViewModel instances
 */
class WorldViewModelFactory(
    private val worldName: String,
    private val webSocketManager: WebSocketManager,
    private val application: Application
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorldViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorldViewModel(worldName, webSocketManager, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}