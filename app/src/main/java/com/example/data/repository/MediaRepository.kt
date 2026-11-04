package com.example.data.repository

import com.example.data.db.StreamXDatabase
import com.example.data.model.AudioEntity
import com.example.data.model.VideoEntity
import com.example.service.MediaScannerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MediaRepository(
    private val database: StreamXDatabase,
    private val scannerService: MediaScannerService
) {
    val videos: Flow<List<VideoEntity>> = database.videoDao.getAllVideos()
    val audios: Flow<List<AudioEntity>> = database.audioDao.getAllAudio()
    val playlists: Flow<List<com.example.data.model.PlaylistEntity>> = database.playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String): Long {
        return database.playlistDao.insertPlaylist(com.example.data.model.PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(id: Int) {
        database.playlistDao.deletePlaylist(id)
    }

    suspend fun updatePlaylistName(id: Int, name: String) {
        database.playlistDao.updatePlaylistName(id, name)
    }

    suspend fun addAudioToPlaylist(playlistId: Int, audioPath: String) {
        database.playlistDao.insertPlaylistAudioItem(
            com.example.data.model.PlaylistAudioCrossRef(playlistId, audioPath)
        )
    }

    suspend fun removeAudioFromPlaylist(playlistId: Int, audioPath: String) {
        database.playlistDao.deletePlaylistAudioItem(playlistId, audioPath)
    }

    fun getAudiosForPlaylist(playlistId: Int): Flow<List<AudioEntity>> {
        return database.playlistDao.getAudiosForPlaylist(playlistId)
    }

    suspend fun scanAndSaveMedia() {
        // scan devices for media
        val scannedVideos = scannerService.scanVideos()
        val scannedAudio = scannerService.scanAudio()

        // Preserve liked status for audio
        val currentLiked = database.audioDao.getLikedAudio().firstOrNull() ?: emptyList()
        val likedPaths = currentLiked.map { it.path }.toSet()

        val audioToInsert = scannedAudio.map { 
            if (likedPaths.contains(it.path)) {
                it.copy(isLiked = true)
            } else {
                it
            }
        }

        // Atomic database sync
        database.videoDao.syncVideos(scannedVideos)
        database.audioDao.syncAudio(audioToInsert)
    }

    suspend fun toggleAudioLike(path: String, isLiked: Boolean) {
        database.audioDao.updateLikedState(path, isLiked)
    }

    fun getLikedAudio(): Flow<List<AudioEntity>> = database.audioDao.getLikedAudio()
    
    suspend fun savePlaybackHistory(mediaPath: String, type: String, position: Long) {
        database.playbackHistoryDao.insertHistory(
            com.example.data.model.PlaybackHistoryEntity(
                mediaPath = mediaPath,
                mediaType = type,
                position = position
            )
        )
    }
    
    suspend fun getPlaybackPosition(mediaPath: String, type: String): Long {
        return database.playbackHistoryDao.getPosition(mediaPath, type) ?: 0L
    }
}
