package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Videos : Screen("videos", "Videos", Icons.Filled.PlayArrow)
    object Audio : Screen("audio", "Audio", Icons.Filled.Audiotrack)
    object Playlists : Screen("playlists", "Playlists", Icons.Filled.PlaylistPlay)
    object PlaylistDetail : Screen("playlist_detail/{id}", "Playlist Detail", Icons.Filled.PlaylistPlay) {
        fun createRoute(id: Int) = "playlist_detail/$id"
    }
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object Player : Screen("player/{path}", "Player", Icons.Filled.PlayArrow) {
        fun createRoute(path: String) = "player/${android.net.Uri.encode(path)}"
    }
    object AudioPlayer : Screen("audio_player/{path}?restart={restart}", "Audio Player", Icons.Filled.PlayArrow) {
        fun createRoute(path: String, restart: Boolean = false) = "audio_player/${android.net.Uri.encode(path)}?restart=$restart"
    }
}
