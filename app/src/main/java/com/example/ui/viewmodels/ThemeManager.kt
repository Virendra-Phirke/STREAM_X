package com.example.ui.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    val themes = listOf(
        Color(0xFFFFFFFF), // Pure White
        Color(0xFFFF0000), // YouTube Red
        Color(0xFF1DB954), // Spotify Green
        Color(0xFF007AFF), // iOS Blue
        Color(0xFFFF2D55)  // Neon Pink
    )
    private val _primaryColor = MutableStateFlow(themes[0])
    val primaryColor = _primaryColor.asStateFlow()
    
    private val _playerUiTheme = MutableStateFlow("Default")
    val playerUiTheme = _playerUiTheme.asStateFlow()
    
    private val _enableCrossfade = MutableStateFlow(true)
    val enableCrossfade = _enableCrossfade.asStateFlow()
    
    private val _enableDynamicBackground = MutableStateFlow(true)
    val enableDynamicBackground = _enableDynamicBackground.asStateFlow()

    private val _ecoModeEnabled = MutableStateFlow(false)
    val ecoModeEnabled = _ecoModeEnabled.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        
        val savedColor = prefs?.getLong("primary_color", themes[0].value.toLong()) ?: themes[0].value.toLong()
        _primaryColor.value = Color(savedColor.toULong())
        
        _playerUiTheme.value = prefs?.getString("player_ui_theme", "Default") ?: "Default"
        
        _enableCrossfade.value = prefs?.getBoolean("enable_crossfade", true) ?: true
        _enableDynamicBackground.value = prefs?.getBoolean("enable_bg", true) ?: true
        _ecoModeEnabled.value = prefs?.getBoolean("eco_mode", false) ?: false
    }
    
    fun setPrimaryColor(color: Color) {
        _primaryColor.value = color
        prefs?.edit()?.putLong("primary_color", color.value.toLong())?.apply()
    }
    
    fun setPlayerUiTheme(theme: String) {
        _playerUiTheme.value = theme
        prefs?.edit()?.putString("player_ui_theme", theme)?.apply()
    }
    
    fun setEnableCrossfade(enabled: Boolean) {
        _enableCrossfade.value = enabled
        prefs?.edit()?.putBoolean("enable_crossfade", enabled)?.apply()
    }
    
    fun setEnableDynamicBackground(enabled: Boolean) {
        _enableDynamicBackground.value = enabled
        prefs?.edit()?.putBoolean("enable_bg", enabled)?.apply()
    }
    
    fun setEcoModeEnabled(enabled: Boolean) {
        _ecoModeEnabled.value = enabled
        prefs?.edit()?.putBoolean("eco_mode", enabled)?.apply()
    }
    
    fun resetSettings() {
        setPrimaryColor(themes[0])
        setEnableCrossfade(true)
        setEnableDynamicBackground(true)
        setEcoModeEnabled(false)
    }
}
