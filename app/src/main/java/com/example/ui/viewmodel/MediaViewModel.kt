package com.example.ui.viewmodel

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
    private val _isScanning = kotlinx.coroutines.flow.MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning

    private var contentObserver: android.database.ContentObserver? = null
    private var contentResolver: android.content.ContentResolver? = null
    private var debouncedScanJob: kotlinx.coroutines.Job? = null

    fun scanMedia() {
        if (hasScanned) return
        viewModelScope.launch {
            hasScanned = true
            _isScanning.value = true
            try {
                repository.scanAndSaveMedia()
            } finally {
                _isScanning.value = false
            }
        }
    }
    
    fun forceScanMedia() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.scanAndSaveMedia()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun observeMediaChanges(context: android.content.Context) {
        if (contentObserver != null) return
        val resolver = context.applicationContext.contentResolver
        contentResolver = resolver

        contentObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                scheduleDebouncedScan()
            }

            override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
                super.onChange(selfChange, uri)
                scheduleDebouncedScan()
            }
        }

        contentObserver?.let { observer ->
            try {
                resolver.registerContentObserver(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    true,
                    observer
                )
                resolver.registerContentObserver(
                    android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    true,
                    observer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun scheduleDebouncedScan() {
        debouncedScanJob?.cancel()
        debouncedScanJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            repository.scanAndSaveMedia()
        }
    }

    override fun onCleared() {
        super.onCleared()
        contentObserver?.let { observer ->
            try {
                contentResolver?.unregisterContentObserver(observer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        contentObserver = null
        contentResolver = null
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
