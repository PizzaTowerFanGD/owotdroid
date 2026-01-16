package com.owot.android.client.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.owot.android.client.data.models.*
import com.owot.android.client.network.WebSocketManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the world activity handling all world interaction
 */
class WorldViewModel(
    val worldName: String, // Make it a public val in the constructor
    private val webSocketManager: WebSocketManager,
    application: Application
) : AndroidViewModel(application) {
    
    // Network state
    private val _clientState = MutableLiveData<ClientState>()
    val clientState: LiveData<ClientState> = _clientState
    
    // World information
    private val _worldInfo = MutableLiveData<WorldInfo>()
    val worldInfo: LiveData<WorldInfo> = _worldInfo
    
    // Chat messages
    private val _chatMessages = MutableLiveData<List<ChatResponse>>()
    val chatMessages: LiveData<List<ChatResponse>> = _chatMessages
    
    // Error handling
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Announcements
    private val _announcements = MutableLiveData<String?>()
    val announcements: LiveData<String?> = _announcements
    
    // Tiles data
    private val _tiles = MutableStateFlow<Map<String, Tile>>(emptyMap())
    val tiles: StateFlow<Map<String, Tile>> = _tiles.asStateFlow()
    
    // Guest cursors
    private val _guestCursors = MutableStateFlow<Map<String, CursorPosition>>(emptyMap())
    val guestCursors: StateFlow<Map<String, CursorPosition>> = _guestCursors.asStateFlow()
    
    // Local cursor
    private val _localCursor = MutableLiveData<CursorPosition?>()
    val localCursor: LiveData<CursorPosition?> = _localCursor
    
    // Camera position
    private val _cameraPosition = MutableStateFlow(Pair(0f, 0f))
    val cameraPosition: StateFlow<Pair<Float, Float>> = _cameraPosition.asStateFlow()
    
    private val _zoom = MutableStateFlow(1.0f)
    val zoom: StateFlow<Float> = _zoom.asStateFlow()
    
    // User preferences
    private val userPreferences = MutableLiveData<UserPreferences>()
    
    // Write buffer for batching edits
    private val writeBuffer = mutableListOf<EditData>()
    private val linkQueue = mutableListOf<LinkData>()
    
    // Render queue for dirty tiles
    private val renderQueue = mutableSetOf<String>()
    
    init {
        setupWebSocketManager()
        initializeUserPreferences()
        initializeClientState()
    }
    
    /**
     * Setup WebSocket manager callbacks
     */
    private fun setupWebSocketManager() {
        webSocketManager.onMessageListener = { message ->
            handleWebSocketMessage(message)
        }
        
        webSocketManager.onConnectionStateListener = { isConnected, isConnecting ->
            _clientState.postValue(_clientState.value?.copy(
                isConnected = isConnected,
                isConnecting = isConnecting
            ) ?: ClientState(isConnected = isConnected, isConnecting = isConnecting))
            
            if (isConnected) {
                // Send initial fetch request
                fetchInitialTiles()
            }
        }
        
        webSocketManager.onErrorListener = { error ->
            _error.postValue(error)
        }
    }
    
    /**
     * Initialize user preferences
     */
    private fun initializeUserPreferences() {
        viewModelScope.launch {
            try {
                val preferences = UserPreferences(
                    nickname = "Anonymous",
                    textColor = android.graphics.Color.BLACK,
                    bgColor = -1,
                    showGrid = true,
                    showCursors = true,
                    autoScroll = true,
                    chatEnabled = true
                )
                userPreferences.postValue(preferences)
            } catch (e: Exception) {
                _error.postValue("Failed to load preferences: ${e.message}")
            }
        }
    }
    
    /**
     * Initialize client state
     */
    private fun initializeClientState() {
        _clientState.value = ClientState(
            isConnected = false,
            isConnecting = false,
            userCount = 0,
            ping = 0
        )
    }
    
    /**
     * Connect to the world
     */
    suspend fun connect(): Boolean {
        return try {
            val success = webSocketManager.connect(worldName)
            if (success) {
                // Update world info
                _worldInfo.postValue(WorldInfo(
                    name = worldName,
                    title = worldName,
                    description = "Connected to $worldName",
                    userCount = 0,
                    isMember = false,
                    canWrite = true
                ))
            }
            success
        } catch (e: Exception) {
            _error.postValue("Connection failed: ${e.message}")
            false
        }
    }
    
    /**
     * Disconnect from the world
     */
    fun disconnect() {
        webSocketManager.disconnect()
        writeBuffer.clear()
        linkQueue.clear()
        renderQueue.clear()
        _tiles.value = emptyMap()
        _guestCursors.value = emptyMap()
        _localCursor.value = null
    }
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean = _clientState.value?.isConnected == true
    
    /**
     * Handle WebSocket messages
     */
    private fun handleWebSocketMessage(message: WSMessage) {
        viewModelScope.launch {
            when (message.kind) {
                "fetch" -> handleFetchResponse(message as FetchResponse)
                "write" -> handleWriteResponse(message as WriteResponse)
                "chat" -> handleChatMessage(message as ChatResponse)
                "chathistory" -> handleChatHistory(message as ChatHistoryResponse)
                "tileUpdate" -> handleTileUpdate(message as TileUpdateMessage)
                "cursor" -> handleCursorUpdate(message as CursorMessage)
                "propUpdate" -> handlePropertyUpdate(message as PropertyUpdateMessage)
                "user_count" -> handleUserCount(message as UserCountMessage)
                "announcement" -> handleAnnouncement(message as AnnouncementMessage)
                "error" -> handleError(message as ErrorMessage)
            }
        }
    }
    
    /**
     * Handle fetch response
     */
    private fun handleFetchResponse(response: FetchResponse) {
        val currentTiles = _tiles.value.toMutableMap()
        response.tiles.forEach { (key, serverTile) ->
            val parts = key.split(",")
            if (parts.size == 2) {
                val tileY = parts[0].toInt()
                val tileX = parts[1].toInt()
                
                // Parse tile content and properties using TileParser
                val content = TileParser.parseContent(serverTile.content)
                val colors = TileParser.parseColorArray(serverTile.properties.color)
                val bgColors = TileParser.parseBgColorArray(serverTile.properties.bgcolor)
                val cellProps = TileParser.parseCellProps(serverTile.properties.cellProps)
                
                val tile = Tile(
                    tileX = tileX,
                    tileY = tileY,
                    content = content,
                    properties = TileProperties(
                        writability = serverTile.properties.writability ?: TileProperties.WRITABILITY_PUBLIC,
                        color = colors,
                        bgColor = bgColors,
                        cellProps = cellProps
                    )
                )
                
                currentTiles["$tileX,$tileY"] = tile
            }
        }
        _tiles.value = currentTiles
    }
    
    /**
     * Handle write response
     */
    private fun handleWriteResponse(response: WriteResponse) {
        response.rejected.forEach { (_, reason) ->
            _error.postValue("Edit rejected: $reason")
        }
        
        // Process link queue for accepted edits
        response.accepted.forEach { _ ->
            // Process pending links for this edit
        }
    }
    
    /**
     * Handle chat message
     */
    private fun handleChatMessage(chatResponse: ChatResponse) {
        val currentMessages = _chatMessages.value ?: emptyList()
        val updatedMessages = currentMessages + chatResponse
        _chatMessages.postValue(updatedMessages)
    }
    
    /**
     * Handle chat history
     */
    private fun handleChatHistory(history: ChatHistoryResponse) {
        val allMessages = history.globalChatPrev + history.pageChatPrev
        _chatMessages.postValue(allMessages.sortedBy { it.date })
    }
    
    /**
     * Handle tile update from server
     */
    private fun handleTileUpdate(update: TileUpdateMessage) {
        val tileKey = "${update.tileX},${update.tileY}"
        val currentTiles = _tiles.value.toMutableMap()
        val tile = currentTiles[tileKey]
        
        if (tile != null) {
            // Store the full character string (may include decoration codes)
            tile.setCharacter(update.charX, update.charY, update.character)
            
            // Update colors if provided
            val charIndex = update.charY * 16 + update.charX
            update.color?.let { tile.properties.color[charIndex] = it }
            update.bgColor?.let { tile.properties.bgColor[charIndex] = it }
            
            tile.lastModified = update.timestamp
            
            // Update the map to trigger StateFlow collectors
            _tiles.value = currentTiles
            
            // Mark tile for re-render
            renderQueue.add(tileKey)
        }
    }
    
    /**
     * Handle cursor update from other users
     */
    private fun handleCursorUpdate(message: CursorMessage) {
        message.position?.let { position ->
            val currentCursors = _guestCursors.value.toMutableMap()
            val clientId = webSocketManager.getClientId()
            
            if (clientId != null) {
                currentCursors[clientId] = position
                _guestCursors.value = currentCursors
            }
        }
    }
    
    /**
     * Handle property updates
     */
    private fun handlePropertyUpdate(update: PropertyUpdateMessage) {
        val currentWorldInfo = _worldInfo.value ?: return
        
        _worldInfo.postValue(currentWorldInfo.copy(
            isMember = update.data.isMember ?: currentWorldInfo.isMember,
            canWrite = update.data.writability?.let { it <= TileProperties.WRITABILITY_MEMBER } 
                ?: currentWorldInfo.canWrite
        ))
    }
    
    /**
     * Handle user count updates
     */
    private fun handleUserCount(countMessage: UserCountMessage) {
        _clientState.postValue(_clientState.value?.copy(userCount = countMessage.count))
        _worldInfo.postValue(_worldInfo.value?.copy(userCount = countMessage.count))
    }
    
    /**
     * Handle announcements
     */
    private fun handleAnnouncement(announcement: AnnouncementMessage) {
        _announcements.postValue(announcement.message)
    }
    
    /**
     * Handle errors
     */
    private fun handleError(error: ErrorMessage) {
        _error.postValue("Server error: ${error.message}")
    }
    
    /**
     * Fetch initial tiles for current viewport
     */
    private fun fetchInitialTiles() {
        viewModelScope.launch {
            try {
                val cameraPos = _cameraPosition.value
                val currentZoom = _zoom.value
                
                // Calculate visible tile range based on viewport
                val viewportWidth = 1920 // This should come from SurfaceView
                val viewportHeight = 1080
                
                val minTileX = ((-cameraPos.first) / (16 * 12 * currentZoom)).toInt() - 1
                val maxTileX = (((-cameraPos.first + viewportWidth) / (16 * 12 * currentZoom)).toInt()) + 1
                val minTileY = ((-cameraPos.second) / (8 * 16 * currentZoom)).toInt() - 1
                val maxTileY = (((-cameraPos.second + viewportHeight) / (8 * 16 * currentZoom)).toInt()) + 1
                
                val fetchRectangles = listOf(
                    FetchRectangle(minTileX, minTileY, maxTileX, maxTileY)
                )
                
                val fetchMessage = FetchMessage(fetchRectangles)
                webSocketManager.sendMessage(fetchMessage)
                
            } catch (e: Exception) {
                _error.postValue("Failed to fetch tiles: ${e.message}")
            }
        }
    }
    
    /**
     * Send chat message
     */
    fun sendChatMessage(message: String) {
        viewModelScope.launch {
            try {
                val preferences = userPreferences.value ?: return@launch
                
                // Convert Android color (ARGB) to hex string for chat (without alpha)
                val colorHex = preferences.textColor?.let { color ->
                    String.format("#%06X", color and 0x00FFFFFF)
                }
                
                val chatMessage = ChatMessage(
                    nickname = preferences.nickname,
                    message = message,
                    location = ChatLocation.PAGE,
                    color = colorHex
                )
                
                webSocketManager.sendMessage(chatMessage)
                
            } catch (e: Exception) {
                _error.postValue("Failed to send chat: ${e.message}")
            }
        }
    }
    
    /**
     * Handle tap at coordinates
     */
    fun onTapAt(tileX: Int, tileY: Int, charX: Int, charY: Int) {
        // Update local cursor
        _localCursor.value = CursorPosition(tileX, tileY, charX, charY)
        
        // Send cursor position to server
        val cursorMessage = CursorMessage(position = CursorPosition(tileX, tileY, charX, charY))
        webSocketManager.sendMessage(cursorMessage)
        
        // Check for links at this position
        checkForLinks(tileX, tileY, charX, charY)
    }
    
    /**
     * Handle long press
     */
    fun onLongPressAt(tileX: Int, tileY: Int, charX: Int, charY: Int) {
        // TODO: Show context menu for selection, protection, etc.
        // Parameters are kept for interface consistency
    }
    
    /**
     * Update camera position
     */
    fun updateCameraPosition(x: Float, y: Float, newZoom: Float) {
        _cameraPosition.value = Pair(x, y)
        _zoom.value = newZoom
        
        // Update boundary on server
        updateBoundaryOnServer()
    }
    
    /**
     * Update boundary on server
     */
    private fun updateBoundaryOnServer() {
        viewModelScope.launch {
            try {
                val cameraPos = _cameraPosition.value
                val zoom = _zoom.value
                
                // Calculate visible tile range
                // These should come from SurfaceView but are hardcoded for now
                // val viewportWidth = 1920 
                // val viewportHeight = 1080
                
                val centerTileX = ((-cameraPos.first) / (16 * 12 * zoom)).toInt()
                val centerTileY = ((-cameraPos.second) / (8 * 16 * zoom)).toInt()
                
                val boundaryMessage = BoundaryMessage(
                    centerX = centerTileX,
                    centerY = centerTileY,
                    minX = centerTileX - 2,
                    minY = centerTileY - 2,
                    maxX = centerTileX + 2,
                    maxY = centerTileY + 2
                )
                
                webSocketManager.sendMessage(boundaryMessage)
                
            } catch (e: Exception) {
                // Log error but don't show to user
            }
        }
    }
    
    /**
     * Get visible tiles for rendering
     */
    fun getVisibleTiles(width: Int, height: Int): Map<String, Tile> {
        val cameraPos = _cameraPosition.value
        val zoom = _zoom.value
        
        val tileWidth = 16 * 12 * zoom
        val tileHeight = 8 * 16 * zoom
        
        val minTileX = ((-cameraPos.first) / tileWidth).toInt() - 1
        val maxTileX = (((-cameraPos.first + width) / tileWidth).toInt()) + 1
        val minTileY = ((-cameraPos.second) / tileHeight).toInt() - 1
        val maxTileY = (((-cameraPos.second + height) / tileHeight).toInt()) + 1
        
        val visibleTiles = mutableMapOf<String, Tile>()
        
        for (tileY in minTileY..maxTileY) {
            for (tileX in minTileX..maxTileX) {
                val tileKey = "$tileX,$tileY"
                _tiles.value[tileKey]?.let { visibleTiles[tileKey] = it }
            }
        }
        
        return visibleTiles
    }
    
    /**
     * Get guest cursors for rendering
     */
    fun getGuestCursors(): Map<String, CursorPosition> = _guestCursors.value
    
    /**
     * Get local cursor
     */
    fun getLocalCursor(): CursorPosition? = _localCursor.value
    
    /**
     * Check for links at position
     */
    private fun checkForLinks(tileX: Int, tileY: Int, charX: Int, charY: Int) {
        val tileKey = "$tileX,$tileY"
        val tile = _tiles.value[tileKey] ?: return
        
        val charIndex = charY * 16 + charX
        val cellProps = tile.properties.cellProps[charIndex]
        
        cellProps?.link?.let { link ->
            when (link.type) {
                LinkType.URL -> link.url?.let { url ->
                    // Handle URL link
                    openUrl(url)
                }
                LinkType.COORD -> {
                    // Handle coordinate link
                    link.linkTileX?.let { linkX ->
                        link.linkTileY?.let { linkY ->
                            centerOn(linkX, linkY)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Open URL (would be handled by activity)
     */
    private fun openUrl(url: String) {
        // This would be implemented by the activity
        // URL parameter kept for interface consistency
    }
    
    /**
     * Center view on coordinates
     */
    private fun centerOn(tileX: Int, tileY: Int, charX: Int = 0, charY: Int = 0) {
        // This would update camera position to center on given coordinates
        // Implementation depends on SurfaceView
        // Parameters are kept for interface consistency
    }
    
    /**
     * Write character at position with optional decorations
     */
    fun writeCharacter(
        tileX: Int,
        tileY: Int,
        charX: Int,
        charY: Int,
        character: Char,
        bold: Boolean = false,
        italic: Boolean = false,
        underline: Boolean = false,
        strikethrough: Boolean = false,
        textColor: Int? = null,
        bgColor: Int? = null
    ) {
        viewModelScope.launch {
            try {
                // Check permissions
                if (!canWriteAt(tileX, tileY)) {
                    _error.postValue("You don't have permission to write at this location")
                    return@launch
                }
                
                // Encode character with decorations
                val encodedChar = TextDecorations.encode(
                    char = character,
                    bold = bold,
                    italic = italic,
                    underline = underline,
                    strikethrough = strikethrough
                )
                
                // Add to local tile immediately for instant feedback
                val tileKey = "$tileX,$tileY"
                val currentTiles = _tiles.value.toMutableMap()
                val tile = currentTiles[tileKey] ?: run {
                    // Create new tile if it doesn't exist
                    Tile(tileX, tileY)
                }
                
                tile.setCharacter(charX, charY, encodedChar)
                
                // Update colors if provided
                val charIndex = charY * 16 + charX
                if (textColor != null) {
                    tile.properties.color[charIndex] = textColor
                }
                if (bgColor != null) {
                    tile.properties.bgColor[charIndex] = bgColor
                }
                
                currentTiles[tileKey] = tile
                _tiles.value = currentTiles
                
                // Add to write buffer
                val editData = EditData(
                    tileY = tileY,
                    tileX = tileX,
                    charY = charY,
                    charX = charX,
                    timestamp = System.currentTimeMillis(),
                    character = encodedChar,
                    editId = webSocketManager.generateEditId(),
                    color = textColor ?: userPreferences.value?.textColor,
                    bgColor = bgColor ?: userPreferences.value?.bgColor?.takeIf { it != -1 }
                )
                
                writeBuffer.add(editData)
                
                // Mark tile for re-render
                renderQueue.add(tileKey)
                
                // Flush buffer periodically or when full
                if (writeBuffer.size >= 10) {
                    flushWriteBuffer()
                }
                
            } catch (e: Exception) {
                _error.postValue("Failed to write character: ${e.message}")
            }
        }
    }
    
    /**
     * Check if user can write at position
     */
    private fun canWriteAt(tileX: Int, tileY: Int): Boolean {
        val tile = _tiles.value["$tileX,$tileY"] ?: return true // Assume public if tile doesn't exist
        
        return when (tile.properties.writability) {
            TileProperties.WRITABILITY_PUBLIC -> true
            TileProperties.WRITABILITY_MEMBER -> _worldInfo.value?.isMember == true
            TileProperties.WRITABILITY_OWNER -> false // Only owners can write here
            else -> true
        }
    }
    
    /**
     * Flush write buffer to server
     */
    private fun flushWriteBuffer() {
        if (writeBuffer.isEmpty()) return
        
        val edits = writeBuffer.toList()
        writeBuffer.clear()
        
        // Convert EditData to protocol arrays
        val protocolEdits = edits.map { it.toProtocolArray() }
        
        val writeMessage = WriteMessage(edits = protocolEdits)
        webSocketManager.sendMessage(writeMessage)
    }
    
    /**
     * Convert server tile properties to client format
     */
    private fun convertServerTileProperties(serverProperties: ServerTileProperties): TileProperties {
        val properties = TileProperties()
        
        serverProperties.writability?.let { properties.writability = it }
        
        // Handle color data flexibly - can be List<Int>, String, or Int
        serverProperties.color?.let { colorData ->
            properties.color = parseColorArrayData(colorData, 128) // 128 characters per tile
        }
        
        serverProperties.bgcolor?.let { bgColorData ->
            properties.bgColor = parseColorArrayData(bgColorData, 128)
        }
        
        serverProperties.char?.let { charData ->
            properties.charWritability = parseColorArrayData(charData, 128)
        }
        
        serverProperties.cellProps?.let { _ ->
            // Convert cell properties
        }
        
        return properties
    }
    
    /**
     * Parse color/array data that can be in various formats
     */
    private fun parseColorArrayData(data: Any?, defaultSize: Int): IntArray {
        val colorArray = IntArray(defaultSize) { -1 }
        
        data?.let {
            when (it) {
                is List<*> -> {
                    // Handle List<Int>
                    it.forEachIndexed { index, value ->
                        if (index < defaultSize) {
                            colorArray[index] = when (value) {
                                is Number -> value.toInt()
                                is String -> parseColorString(value) ?: -1
                                else -> -1
                            }
                        }
                    }
                }
                is String -> {
                    // Handle string like "1,2,3,4" or hex colors
                    if (it.contains(",")) {
                        it.split(",").forEachIndexed { index, colorStr ->
                            if (index < defaultSize) {
                                colorArray[index] = parseColorString(colorStr.trim()) ?: -1
                            }
                        }
                    } else {
                        // Single color value
                        val parsedColor = parseColorString(it)
                        if (parsedColor != null) {
                            colorArray[0] = parsedColor
                        }
                    }
                }
                is Number -> {
                    // Single integer color
                    colorArray[0] = it.toInt()
                }
            }
        }
        
        return colorArray
    }
    
    /**
     * Parse a color string (hex, decimal, etc.) to integer
     */
    private fun parseColorString(colorStr: String): Int? {
        return try {
            when {
                colorStr.startsWith("#") -> {
                    android.graphics.Color.parseColor(colorStr)
                }
                colorStr.startsWith("0x") -> {
                    colorStr.substring(2).toInt(16)
                }
                colorStr.all { it.isDigit() } -> {
                    colorStr.toInt()
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Activity lifecycle methods
     */
    fun onResume() {
        if (!isConnected()) {
            viewModelScope.launch {
                connect()
            }
        }
    }
    
    /**
     * Called when activity confirms it's in valid state to proceed
     */
    fun onActivityResume() {
        // This is called after the activity has verified it's not finishing/destroyed
        // Additional safety checks can be added here
        if (!isConnected()) {
            viewModelScope.launch {
                connect()
            }
        }
    }
    
    fun onPause() {
        flushWriteBuffer()
    }
    
    /**
     * Create a URL link at position
     */
    fun createUrlLink(tileX: Int, tileY: Int, charX: Int, charY: Int, url: String) {
        viewModelScope.launch {
            try {
                val linkMessage = LinkBuilder.createUrlLink(tileX, tileY, charX, charY, url)
                webSocketManager.sendMessage(linkMessage)
                
                // Update local tile
                val tileKey = "$tileX,$tileY"
                val currentTiles = _tiles.value.toMutableMap()
                val tile = currentTiles[tileKey]
                
                if (tile != null) {
                    val charIndex = charY * 16 + charX
                    val linkProps = LinkProperties(LinkType.URL, url = url)
                    tile.properties.cellProps[charIndex] = CellProperties(link = linkProps)
                    _tiles.value = currentTiles
                }
            } catch (e: Exception) {
                _error.postValue("Failed to create link: ${e.message}")
            }
        }
    }
    
    /**
     * Create a coordinate link at position
     */
    fun createCoordLink(tileX: Int, tileY: Int, charX: Int, charY: Int, linkTileX: Int, linkTileY: Int) {
        viewModelScope.launch {
            try {
                val linkMessage = LinkBuilder.createCoordLink(tileX, tileY, charX, charY, linkTileX, linkTileY)
                webSocketManager.sendMessage(linkMessage)
                
                // Update local tile
                val tileKey = "$tileX,$tileY"
                val currentTiles = _tiles.value.toMutableMap()
                val tile = currentTiles[tileKey]
                
                if (tile != null) {
                    val charIndex = charY * 16 + charX
                    val linkProps = LinkProperties(LinkType.COORD, linkTileX = linkTileX, linkTileY = linkTileY)
                    tile.properties.cellProps[charIndex] = CellProperties(link = linkProps)
                    _tiles.value = currentTiles
                }
            } catch (e: Exception) {
                _error.postValue("Failed to create coordinate link: ${e.message}")
            }
        }
    }
    
    /**
     * Remove link at position
     */
    fun removeLink(tileX: Int, tileY: Int, charX: Int, charY: Int) {
        viewModelScope.launch {
            try {
                val linkMessage = LinkBuilder.createNoteLink(tileX, tileY, charX, charY)
                webSocketManager.sendMessage(linkMessage)
                
                // Update local tile
                val tileKey = "$tileX,$tileY"
                val currentTiles = _tiles.value.toMutableMap()
                val tile = currentTiles[tileKey]
                
                if (tile != null) {
                    val charIndex = charY * 16 + charX
                    tile.properties.cellProps.remove(charIndex)
                    _tiles.value = currentTiles
                }
            } catch (e: Exception) {
                _error.postValue("Failed to remove link: ${e.message}")
            }
        }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Clear announcement
     */
    fun clearAnnouncement() {
        _announcements.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        flushWriteBuffer()
    }
}