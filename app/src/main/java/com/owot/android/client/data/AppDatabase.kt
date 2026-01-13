package com.owot.android.client.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.owot.android.client.OWOTApplication
import com.owot.android.client.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Room database for OWOT client
 */
@Database(
    entities = [
        TileEntity::class,
        ChatMessageEntity::class,
        WorldPropertiesEntity::class,
        UserPreferencesEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun tileDao(): TileDao
    abstract fun chatDao(): ChatDao
    abstract fun worldPropertiesDao(): WorldPropertiesDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(
            context: OWOTApplication,
            scope: CoroutineScope
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "owot_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(OWOTDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
    
    /**
     * Database callback for initialization tasks
     */
    private class OWOTDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database)
                }
            }
        }
        
        private suspend fun populateDatabase(database: AppDatabase) {
            // Initialize default user preferences
            val userPreferencesDao = database.userPreferencesDao()
            val defaultPreferences = UserPreferencesEntity(
                worldName = "default",
                nickname = "Anonymous",
                textColor = android.graphics.Color.BLACK,
                bgColor = -1,
                fontSize = 14f,
                showGrid = true,
                showCursors = true,
                autoScroll = true,
                chatEnabled = true
            )
            
            try {
                userPreferencesDao.insertUserPreferences(defaultPreferences)
            } catch (e: Exception) {
                // Preferences might already exist, ignore
            }
        }
    }
}