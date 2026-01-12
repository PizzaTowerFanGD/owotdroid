package com.owot.android.client.network

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.Nullable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Background service for managing WebSocket connections
 * Keeps connections alive when app is in background
 */
@AndroidEntryPoint
class WebSocketService : Service() {
    
    @Inject
    lateinit var webSocketManager: WebSocketManager
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val ACTION_CONNECT = "connect"
        private const val ACTION_DISCONNECT = "disconnect"
        private const val EXTRA_WORLD_NAME = "world_name"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WebSocketService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "WebSocketService start command")
        
        when (intent?.action) {
            ACTION_CONNECT -> {
                val worldName = intent.getStringExtra(EXTRA_WORLD_NAME)
                if (worldName != null) {
                    connectToWorld(worldName)
                }
            }
            ACTION_DISCONNECT -> {
                disconnectFromWorld()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(@Nullable intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WebSocketService destroyed")
        serviceScope.cancel()
    }
    
    private fun connectToWorld(worldName: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Service connecting to world: $worldName")
                webSocketManager.connect(worldName)
            } catch (e: Exception) {
                Log.e(TAG, "Service connection failed", e)
            }
        }
    }
    
    private fun disconnectFromWorld() {
        try {
            Log.d(TAG, "Service disconnecting from world")
            webSocketManager.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Service disconnection failed", e)
        }
    }
    
    companion object {
        private const val TAG = "WebSocketService"
        
        fun startConnection(context: android.content.Context, worldName: String) {
            val intent = Intent(context, WebSocketService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_WORLD_NAME, worldName)
            }
            context.startService(intent)
        }
        
        fun stopConnection(context: android.content.Context) {
            val intent = Intent(context, WebSocketService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }
}