package com.owot.android.client.data.models

import com.google.gson.annotations.SerializedName

/**
 * Base class for all WebSocket messages
 */
sealed class WSMessage {
    abstract val kind: String
}

/**
 * Fetch message for requesting tile data
 */
data class FetchMessage(
    @SerializedName("fetchRectangles")
    val fetchRectangles: List<FetchRectangle>,
    @SerializedName("utf16")
    val utf16: Boolean = true,
    @SerializedName("array")
    val array: Boolean = false,
    @SerializedName("content_only")
    val contentOnly: Boolean = false,
    @SerializedName("concat")
    val concat: Boolean = false,
    @SerializedName("request")
    val request: String? = null
) : WSMessage() {
    override val kind: String = "fetch"
}

data class FetchRectangle(
    @SerializedName("minX")
    val minX: Int,
    @SerializedName("minY")
    val minY: Int,
    @SerializedName("maxX")
    val maxX: Int,
    @SerializedName("maxY")
    val maxY: Int
)

/**
 * Write message for sending character edits
 * OWOT protocol sends edits as arrays: [tileY, tileX, charY, charX, timestamp, character, editId, textColor?, bgColor?]
 */
data class WriteMessage(
    @SerializedName("kind")
    override val kind: String = "write",
    @SerializedName("edits")
    val edits: List<List<Any>>
) : WSMessage()

/**
 * Helper class to build edit arrays for WriteMessage
 */
object EditBuilder {
    /**
     * Create an edit array in OWOT format: [tileY, tileX, charY, charX, timestamp, character, editId, textColor?, bgColor?]
     */
    fun createEdit(
        tileY: Int,
        tileX: Int,
        charY: Int,
        charX: Int,
        timestamp: Long,
        character: String,
        editId: Any, // Can be String or Number
        textColor: Int? = null,
        bgColor: Int? = null
    ): List<Any> {
        val edit = mutableListOf<Any>(tileY, tileX, charY, charX, timestamp, character, editId)
        
        // Add text color if provided (strip alpha channel for OWOT compatibility)
        if (textColor != null) {
            edit.add(textColor and 0x00FFFFFF)
        }
        
        // Add background color if provided (text color must be included first)
        if (bgColor != null) {
            if (textColor == null) {
                edit.add(0) // Placeholder for text color
            }
            edit.add(bgColor and 0x00FFFFFF)  // Strip alpha channel
        }
        
        return edit
    }
}

/**
 * EditData class for internal use (not sent over network)
 */
data class EditData(
    val tileY: Int,
    val tileX: Int,
    val charY: Int,
    val charX: Int,
    val timestamp: Long,
    val character: String,
    val editId: String,
    val color: Int? = null,
    val bgColor: Int? = null
) {
    /**
     * Convert to OWOT protocol array format
     */
    fun toProtocolArray(): List<Any> {
        return EditBuilder.createEdit(
            tileY, tileX, charY, charX, timestamp, character, editId, color, bgColor
        )
    }
}

/**
 * Chat message
 */
data class ChatMessage(
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("location")
    val location: ChatLocation,
    @SerializedName("color")
    val color: Int? = null,
    @SerializedName("customMeta")
    val customMeta: Map<String, Any>? = null
) : WSMessage() {
    override val kind: String = "chat"
}

enum class ChatLocation(val value: String) {
    PAGE("page"),
    GLOBAL("global")
}

/**
 * Chat history request
 */
data class ChatHistoryMessage(
    @SerializedName("request")
    val request: String? = null
) : WSMessage() {
    override val kind: String = "chathistory"
}

/**
 * Ping message for latency measurement
 */
data class PingMessage(
    @SerializedName("id")
    val id: String? = null
) : WSMessage() {
    override val kind: String = "ping"
}

/**
 * Command message for broadcasting commands
 */
data class CommandMessage(
    @SerializedName("data")
    val data: String,
    @SerializedName("include_username")
    val includeUsername: Boolean = false,
    @SerializedName("coords")
    val coords: List<Int>? = null
) : WSMessage() {
    override val kind: String = "cmd"
}

/**
 * Command options message
 */
data class CommandOptionsMessage(
    @SerializedName("request")
    val request: String? = null
) : WSMessage() {
    override val kind: String = "cmd_opt"
}

/**
 * Protection message for changing tile/character protection
 */
data class ProtectionMessage(
    @SerializedName("data")
    val data: ProtectionData
) : WSMessage() {
    override val kind: String = "protect"
}

data class ProtectionData(
    @SerializedName("tileX")
    val tileX: Int,
    @SerializedName("tileY")
    val tileY: Int,
    @SerializedName("type")
    val type: ProtectionType,
    @SerializedName("charX")
    val charX: Int = -1,
    @SerializedName("charY")
    val charY: Int = -1,
    @SerializedName("charWidth")
    val charWidth: Int = 1,
    @SerializedName("charHeight")
    val charHeight: Int = 1,
    @SerializedName("precise")
    val precise: Boolean = false
)

