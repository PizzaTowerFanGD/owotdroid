package com.owot.android.client.data.models

import android.graphics.Color
import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * Represents a single tile in the OWOT world
 */
data class Tile(
    val tileX: Int,
    val tileY: Int,
    var content: CharArray = CharArray(128), // 16x8 tile = 128 characters
    var properties: TileProperties = TileProperties(),
    var lastModified: Long = System.currentTimeMillis()
) {
    companion object {
        const val TILE_WIDTH = 16
        const val TILE_HEIGHT = 8
        const val TILE_SIZE = TILE_WIDTH * TILE_HEIGHT
    }
    
    fun setCharacter(x: Int, y: Int, char: Char) {
        if (x in 0 until TILE_WIDTH && y in 0 until TILE_HEIGHT) {
            content[y * TILE_WIDTH + x] = char
        }
    }
    
    fun getCharacter(x: Int, y: Int): Char {
        return if (x in 0 until TILE_WIDTH && y in 0 until TILE_HEIGHT) {
            content[y * TILE_WIDTH + x]
        } else ' '
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as Tile
        
        if (tileX != other.tileX) return false
        if (tileY != other.tileY) return false
        if (!content.contentEquals(other.content)) return false
        if (properties != other.properties) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = tileX
        result = 31 * result + tileY
        result = 31 * result + content.contentHashCode()
        result = 31 * result + properties.hashCode()
        return result
    }
}

/**
 * Properties for a tile including colors, links, and writability
 */
data class TileProperties(
    var writability: Int = WRITABILITY_PUBLIC, // 0: public, 1: member, 2: owner
    var color: IntArray = IntArray(Tile.TILE_SIZE) { Color.BLACK },
    var bgColor: IntArray = IntArray(Tile.TILE_SIZE) { -1 }, // -1 for no background
    var charWritability: IntArray = IntArray(Tile.TILE_SIZE) { -1 }, // -1 to inherit from tile
    var cellProps: MutableMap<Int, CellProperties> = mutableMapOf() // key: position index
) {
    companion object {
        const val WRITABILITY_PUBLIC = 0
        const val WRITABILITY_MEMBER = 1
        const val WRITABILITY_OWNER = 2
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as TileProperties
        
        if (writability != other.writability) return false
        if (!color.contentEquals(other.color)) return false
        if (!bgColor.contentEquals(other.bgColor)) return false
        if (!charWritability.contentEquals(other.charWritability)) return false
        if (cellProps != other.cellProps) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = writability
        result = 31 * result + color.contentHashCode()
        result = 31 * result + bgColor.contentHashCode()
        result = 31 * result + charWritability.contentHashCode()
        result = 31 * result + cellProps.hashCode()
        return result
    }
}

/**
 * Properties for individual cells within a tile
 */
data class CellProperties(
    var link: LinkProperties? = null
)

/**
 * Link properties for a character
 */
data class LinkProperties(
    val type: LinkType,
    val url: String? = null,
    val linkTileX: Int? = null,
    val linkTileY: Int? = null
)

enum class LinkType {
    @SerializedName("url")
    URL,
    
    @SerializedName("coord")
    COORD
}

/**
 * Character decorations (bold, italic, underline, etc.)
 */
enum class CharacterDecoration(val unicode: Int) {
    BOLD(0x20F0),
    ITALIC(0x20F1),
    UNDERLINE(0x20F2),
    STRIKETHROUGH(0x20F3)
}

/**
 * World model containing global world information
 */
data class WorldModel(
    val name: String,
    var creationDate: Date? = null,
    var views: Long = 0,
    var isMember: Boolean = false,
    var writability: Int = TileProperties.WRITABILITY_PUBLIC,
    var theme: WorldTheme = WorldTheme(),
    var features: Set<String> = emptySet(),
    var charRate: Int = 100, // characters per second limit
    var maxEditSize: Int = 1000
)

/**
 * World theme and styling
 */
data class WorldTheme(
    var backgroundColor: Int = Color.WHITE,
    var textColor: Int = Color.BLACK,
    var gridColor: Int = Color.GRAY,
    var cursorColor: Int = Color.BLUE,
    var linkColor: Int = Color.BLUE,
    var fontSize: Float = 14f,
    var fontFamily: String = "monospace"
)

/**
 * User model containing user-specific information
 */
data class UserModel(
    var username: String = "Anonymous",
    var isAdmin: Boolean = false,
    var isOp: Boolean = false,
    var isStaff: Boolean = false,
    var canWrite: Boolean = true,
    var canColorText: Boolean = false,
    var canProtectTiles: Boolean = false,
    var canAdmin: Boolean = false
)

/**
 * Client state information
 */
data class ClientState(
    var clientId: String = "",
    var socketChannel: String = "",
    var isConnected: Boolean = false,
    var isConnecting: Boolean = false,
    var lastPingTime: Long = 0,
    var ping: Long = 0,
    var userCount: Int = 0
)