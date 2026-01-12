package com.owot.android.client.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.owot.android.client.data.models.*

/**
 * Type converters for Room database
 */
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromCharArray(value: CharArray): String {
        return String(value)
    }
    
    @TypeConverter
    fun toCharArray(value: String): CharArray {
        return value.toCharArray()
    }
    
    @TypeConverter
    fun fromIntArray(value: IntArray): String {
        return value.joinToString(",")
    }
    
    @TypeConverter
    fun toIntArray(value: String): IntArray {
        return if (value.isEmpty()) {
            IntArray(0)
        } else {
            value.split(",").map { it.toInt() }.toIntArray()
        }
    }
    
    @TypeConverter
    fun fromStringSet(value: Set<String>): String {
        return value.joinToString(",")
    }
    
    @TypeConverter
    fun toStringSet(value: String): Set<String> {
        return if (value.isEmpty()) {
            emptySet()
        } else {
            value.split(",").toSet()
        }
    }
    
    @TypeConverter
    fun fromMapStringCellProperties(value: MutableMap<Int, CellProperties>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toMapStringCellProperties(value: String): MutableMap<Int, CellProperties> {
        return try {
            val type = object : TypeToken<MutableMap<Int, CellProperties>>() {}.type
            gson.fromJson(value, type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }
    
    @TypeConverter
    fun fromLinkType(value: LinkType): String {
        return value.name
    }
    
    @TypeConverter
    fun toLinkType(value: String): LinkType {
        return try {
            LinkType.valueOf(value)
        } catch (e: Exception) {
            LinkType.URL
        }
    }
    
    @TypeConverter
    fun fromProtectionType(value: ProtectionType): String {
        return value.value
    }
    
    @TypeConverter
    fun toProtectionType(value: String): ProtectionType {
        return try {
            ProtectionType.values().firstOrNull { it.value == value } ?: ProtectionType.PUBLIC
        } catch (e: Exception) {
            ProtectionType.PUBLIC
        }
    }
    
    @TypeConverter
    fun fromChatLocation(value: ChatLocation): String {
        return value.value
    }
    
    @TypeConverter
    fun toChatLocation(value: String): ChatLocation {
        return try {
            ChatLocation.values().firstOrNull { it.value == value } ?: ChatLocation.PAGE
        } catch (e: Exception) {
            ChatLocation.PAGE
        }
    }
}

/**
 * Tile entity for Room database
 */
@Entity(tableName = "tiles")
@TypeConverters(Converters::class)
data class TileEntity(
    @PrimaryKey
    val tileKey: String, // "tileX,tileY"
    val tileX: Int,
    val tileY: Int,
    val content: CharArray,
    val writability: Int,
    val color: IntArray,
    val bgColor: IntArray,
    val charWritability: IntArray,
    val cellProps: MutableMap<Int, CellProperties>,
    val lastModified: Long
) {
    fun toTile(): Tile {
        return Tile(
            tileX = tileX,
            tileY = tileY,
            content = content,
            properties = TileProperties(
                writability = writability,
                color = color,
                bgColor = bgColor,
                charWritability = charWritability,
                cellProps = cellProps
            ),
            lastModified = lastModified
        )
    }
    
    companion object {
        fun fromTile(tile: Tile): TileEntity {
            return TileEntity(
                tileKey = "${tile.tileX},${tile.tileY}",
                tileX = tile.tileX,
                tileY = tile.tileY,
                content = tile.content,
                writability = tile.properties.writability,
                color = tile.properties.color,
                bgColor = tile.properties.bgColor,
                charWritability = tile.properties.charWritability,
                cellProps = tile.properties.cellProps,
                lastModified = tile.lastModified
            )
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as TileEntity
        
        if (tileKey != other.tileKey) return false
        if (tileX != other.tileX) return false
        if (tileY != other.tileY) return false
        if (!content.contentEquals(other.content)) return false
        if (writability != other.writability) return false
        if (!color.contentEquals(other.color)) return false
        if (!bgColor.contentEquals(other.bgColor)) return false
        if (!charWritability.contentEquals(other.charWritability)) return false
        if (cellProps != other.cellProps) return false
        if (lastModified != other.lastModified) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = tileKey.hashCode()
        result = 31 * result + tileX
        result = 31 * result + tileY
        result = 31 * result + content.contentHashCode()
        result = 31 * result + writability
        result = 31 * result + color.contentHashCode()
        result = 31 * result + bgColor.contentHashCode()
        result = 31 * result + charWritability.contentHashCode()
        result = 31 * result + cellProps.hashCode()
        result = 31 * result + lastModified.hashCode()
        return result
    }
}

/**
 * Chat message entity for Room database
 */
@Entity(tableName = "chat_messages")
@TypeConverters(Converters::class)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val nickname: String,
    val message: String,
    val location: ChatLocation,
    val color: Int? = null,
    val isOp: Boolean = false,
    val isAdmin: Boolean = false,
    val isStaff: Boolean = false,
    val timestamp: Long,
    val realUsername: String? = null
) {
    fun toChatResponse(): ChatResponse {
        return ChatResponse(
            kind = "chat",
            location = location,
            id = id,
            type = "user",
            nickname = nickname,
            message = message,
            realUsername = realUsername,
            isOp = isOp,
            isAdmin = isAdmin,
            isStaff = isStaff,
            color = color,
            date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(timestamp)),
            dataObj = null
        )
    }
    
    companion object {
        fun fromChatResponse(response: ChatResponse): ChatMessageEntity {
            return ChatMessageEntity(
                id = response.id,
                nickname = response.nickname,
                message = response.message,
                location = response.location,
                color = response.color,
                isOp = response.isOp,
                isAdmin = response.isAdmin,
                isStaff = response.isStaff,
                timestamp = parseDate(response.date),
                realUsername = response.realUsername
            )
        }
        
        private fun parseDate(dateString: String): Long {
            return try {
                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                val date = format.parse(dateString)
                date?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}

/**
 * World properties entity for Room database
 */
@Entity(tableName = "world_properties")
@TypeConverters(Converters::class)
data class WorldPropertiesEntity(
    @PrimaryKey
    val worldName: String,
    val creationDate: Long? = null,
    val views: Long = 0,
    val isMember: Boolean = false,
    val writability: Int = TileProperties.WRITABILITY_PUBLIC,
    val backgroundColor: Int = android.graphics.Color.WHITE,
    val textColor: Int = android.graphics.Color.BLACK,
    val gridColor: Int = android.graphics.Color.GRAY,
    val cursorColor: Int = android.graphics.Color.BLUE,
    val linkColor: Int = android.graphics.Color.BLUE,
    val fontSize: Float = 14f,
    val fontFamily: String = "monospace",
    val features: Set<String> = emptySet(),
    val charRate: Int = 100,
    val maxEditSize: Int = 1000,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toWorldModel(): WorldModel {
        return WorldModel(
            name = worldName,
            creationDate = creationDate?.let { java.util.Date(it) },
            views = views,
            isMember = isMember,
            writability = writability,
            theme = WorldTheme(
                backgroundColor = backgroundColor,
                textColor = textColor,
                gridColor = gridColor,
                cursorColor = cursorColor,
                linkColor = linkColor,
                fontSize = fontSize,
                fontFamily = fontFamily
            ),
            features = features,
            charRate = charRate,
            maxEditSize = maxEditSize
        )
    }
    
    companion object {
        fun fromWorldModel(world: WorldModel): WorldPropertiesEntity {
            return WorldPropertiesEntity(
                worldName = world.name,
                creationDate = world.creationDate?.time,
                views = world.views,
                isMember = world.isMember,
                writability = world.writability,
                backgroundColor = world.theme.backgroundColor,
                textColor = world.theme.textColor,
                gridColor = world.theme.gridColor,
                cursorColor = world.theme.cursorColor,
                linkColor = world.theme.linkColor,
                fontSize = world.theme.fontSize,
                fontFamily = world.theme.fontFamily,
                features = world.features,
                charRate = world.charRate,
                maxEditSize = world.maxEditSize,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
}

/**
 * User preferences entity for Room database
 */
@Entity(tableName = "user_preferences")
@TypeConverters(Converters::class)
data class UserPreferencesEntity(
    @PrimaryKey
    val worldName: String,
    val nickname: String = "Anonymous",
    val textColor: Int = android.graphics.Color.BLACK,
    val bgColor: Int = -1,
    val fontSize: Float = 14f,
    val showGrid: Boolean = true,
    val showCursors: Boolean = true,
    val autoScroll: Boolean = true,
    val chatEnabled: Boolean = true,
    val lastUsed: Long = System.currentTimeMillis()
) {
    fun toUserModel(): UserModel {
        return UserModel(
            username = nickname,
            canWrite = true,
            canColorText = true
        )
    }
    
    companion object {
        fun fromUserModel(worldName: String, user: UserModel): UserPreferencesEntity {
            return UserPreferencesEntity(
                worldName = worldName,
                nickname = user.username
            )
        }
    }
}