enum class ProtectionType(val value: String) {
    @SerializedName("public")
    PUBLIC("public"),
    @SerializedName("member-only")
    MEMBER_ONLY("member-only"),
    @SerializedName("owner-only")
    OWNER_ONLY("owner-only")
}

/**
 * Link message for creating links
 * OWOT protocol format: { kind: "link", type: "url"/"coord"/"note", tileX, tileY, charX, charY, data: {...} }
 */
data class LinkMessage(
    @SerializedName("kind")
    override val kind: String = "link",
    @SerializedName("type")
    val type: String, // "url", "coord", or "note"
    @SerializedName("tileX")
    val tileX: Int,
    @SerializedName("tileY")
    val tileY: Int,
    @SerializedName("charX")
    val charX: Int,
    @SerializedName("charY")
    val charY: Int,
    @SerializedName("data")
    val data: Map<String, Any>
) : WSMessage()

/**
 * Helper object to create link messages
 */
object LinkBuilder {
    fun createUrlLink(tileX: Int, tileY: Int, charX: Int, charY: Int, url: String): LinkMessage {
        return LinkMessage(
            type = "url",
            tileX = tileX,
            tileY = tileY,
            charX = charX,
            charY = charY,
            data = mapOf("type" to "url", "url" to url)
        )
    }
    
    fun createCoordLink(tileX: Int, tileY: Int, charX: Int, charY: Int, linkTileX: Int, linkTileY: Int): LinkMessage {
        return LinkMessage(
            type = "coord",
            tileX = tileX,
            tileY = tileY,
            charX = charX,
            charY = charY,
            data = mapOf("type" to "coord", "link_tileX" to linkTileX, "link_tileY" to linkTileY)
        )
    }
    
    fun createNoteLink(tileX: Int, tileY: Int, charX: Int, charY: Int): LinkMessage {
        return LinkMessage(
            type = "note",
            tileX = tileX,
            tileY = tileY,
            charX = charX,
            charY = charY,
            data = mapOf("type" to "note")
        )
    }
}

/**
 * LinkData class for internal use
 */
data class LinkData(
    val tileX: Int,
    val tileY: Int,
    val charX: Int,
    val charY: Int,
    val type: LinkType,
    val url: String? = null,
    val linkTileX: Int? = null,
    val linkTileY: Int? = null
)

/**
 * Clear tile message for erasing content
 */
data class ClearTileMessage(
    @SerializedName("data")
    val data: ClearData
) : WSMessage() {
    override val kind: String = "clear_tile"
}

data class ClearData(
    @SerializedName("tileX")
    val tileX: Int,
    @SerializedName("tileY")
    val tileY: Int,
    @SerializedName("charX")
    val charX: Int = -1,
    @SerializedName("charY")
    val charY: Int = -1,
    @SerializedName("charWidth")
    val charWidth: Int = 1,
    @SerializedName("charHeight")
    val charHeight: Int = 1
)

/**
 * Cursor message for updating cursor position
 */
data class CursorMessage(
    @SerializedName("hidden")
    val hidden: Boolean? = null,
    @SerializedName("position")
    val position: CursorPosition? = null
) : WSMessage() {
    override val kind: String = "cursor"
}

data class CursorPosition(
    @SerializedName("tileX")
    val tileX: Int,
    @SerializedName("tileY")
    val tileY: Int,
    @SerializedName("charX")
    val charX: Int,
    @SerializedName("charY")
    val charY: Int
)

/**
 * Boundary message for informing server of visible area
 */
data class BoundaryMessage(
    @SerializedName("centerX")
    val centerX: Int,
    @SerializedName("centerY")
    val centerY: Int,
    @SerializedName("minX")
    val minX: Int,
    @SerializedName("minY")
    val minY: Int,
    @SerializedName("maxX")
    val maxX: Int,
    @SerializedName("maxY")
    val maxY: Int
) : WSMessage() {
    override val kind: String = "boundary"
}

/**
 * Stats message for requesting world statistics
 */
data class StatsMessage(
    @SerializedName("id")
    val id: String? = null
) : WSMessage() {
    override val kind: String = "stats"
}

/**
 * Server response messages
 */

/**
 * Fetch response from server
 */
data class FetchResponse(
    override val kind: String = "fetch",
    @SerializedName("tiles")
    val tiles: Map<String, ServerTile>,
    @SerializedName("request")
    val request: String? = null
) : WSMessage()

data class ServerTile(
    @SerializedName("content")
    val content: Any, // Can be Array<String> or String
    @SerializedName("properties")
    val properties: ServerTileProperties
)

