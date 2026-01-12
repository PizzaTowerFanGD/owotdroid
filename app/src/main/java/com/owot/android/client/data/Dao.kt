package com.owot.android.client.data

import androidx.room.*
import com.owot.android.client.data.models.*

/**
 * Tile DAO for database operations
 */
@Dao
interface TileDao {
    
    @Query("SELECT * FROM tiles WHERE tileX = :tileX AND tileY = :tileY")
    suspend fun getTile(tileX: Int, tileY: Int): TileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTile(tile: TileEntity)
    
    @Delete
    suspend fun deleteTile(tile: TileEntity)
    
    @Query("DELETE FROM tiles WHERE tileX = :tileX AND tileY = :tileY")
    suspend fun deleteTileByCoords(tileX: Int, tileY: Int)
    
    @Query("SELECT * FROM tiles WHERE tileX BETWEEN :minX AND :maxX AND tileY BETWEEN :minY AND :maxY")
    suspend fun getTilesInRange(minX: Int, maxX: Int, minY: Int, maxY: Int): List<TileEntity>
    
    @Query("SELECT COUNT(*) FROM tiles")
    suspend fun getTileCount(): Int
    
    @Query("DELETE FROM tiles")
    suspend fun deleteAllTiles()
    
    @Query("DELETE FROM tiles WHERE lastModified < :timestamp")
    suspend fun deleteOldTiles(timestamp: Long)
}

/**
 * Chat message DAO for storing chat history
 */
@Dao
interface ChatDao {
    
    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity>
    
    @Query("SELECT * FROM chat_messages WHERE location = :location ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getMessagesByLocation(location: String, limit: Int): List<ChatMessageEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
    
    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
    
    @Query("DELETE FROM chat_messages WHERE timestamp < :timestamp")
    suspend fun deleteOldMessages(timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getMessageCount(): Int
}

/**
 * World properties DAO
 */
@Dao
interface WorldPropertiesDao {
    
    @Query("SELECT * FROM world_properties WHERE worldName = :worldName")
    suspend fun getWorldProperties(worldName: String): WorldPropertiesEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorldProperties(properties: WorldPropertiesEntity)
    
    @Delete
    suspend fun deleteWorldProperties(properties: WorldPropertiesEntity)
    
    @Query("DELETE FROM world_properties WHERE worldName = :worldName")
    suspend fun deleteWorldPropertiesByName(worldName: String)
}

/**
 * User preferences DAO
 */
@Dao
interface UserPreferencesDao {
    
    @Query("SELECT * FROM user_preferences WHERE worldName = :worldName")
    suspend fun getUserPreferences(worldName: String): UserPreferencesEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPreferences(preferences: UserPreferencesEntity)
    
    @Query("DELETE FROM user_preferences WHERE worldName = :worldName")
    suspend fun deleteUserPreferences(worldName: String)
}