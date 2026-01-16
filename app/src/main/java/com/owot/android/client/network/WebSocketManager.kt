package com.owot.android.client.network

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.owot.android.client.BuildConfig
import com.owot.android.client.data.models.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean
import java.net.URI
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.SSLContext
import java.security.KeyStore
import java.io.IOException

/**
 * Manages WebSocket connections and message handling for OWOT client
 */
class WebSocketManager(private val context: Context) {
    
    companion object {
        private const val TAG = "WebSocketManager"
        private const val CONNECT_TIMEOUT = 10000L
        private const val WRITE_TIMEOUT = 10000L
        private const val READ_TIMEOUT = 0L // 0 means no read timeout
        private const val PING_INTERVAL = 30000L // 30 seconds
        private const val RECONNECT_DELAY = 2000L // 2 seconds
        private const val MAX_RECONNECT_ATTEMPTS = 10
    }
    
    private val gson = Gson()
    private val clientIdCounter = AtomicInteger(1)
    private val lastPingId = AtomicLong(0)
    private val reconnectAttempts = AtomicInteger(0)
    
    // Connection state
    private val isConnected = AtomicBoolean(false)
    private val isConnecting = AtomicBoolean(false)
    private val currentWorld = AtomicReference<String?>(null)
    private val clientId = AtomicReference<String?>(null)
    private val socketChannel = AtomicReference<String?>(null)
    
    // WebSocket components
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var currentRequest: Request? = null
    
    // Callbacks and listeners
    var onMessageListener: ((WSMessage) -> Unit)? = null
    var onConnectionStateListener: ((Boolean, Boolean) -> Unit)? = null // (isConnected, isConnecting)
    var onErrorListener: ((String) -> Unit)? = null
    
    // Coroutines
    private val supervisorJob = SupervisorJob()
    private val networkScope = CoroutineScope(Dispatchers.IO + supervisorJob)
    private val pingJob = AtomicReference<Job?>(null)
    private val reconnectJob = AtomicReference<Job?>(null)
    
    /**
     * Initialize the WebSocket client with proper SSL configuration
     */
    private fun initializeClient() {
        try {
            // Create custom trust manager that accepts self-signed certificates
            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(null as KeyStore?)
            val trustManagers = trustManagerFactory.trustManagers
            if (trustManagers.size != 1 || trustManagers[0] !is X509TrustManager) {
                throw IllegalStateException("Unexpected default trust managers: ${trustManagers.contentToString()}")
            }
            val trustManager = trustManagers[0] as X509TrustManager
            
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(trustManager), null)
            val sslSocketFactory = sslContext.socketFactory
            
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
            
            client = OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
                .writeTimeout(WRITE_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
                .addInterceptor(loggingInterceptor)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .hostnameVerifier { _, _ -> true } // Accept all hostnames for testing
                .build()
                
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WebSocket client", e)
            onErrorListener?.invoke("Failed to initialize WebSocket client: ${e.message}")
        }
    }
    