data class ServerTileProperties(
    @SerializedName("cell_props")
    val cellProps: Map<String, Map<String, Any>>? = null, // Nested map: "y" -> "x" -> cell data
    @SerializedName("writability")
    val writability: Int? = null,
    @SerializedName("color")
    val color: Any? = null, // Can be List<Int>, String, or Int
    @SerializedName("bgcolor")
    val bgcolor: Any? = null, // Can be List<Int>, String, or Int
    @SerializedName("char")
    val char: Any? = null // Can be List<Int>, String, or Int
)

/**
 * Helper object to parse server tile data
 */
object TileParser {
    /**
     * Parse server tile content into array of strings
     */
    fun parseContent(content: Any): Array<String> {
        return when (content) {
            is List<*> -> {
                // Content is already an array
                val arr = Array(128) { " " }
                content.forEachIndexed { index, item ->
                    if (index < 128 && item is String) {
                        arr[index] = item
                    }
                }
                arr
            }
            is String -> {
                // Content is a string, split into characters
                val arr = Array(128) { " " }
                content.forEachIndexed { index, char ->
                    if (index < 128) {
                        arr[index] = char.toString()
                    }
                }
                arr
            }
            else -> Array(128) { " " }
        }
    }
    
    /**
     * Parse color data (can be list, string, or single value)
     */
    fun parseColorArray(colorData: Any?): IntArray {
        val colors = IntArray(128) { 0 } // Default to black
        
        when (colorData) {
            is List<*> -> {
                colorData.forEachIndexed { index, item ->
                    if (index < 128) {
                        colors[index] = parseColorValue(item)
                    }
                }
            }
            is String -> {
                // Single color for all or comma-separated
                val parts = colorData.split(",")
                if (parts.size > 1) {
                    parts.forEachIndexed { index, part ->
                        if (index < 128) {
                            colors[index] = parseColorValue(part.trim())
                        }
                    }
                } else {
                    val color = parseColorValue(colorData)
                    colors.fill(color)
                }
            }
            is Number -> {
                val color = colorData.toInt()
                colors.fill(color)
            }
            null -> colors.fill(0)
        }
        
        return colors
    }
    
    /**
     * Parse background color array (similar to color but defaults to -1 for transparent)
     */
    fun parseBgColorArray(bgColorData: Any?): IntArray {
        val bgColors = IntArray(128) { -1 } // Default to transparent
        
        when (bgColorData) {
            is List<*> -> {
                bgColorData.forEachIndexed { index, item ->
                    if (index < 128) {
                        bgColors[index] = parseColorValue(item)
                    }
                }
            }
            is String -> {
                val parts = bgColorData.split(",")
                if (parts.size > 1) {
                    parts.forEachIndexed { index, part ->
                        if (index < 128) {
                            bgColors[index] = parseColorValue(part.trim())
                        }
                    }
                } else {
                    val bgColor = parseColorValue(bgColorData)
                    bgColors.fill(bgColor)
                }
            }
            is Number -> {
                val bgColor = bgColorData.toInt()
                bgColors.fill(bgColor)
            }
            null -> bgColors.fill(-1)
        }
        
        return bgColors
    }
    
    /**
     * Parse a single color value (can be hex string, decimal, or number)
     * Returns RGB color as integer (without alpha channel for OWOT compatibility)
     */
    private fun parseColorValue(value: Any?): Int {
        return when (value) {
            is Number -> {
                // Strip alpha channel if present
                value.toInt() and 0x00FFFFFF
            }
            is String -> {
                val trimmed = value.trim()
                when {
                    trimmed.startsWith("#") -> {
                        // Hex color like "#FF0000"
                        // Parse and strip alpha channel
                        try {
                            val color = android.graphics.Color.parseColor(trimmed)
                            color and 0x00FFFFFF  // Remove alpha channel
                        } catch (e: Exception) {
                            0
                        }
                    }
                    trimmed.startsWith("0x") -> {
                        // Hex color like "0xFF0000"
                        val hex = trimmed.substring(2).toIntOrNull(16) ?: 0
                        hex and 0x00FFFFFF  // Remove alpha channel
                    }
                    trimmed.contains(",") -> {
                        // RGB format like "255,0,0"
                        val parts = trimmed.split(",")
                        if (parts.size >= 3) {
                            val r = (parts[0].toIntOrNull() ?: 0) and 0xFF
                            val g = (parts[1].toIntOrNull() ?: 0) and 0xFF
                            val b = (parts[2].toIntOrNull() ?: 0) and 0xFF
                            (r shl 16) or (g shl 8) or b  // Create RGB without alpha
                        } else 0
                    }
                    else -> {
                        // Try parsing as decimal
                        val decimal = trimmed.toIntOrNull() ?: 0
                        decimal and 0x00FFFFFF  // Remove alpha channel
                    }
                }
            }
            else -> 0
        }
    }
    
