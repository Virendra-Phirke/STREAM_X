package com.example

import android.app.Application
import com.example.di.AppContainer

class StreamXApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        com.example.ui.viewmodels.ThemeManager.initialize(this)
        container = AppContainer(this)
    }
}
