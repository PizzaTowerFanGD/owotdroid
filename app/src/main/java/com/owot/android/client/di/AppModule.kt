package com.owot.android.client.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.owot.android.client.OWOTApplication
import com.owot.android.client.data.AppDatabase
import com.owot.android.client.network.WebSocketManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Hilt module for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "owot_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    @Singleton
    fun provideWebSocketManager(@ApplicationContext context: Context): WebSocketManager {
        return WebSocketManager(context.applicationContext as OWOTApplication)
    }
    
    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob())
    }
}