    /**
     * Parse cell properties from server data
     */
    fun parseCellProps(cellPropsData: Map<String, Map<String, Any>>?): MutableMap<Int, CellProperties> {
        val cellProps = mutableMapOf<Int, CellProperties>()
        
        cellPropsData?.forEach { (yStr, xMap) ->
            val y = yStr.toIntOrNull() ?: return@forEach
            xMap.forEach { (xStr, cellData) ->
                val x = xStr.toIntOrNull() ?: return@forEach
                val index = y * 16 + x
                
                // Parse cell data
                if (cellData is Map<*, *>) {
                    val linkData = cellData["link"] as? Map<*, *>
                    if (linkData != null) {
                        val linkType = linkData["type"] as? String
                        val link = when (linkType) {
                            "url" -> {
                                val url = linkData["url"] as? String ?: ""
                                LinkProperties(LinkType.URL, url = url)
                            }
                            "coord" -> {
                                val linkTileX = (linkData["link_tileX"] as? Number)?.toInt()
                                val linkTileY = (linkData["link_tileY"] as? Number)?.toInt()
                                LinkProperties(LinkType.COORD, linkTileX = linkTileX, linkTileY = linkTileY)
                            }
                            else -> null
                        }
                        
                        if (link != null) {
                            cellProps[index] = CellProperties(link = link)
                        }
                    }
                }
            }
        }
        
        return cellProps
    }
}

/**
 * Write response from server
 */
data class WriteResponse(
    override val kind: String = "write",
    @SerializedName("accepted")
    val accepted: List<String>,
    @SerializedName("rejected")
    val rejected: Map<String, String>
) : WSMessage()

/**
 * Chat response from server
 */
data class ChatResponse(
    override val kind: String = "chat",
    @SerializedName("location")
    val location: ChatLocation,
    @SerializedName("id")
    val id: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("realUsername")
    val realUsername: String? = null,
    @SerializedName("op")
    val isOp: Boolean = false,
    @SerializedName("admin")
    val isAdmin: Boolean = false,
    @SerializedName("staff")
    val isStaff: Boolean = false,
    @SerializedName("color")
    val color: Int? = null,
    @SerializedName("date")
    val date: String,
    @SerializedName("dataObj")
    val dataObj: ChatMessage? = null
) : WSMessage()

/**
 * Chat history response
 */
data class ChatHistoryResponse(
    override val kind: String = "chathistory",
    @SerializedName("global_chat_prev")
    val globalChatPrev: List<ChatResponse>,
    @SerializedName("page_chat_prev")
    val pageChatPrev: List<ChatResponse>
) : WSMessage()

/**
 * Channel assignment from server
 */
data class ChannelResponse(
    override val kind: String = "channel",
    @SerializedName("socketChannel")
    val socketChannel: String,
    @SerializedName("id")
    val clientId: String
) : WSMessage()

/**
 * Ping response
 */
data class PongMessage(
    override val kind: String = "ping",
    @SerializedName("id")
    val id: String? = null
) : WSMessage()

/**
 * Announcement from server
 */
data class AnnouncementMessage(
    override val kind: String = "announcement",
    @SerializedName("message")
    val message: String
) : WSMessage()

/**
 * Property update from server
 */
data class PropertyUpdateMessage(
    override val kind: String = "propUpdate",
    @SerializedName("data")
    val data: PropertyUpdateData
) : WSMessage()

data class PropertyUpdateData(
    @SerializedName("isMember")
    val isMember: Boolean? = null,
    @SerializedName("writability")
    val writability: Int? = null,
    @SerializedName("theme")
    val theme: WorldTheme? = null,
    @SerializedName("features")
    val features: Set<String>? = null
)

/**
 * User count update
 */
data class UserCountMessage(
    override val kind: String = "user_count",
    @SerializedName("count")
    val count: Int
) : WSMessage()

/**
 * Chat delete request
 */
data class ChatDeleteMessage(
    override val kind: String = "chatdelete",
    @SerializedName("id")
    val id: String,
    @SerializedName("time")
    val time: String
) : WSMessage()

/**
 * Error message from server
 */
data class ErrorMessage(
    override val kind: String = "error",
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String
) : WSMessage()

/**
 * Tile update from server
 */
data class TileUpdateMessage(
    override val kind: String = "tileUpdate",
    @SerializedName("tileY")
    val tileY: Int,
    @SerializedName("tileX")
    val tileX: Int,
    @SerializedName("charY")
    val charY: Int,
    @SerializedName("charX")
    val charX: Int,
    @SerializedName("character")
    val character: String,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("color")
    val color: Int? = null,
    @SerializedName("bgColor")
    val bgColor: Int? = null,
    @SerializedName("editID")
    val editId: String
) : WSMessage()