    /**
     * Connect to a world WebSocket
     */
    suspend fun connect(worldName: String): Boolean {
        if (isConnected.get() || isConnecting.get()) {
            Log.w(TAG, "Already connected or connecting")
            return false
        }
        
        currentWorld.set(worldName)
        isConnecting.set(true)
        onConnectionStateListener?.invoke(false, true)
        
        try {
            initializeClient()
            
            val wsUrl = buildWsUrl(worldName)
            val request = Request.Builder()
                .url(wsUrl)
                .header("User-Agent", "OWOT-Android-Client/1.0")
                .build()
            currentRequest = request
            
            withContext(Dispatchers.IO) {
                client?.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        Log.d(TAG, "WebSocket connected to $worldName")
                        this@WebSocketManager.webSocket = webSocket
                        isConnected.set(true)
                        isConnecting.set(false)
                        reconnectAttempts.set(0)
                        onConnectionStateListener?.invoke(true, false)
                        
                        // Start ping job
                        startPingJob()
                        
                        // Request chat history on connection
                        sendMessage(ChatHistoryMessage())
                    }
                    
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        handleMessage(text)
                    }
                    
                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.e(TAG, "WebSocket connection failed", t)
                        handleConnectionFailure(t.message ?: "Unknown error")
                    }
                    
                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "WebSocket closed: $code - $reason")
                        handleConnectionClosed()
                    }
                })
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to world $worldName", e)
            isConnecting.set(false)
            onConnectionStateListener?.invoke(false, false)
            onErrorListener?.invoke("Failed to connect: ${e.message}")
            return false
        }
    }
    
    /**
     * Disconnect from the current world
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket")
        
        // Stop ping job
        pingJob.get()?.cancel()
        reconnectJob.get()?.cancel()
        
        // Close WebSocket
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        
        // Reset state
        isConnected.set(false)
        isConnecting.set(false)
        clientId.set(null)
        socketChannel.set(null)
        currentWorld.set(null)
        
        onConnectionStateListener?.invoke(false, false)
    }
    
    /**
     * Send a message to the server
     */
    fun sendMessage(message: WSMessage) {
        if (!isConnected.get()) {
            Log.w(TAG, "Cannot send message: not connected")
            return
        }
        
        try {
            val json = gson.toJson(message)
            Log.d(TAG, "Sending message: ${message.kind}")
            
            val sent = webSocket?.send(json)
            if (sent == false) {
                Log.w(TAG, "Failed to send message")
                // Connection might be broken, try to reconnect
                handleConnectionFailure("Failed to send message")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            onErrorListener?.invoke("Failed to send message: ${e.message}")
        }
    }
    
    /**
     * Handle incoming WebSocket messages
     */
    private fun handleMessage(json: String) {
        try {
            // First parse as a generic map to determine the message kind
            val jsonObject = gson.fromJson(json, Map::class.java)
            val kind = jsonObject["kind"] as? String
            
            when (kind) {
                "channel" -> handleChannelResponse(gson.fromJson(json, ChannelResponse::class.java))
                "ping" -> handlePing(gson.fromJson(json, PongMessage::class.java))
                "announcement" -> handleAnnouncement(gson.fromJson(json, AnnouncementMessage::class.java))
                "propUpdate" -> handlePropertyUpdate(gson.fromJson(json, PropertyUpdateMessage::class.java))
                "user_count" -> handleUserCount(gson.fromJson(json, UserCountMessage::class.java))
                "error" -> handleError(gson.fromJson(json, ErrorMessage::class.java))
                "tileUpdate" -> handleTileUpdate(gson.fromJson(json, TileUpdateMessage::class.java))
                "fetch" -> handleFetchResponse(gson.fromJson(json, FetchResponse::class.java))
                "write" -> handleWriteResponse(gson.fromJson(json, WriteResponse::class.java))
                "chat" -> handleChatMessage(gson.fromJson(json, ChatResponse::class.java))
                "chathistory" -> handleChatHistory(gson.fromJson(json, ChatHistoryResponse::class.java))
                "cursor" -> handleCursorMessage(gson.fromJson(json, CursorMessage::class.java))
                else -> {
                    Log.w(TAG, "Unknown message kind: $kind")
                    onErrorListener?.invoke("Unknown message type: $kind")
                }
            }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse message: $json", e)
            onErrorListener?.invoke("Invalid message format: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
            onErrorListener?.invoke("Error processing message: ${e.message}")
        }
    }
    
    /**
     * Handle channel assignment from server
     */
    private fun handleChannelResponse(response: ChannelResponse) {
        clientId.set(response.clientId)
        socketChannel.set(response.socketChannel)
        Log.d(TAG, "Assigned client ID: ${response.clientId}")
    }
    
    /**
     * Handle ping/pong messages
     */
    private fun handlePing(pong: PongMessage) {
        val currentTime = System.currentTimeMillis()
        val pingId = pong.id?.toLongOrNull() ?: return
        
        if (pingId == lastPingId.get()) {
            val pingTime = currentTime - pingId
            Log.d(TAG, "Ping: ${pingTime}ms")
            // Update ping time in client state
            // This would be handled by the calling component
        }
    }
    
    /**
     * Handle server announcements
     */
    private fun handleAnnouncement(announcement: AnnouncementMessage) {
        Log.d(TAG, "Server announcement: ${announcement.message}")
        // Forward to UI or handle as needed
    }
    
    /**
     * Handle property updates from server
     */
    private fun handlePropertyUpdate(update: PropertyUpdateMessage) {
        Log.d(TAG, "Property update received")
        onMessageListener?.invoke(update)
    }
    
    /**
     * Handle user count updates
     */
    private fun handleUserCount(countMessage: UserCountMessage) {
        Log.d(TAG, "User count: ${countMessage.count}")
        onMessageListener?.invoke(countMessage)
    }
    
    /**
     * Handle error messages from server
     */
    private fun handleError(error: ErrorMessage) {
        Log.e(TAG, "Server error: ${error.code} - ${error.message}")
        onErrorListener?.invoke("Server error: ${error.message}")
        
        // Handle specific error codes
        when (error.code) {
            "CONN_LIMIT" -> {
                onErrorListener?.invoke("Server connection limit reached. Please try again later.")
            }
            "NO_PERM" -> {
                onErrorListener?.invoke("You don't have permission to perform this action.")
            }
            else -> {
                onErrorListener?.invoke("Error: ${error.message}")
            }
        }
    }
    
    /**
     * Handle tile updates from server
     */
    private fun handleTileUpdate(update: TileUpdateMessage) {
        Log.d(TAG, "Tile update: ${update.tileX},${update.tileY} -> ${update.character}")
        onMessageListener?.invoke(update)
    }
    
    /**
     * Handle fetch response from server
     */
    private fun handleFetchResponse(response: FetchResponse) {
        Log.d(TAG, "Fetch response received with ${response.tiles.size} tiles")
        onMessageListener?.invoke(response)
    }
    
    /**
     * Handle write response from server
     */
    private fun handleWriteResponse(response: WriteResponse) {
        Log.d(TAG, "Write response: ${response.accepted.size} accepted, ${response.rejected.size} rejected")
        onMessageListener?.invoke(response)
    }
    
    /**
     * Handle chat message from server
     */
    private fun handleChatMessage(chatMessage: ChatResponse) {
        Log.d(TAG, "Chat message from ${chatMessage.nickname}: ${chatMessage.message}")
        onMessageListener?.invoke(chatMessage)
    }
    
    /**
     * Handle chat history response from server
     */
    private fun handleChatHistory(historyResponse: ChatHistoryResponse) {
        Log.d(TAG, "Chat history: ${historyResponse.globalChatPrev.size} global, ${historyResponse.pageChatPrev.size} page messages")
        onMessageListener?.invoke(historyResponse)
    }
    
    /**
     * Handle cursor message from server
     */
    private fun handleCursorMessage(cursorMessage: CursorMessage) {
        Log.d(TAG, "Cursor update received")
        onMessageListener?.invoke(cursorMessage)
    }
    
    /**
     * Handle connection failure
     */
    private fun handleConnectionFailure(reason: String) {
        Log.w(TAG, "Connection failed: $reason")
        
        isConnected.set(false)
        isConnecting.set(false)
        onConnectionStateListener?.invoke(false, false)
        
        // Attempt to reconnect
        if (reconnectAttempts.get() < MAX_RECONNECT_ATTEMPTS) {
            scheduleReconnect()
        } else {
            onErrorListener?.invoke("Failed to reconnect after ${MAX_RECONNECT_ATTEMPTS} attempts")
        }
    }
    
    /**
     * Handle connection closure
     */
    private fun handleConnectionClosed() {
        Log.d(TAG, "Connection closed")
        
        isConnected.set(false)
        isConnecting.set(false)
        onConnectionStateListener?.invoke(false, false)
        
        // Stop ping job
        pingJob.get()?.cancel()
        
        // Attempt to reconnect if we were previously connected
        if (currentWorld.get() != null && reconnectAttempts.get() < MAX_RECONNECT_ATTEMPTS) {
            scheduleReconnect()
        }
    }
    
    /**
     * Schedule a reconnect attempt
     */
    private fun scheduleReconnect() {
        reconnectJob.get()?.cancel()
        
        val attempt = reconnectAttempts.incrementAndGet()
        val delay = RECONNECT_DELAY * attempt // Exponential backoff
        
        Log.d(TAG, "Scheduling reconnect attempt $attempt in ${delay}ms")
        
        val job = networkScope.launch {
            delay(delay)
            val worldName = currentWorld.get()
            if (worldName != null) {
                Log.d(TAG, "Attempting to reconnect...")
                connect(worldName)
            }
        }
        reconnectJob.set(job)
    }
    
    /**
     * Start ping job to keep connection alive
     */
    private fun startPingJob() {
        pingJob.get()?.cancel()
        
        val job = networkScope.launch {
            while (isConnected.get()) {
                try {
                    val pingId = System.currentTimeMillis()
                    lastPingId.set(pingId)
                    sendMessage(PingMessage(pingId.toString()))
                    delay(PING_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Ping job error", e)
                    break
                }
            }
        }
        pingJob.set(job)
    }
    
    /**
     * Build WebSocket URL for a world
     */
    private fun buildWsUrl(worldName: String): String {
        val baseUrl = "wss://ourworldoftext.com"
        return "$baseUrl/$worldName/ws/"
    }
    
    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean = isConnected.get()
    
    /**
     * Check if currently connecting
     */
    fun isConnecting(): Boolean = isConnecting.get()
    
    /**
     * Get current client ID
     */
    fun getClientId(): String? = clientId.get()
    
    /**
     * Get current socket channel
     */
    fun getSocketChannel(): String? = socketChannel.get()
    
    /**
     * Generate unique edit ID for write operations
     */
    fun generateEditId(): String {
        return "android_${clientIdCounter.getAndIncrement()}_${System.currentTimeMillis()}"
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        disconnect()
        client?.dispatcher?.executorService?.shutdown()
        networkScope.cancel()
        supervisorJob.cancel()
    }
}