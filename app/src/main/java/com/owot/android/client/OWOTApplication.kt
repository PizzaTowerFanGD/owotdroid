package com.owot.android.client

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.work.Configuration
import com.owot.android.client.data.AppDatabase
import com.owot.android.client.network.WebSocketManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory

@HiltAndroidApp
class OWOTApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    // Database for local storage
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "owot_database"
        ).fallbackToDestructiveMigration().build()
    }
    
    // Global coroutine scope
    val applicationScope = CoroutineScope(SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
    
    companion object {
        lateinit var instance: OWOTApplication
            private set
            
        fun getContext(): Context = instance.applicationContext
    }
}