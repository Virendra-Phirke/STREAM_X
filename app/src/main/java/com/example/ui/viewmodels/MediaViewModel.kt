package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AudioEntity
import com.example.data.model.VideoEntity
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MediaViewModel(private val repository: MediaRepository) : ViewModel() {
    
    val videos: StateFlow<List<VideoEntity>> = repository.videos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val audios: StateFlow<List<AudioEntity>> = repository.audios.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playlists: StateFlow<List<com.example.data.model.PlaylistEntity>> = repository.playlists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val likedAudio: StateFlow<List<AudioEntity>> = repository.getLikedAudio().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleAudioLike(path: String, isLiked: Boolean) {
        viewModelScope.launch {
            repository.toggleAudioLike(path, isLiked)
        }
    }

    fun createPlaylist(name: String, audios: List<String> = emptyList()) {
        viewModelScope.launch { 
            val id = repository.createPlaylist(name)
            if (audios.isNotEmpty()) {
                audios.forEach { repository.addAudioToPlaylist(id.toInt(), it) }
            }
        }
    }

    fun updatePlaylistName(id: Int, name: String) {
        viewModelScope.launch { repository.updatePlaylistName(id, name) }
    }

    fun deletePlaylists(ids: List<Int>) {
        viewModelScope.launch { ids.forEach { repository.deletePlaylist(it) } }
    }

    fun addAudiosToPlaylist(playlistId: Int, audioPaths: List<String>) {
        viewModelScope.launch { audioPaths.forEach { repository.addAudioToPlaylist(playlistId, it) } }
    }

    fun removeAudiosFromPlaylist(playlistId: Int, audioPaths: List<String>) {
        viewModelScope.launch { audioPaths.forEach { repository.removeAudioFromPlaylist(playlistId, it) } }
    }

    fun getAudiosForPlaylist(playlistId: Int): kotlinx.coroutines.flow.Flow<List<AudioEntity>> {
        return repository.getAudiosForPlaylist(playlistId)
    }

    private var hasScanned = false

    fun scanMedia() {
        if (hasScanned) return
        viewModelScope.launch {
            hasScanned = true
            repository.scanAndSaveMedia()
        }
    }
    
    fun forceScanMedia() {
        viewModelScope.launch {
            repository.scanAndSaveMedia()
        }
    }

    fun observeMediaChanges(context: android.content.Context) {
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                forceScanMedia()
            }
        }
        context.contentResolver.registerContentObserver(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        context.contentResolver.registerContentObserver(
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
    }
}

class MediaViewModelFactory(private val repository: MediaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
