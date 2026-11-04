package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.db.StreamXDatabase
import com.example.data.repository.MediaRepository
import com.example.service.MediaScannerService

/** Manages dependencies for the application manually */
class AppContainer(private val context: Context) {
    val database = Room.databaseBuilder(
        context,
        StreamXDatabase::class.java,
        "streamx.db"
    ).build()

    val mediaScannerService = MediaScannerService(context)
    val mediaRepository = MediaRepository(database, mediaScannerService)
    val sharedPreferences = context.getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)
}

// Extension to get the container from context
val Context.appContainer: AppContainer
    get() = (applicationContext as com.example.StreamXApplication).container
