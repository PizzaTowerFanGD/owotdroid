package com.owot.android.client.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owot.android.client.data.models.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    
    private lateinit var database: AppDatabase
    private lateinit var tileDao: TileDao
    private lateinit var chatDao: ChatDao
    private lateinit var worldPropertiesDao: WorldPropertiesDao
    private lateinit var userPreferencesDao: UserPreferencesDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        tileDao = database.tileDao()
        chatDao = database.chatDao()
        worldPropertiesDao = database.worldPropertiesDao()
        userPreferencesDao = database.userPreferencesDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun testTileOperations() = runBlocking {
        // Test inserting a tile
        val tileEntity = TileEntity(
            tileKey = "0,0",
            tileX = 0,
            tileY = 0,
            content = "Hello World".toCharArray(),
            writability = 0,
            color = IntArray(128) { android.graphics.Color.BLACK },
            bgColor = IntArray(128) { -1 },
            charWritability = IntArray(128) { -1 },
            cellProps = mutableMapOf(),
            lastModified = System.currentTimeMillis()
        )
        
        tileDao.insertTile(tileEntity)
        
        // Test retrieving the tile
        val retrievedTile = tileDao.getTile(0, 0)
        assert(retrievedTile != null)
        assert(retrievedTile?.tileX == 0)
        assert(retrievedTile?.tileY == 0)
    }
    
    @Test
    fun testChatMessageOperations() = runBlocking {
        // Test inserting a chat message
        val chatMessage = ChatMessageEntity(
            id = "test1",
            nickname = "TestUser",
            message = "Hello World!",
            location = ChatLocation.PAGE,
            timestamp = System.currentTimeMillis()
        )
        
        chatDao.insertMessage(chatMessage)
        
        // Test retrieving messages
        val messages = chatDao.getRecentMessages(10)
        assert(messages.size == 1)
        assert(messages[0].nickname == "TestUser")
    }
    
    @Test
    fun testWorldPropertiesOperations() = runBlocking {
        // Test inserting world properties
        val properties = WorldPropertiesEntity(
            worldName = "test_world",
            backgroundColor = android.graphics.Color.WHITE,
            textColor = android.graphics.Color.BLACK
        )
        
        worldPropertiesDao.insertWorldProperties(properties)
        
        // Test retrieving properties
        val retrieved = worldPropertiesDao.getWorldProperties("test_world")
        assert(retrieved != null)
        assert(retrieved?.worldName == "test_world")
    }
    
    @Test
    fun testUserPreferencesOperations() = runBlocking {
        // Test inserting user preferences
        val preferences = UserPreferencesEntity(
            worldName = "test_world",
            nickname = "TestUser",
            textColor = android.graphics.Color.BLUE
        )
        
        userPreferencesDao.insertUserPreferences(preferences)
        
        // Test retrieving preferences
        val retrieved = userPreferencesDao.getUserPreferences("test_world")
        assert(retrieved != null)
        assert(retrieved?.nickname == "TestUser")
    }
}