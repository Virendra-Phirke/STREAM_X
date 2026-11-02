package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Videos : Screen("videos", "Videos", Icons.Outlined.PlayCircle)
    object Audio : Screen("audio", "Audio", Icons.Outlined.MusicNote)
    object Playlists : Screen("playlists", "Playlists", Icons.Outlined.QueueMusic)
    object PlaylistDetail : Screen("playlist_detail/{id}", "Playlist Detail", Icons.Outlined.QueueMusic) {
        fun createRoute(id: Int) = "playlist_detail/$id"
    }
    object Settings : Screen("settings", "Settings", Icons.Outlined.Tune)
    object Player : Screen("player/{path}", "Player", Icons.Outlined.PlayCircle) {
        fun createRoute(path: String) = "player/${android.net.Uri.encode(path)}"
    }
    object AudioPlayer : Screen("audio_player/{path}?restart={restart}", "Audio Player", Icons.Outlined.PlayCircle) {
        fun createRoute(path: String, restart: Boolean = false) = "audio_player/${android.net.Uri.encode(path)}?restart=$restart"
    }
}
