package com.owot.android.client

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.owot.android.client.data.AppDatabase
import com.owot.android.client.network.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class OWOTApplication : Application() {
    
    // Database for local storage
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "owot_database"
        ).fallbackToDestructiveMigration().build()
    }
    
    // Global coroutine scope
    val applicationScope = CoroutineScope(SupervisorJob())
    
    // WebSocket manager for network operations
    lateinit var webSocketManager: WebSocketManager
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize WebSocket manager
        webSocketManager = WebSocketManager(this)
    }
    
    companion object {
        lateinit var instance: OWOTApplication
            private set
            
        fun getContext(): Context = instance.applicationContext
    }
}