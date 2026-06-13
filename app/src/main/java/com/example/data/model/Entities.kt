package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "videos")
@Serializable
data class VideoEntity(
    @PrimaryKey val path: String,
    val id: Int = 0,
    val title: String,
    val duration: Long,
    val resolution: String,
    val size: Long,
    val lastPlayed: Long
)

@Entity(tableName = "audio")
@Serializable
data class AudioEntity(
    @PrimaryKey val path: String,
    val id: Int = 0,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val albumId: Long = 0L,
    val size: Long = 0L,
    val isLiked: Boolean = false
)

@Entity(tableName = "playlists")
@Serializable
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_audio_cross_ref", primaryKeys = ["playlistId", "audioPath"])
@Serializable
data class PlaylistAudioCrossRef(
    val playlistId: Int,
    val audioPath: String
)

@Entity(tableName = "playback_history")
@Serializable
data class PlaybackHistoryEntity(
    @PrimaryKey val mediaPath: String,
    val mediaType: String,
    val position: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
