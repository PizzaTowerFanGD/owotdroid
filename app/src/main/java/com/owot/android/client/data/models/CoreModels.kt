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
    var content: Array<String> = Array(128) { " " }, // 16x8 tile = 128 character strings (with decorations)
    var properties: TileProperties = TileProperties(),
    var lastModified: Long = System.currentTimeMillis()
) {
    companion object {
        const val TILE_WIDTH = 16
        const val TILE_HEIGHT = 8
        const val TILE_SIZE = TILE_WIDTH * TILE_HEIGHT
    }
    
    /**
     * Set a character at position (may include decoration codes)
     */
    fun setCharacter(x: Int, y: Int, charStr: String) {
        if (x in 0 until TILE_WIDTH && y in 0 until TILE_HEIGHT) {
            content[y * TILE_WIDTH + x] = charStr
        }
    }
    
    /**
     * Get a character at position
     */
    fun getCharacter(x: Int, y: Int): String {
        return if (x in 0 until TILE_WIDTH && y in 0 until TILE_HEIGHT) {
            content[y * TILE_WIDTH + x]
        } else " "
    }
    
    /**
     * Get decoded character at position
     */
    fun getDecodedCharacter(x: Int, y: Int): DecodedCharacter {
        val charStr = getCharacter(x, y)
        return TextDecorations.decode(charStr)
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
enum class CharacterDecoration(val bit: Int) {
    BOLD(8),        // 0b1000
    ITALIC(4),      // 0b0100
    UNDERLINE(2),   // 0b0010
    STRIKETHROUGH(1) // 0b0001
}

/**
 * Text decoration utilities for encoding/decoding OWOT decorations
 */
object TextDecorations {
    const val TEXT_DECORATION_OFFSET = 0x20F0
    
    /**
     * Encode text decorations into OWOT format
     * Returns: character + decoration Unicode characters
     */
    fun encode(
        char: Char,
        bold: Boolean = false,
        italic: Boolean = false,
        underline: Boolean = false,
        strikethrough: Boolean = false
    ): String {
        val bitmap = (if (bold) CharacterDecoration.BOLD.bit else 0) or
                     (if (italic) CharacterDecoration.ITALIC.bit else 0) or
                     (if (underline) CharacterDecoration.UNDERLINE.bit else 0) or
                     (if (strikethrough) CharacterDecoration.STRIKETHROUGH.bit else 0)
        
        return if (bitmap == 0) {
            char.toString()
        } else {
            char.toString() + (TEXT_DECORATION_OFFSET + bitmap).toChar()
        }
    }
    
    /**
     * Decode text decorations from OWOT format
     * Returns: DecodedCharacter with character and decoration flags
     */
    fun decode(charStr: String): DecodedCharacter {
        if (charStr.isEmpty()) {
            return DecodedCharacter(' ', false, false, false, false)
        }
        
        val char = charStr[0]
        var bold = false
        var italic = false
        var underline = false
        var strikethrough = false
        
        // Parse decoration characters
        for (i in 1 until charStr.length) {
            val code = charStr[i].code
            if (code >= TEXT_DECORATION_OFFSET && code <= TEXT_DECORATION_OFFSET + 15) {
                val bitmap = code - TEXT_DECORATION_OFFSET
                bold = bold || (bitmap and CharacterDecoration.BOLD.bit != 0)
                italic = italic || (bitmap and CharacterDecoration.ITALIC.bit != 0)
                underline = underline || (bitmap and CharacterDecoration.UNDERLINE.bit != 0)
                strikethrough = strikethrough || (bitmap and CharacterDecoration.STRIKETHROUGH.bit != 0)
            }
        }
        
        return DecodedCharacter(char, bold, italic, underline, strikethrough)
    }
}

/**
 * Decoded character with decoration information
 */
data class DecodedCharacter(
    val char: Char,
    val bold: Boolean,
    val italic: Boolean,
    val underline: Boolean,
    val strikethrough: Boolean
)

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