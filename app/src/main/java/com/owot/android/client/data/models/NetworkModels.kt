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
 */
data class WriteMessage(
    @SerializedName("edits")
    val edits: List<EditData>,
    @SerializedName("public_only")
    val publicOnly: Boolean = false,
    @SerializedName("preserve_links")
    val preserveLinks: Boolean = false,
    @SerializedName("request")
    val request: String? = null
) : WSMessage() {
    override val kind: String = "write"
}

data class EditData(
    @SerializedName("tileY")
    val tileY: Int,
    @SerializedName("tileX")
    val tileX: Int,
    @SerializedName("charY")
    val charY: Int,
    @SerializedName("charX")
    val charX: Int,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("character")
    val character: String,
    @SerializedName("editID")
    val editId: String,
    @SerializedName("color")
    val color: Int? = null,
    @SerializedName("bgColor")
    val bgColor: Int? = null
)

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
 */
data class LinkMessage(
    @SerializedName("data")
    val data: LinkData
) : WSMessage() {
    override val kind: String = "link"
}

data class LinkData(
    @SerializedName("tileX")
    val tileX: Int,
    @SerializedName("tileY")
    val tileY: Int,
    @SerializedName("charX")
    val charX: Int,
    @SerializedName("charY")
    val charY: Int,
    @SerializedName("type")
    val type: LinkType,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("link_tileX")
    val linkTileX: Int? = null,
    @SerializedName("link_tileY")
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
    val content: String,
    @SerializedName("properties")
    val properties: ServerTileProperties
)

data class ServerTileProperties(
    @SerializedName("cell_props")
    val cellProps: Map<String, CellProperties>? = null,
    @SerializedName("writability")
    val writability: Int? = null,
    @SerializedName("color")
    val color: List<Int>? = null,
    @SerializedName("bgcolor")
    val bgcolor: List<Int>? = null,
    @SerializedName("char")
    val char: List<Int>? = null
)

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