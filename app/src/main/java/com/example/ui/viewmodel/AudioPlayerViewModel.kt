package com.example.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.service.PlaybackService
import com.example.ui.components.QueueItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import com.example.data.model.AudioEntity
import android.net.Uri

class AudioPlayerViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {

    var mediaController: MediaController? = null
        private set

    private val prefs = application.getSharedPreferences("playback_state", Context.MODE_PRIVATE)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _currentMediaItemIndex = MutableStateFlow(0)
    val currentMediaItemIndex: StateFlow<Int> = _currentMediaItemIndex

    private val _currentQueue = MutableStateFlow<List<QueueItem>>(emptyList())
    val currentQueue: StateFlow<List<QueueItem>> = _currentQueue

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private val _recentlyPlayed = MutableStateFlow<List<QueueItem>>(emptyList())
    val recentlyPlayed: StateFlow<List<QueueItem>> = _recentlyPlayed

    private val _currentAlbumArtUrl = MutableStateFlow<String?>(null)
    val currentAlbumArtUrl: StateFlow<String?> = _currentAlbumArtUrl

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled

    private var initialized = false

    init {
        // We will initialize state restoration in initializeController if empty 
        // to avoid loading DB queries here.
    }

    fun initializeController(context: Context, playlist: List<AudioEntity>, initialIndex: Int, autoPlay: Boolean = true, restart: Boolean = false) {
        if (playlist.isEmpty()) return

        if (initialized) {
            if (mediaController != null && mediaController?.mediaItemCount == playlist.size) {
                 val existingUri = mediaController?.getMediaItemAt(0)?.localConfiguration?.uri
                 if (existingUri == Uri.parse(playlist[0].path)) {
                     if (mediaController?.currentMediaItemIndex != initialIndex) {
                         mediaController?.seekTo(initialIndex, 0L)
                         if (autoPlay || restart) mediaController?.play()
                     } else if (restart) {
                         mediaController?.seekTo(initialIndex, 0L)
                         mediaController?.play()
                     }
                     return
                 }
            }
        }
        initialized = true

        viewModelScope.launch {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val controller = MediaController.Builder(context, sessionToken).buildAsync().await()
            mediaController = controller

            _volume.value = controller.volume

            var progressJob: kotlinx.coroutines.Job? = null

            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                    _isPlaying.value = isPlayingChange
                    if (isPlayingChange) {
                        progressJob?.cancel()
                        progressJob = viewModelScope.launch {
                            while (_isPlaying.value) {
                                _currentPosition.value = controller.currentPosition
                                _duration.value = controller.duration.coerceAtLeast(0)
                                kotlinx.coroutines.delay(200)
                            }
                        }
                    } else {
                        progressJob?.cancel()
                    }
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    _duration.value = controller.duration.coerceAtLeast(0)
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _currentMediaItemIndex.value = controller.currentMediaItemIndex
                    savePlaybackState(playlist)
                    updateQueue()
                    
                    _currentAlbumArtUrl.value = null
                    mediaItem?.let { item ->
                        viewModelScope.launch {
                            val title = item.mediaMetadata.title?.toString() ?: ""
                            val artist = item.mediaMetadata.artist?.toString() ?: ""
                            val album = item.mediaMetadata.albumTitle?.toString() ?: ""
                            val url = com.example.service.AlbumArtService.fetchAlbumArtUrl(artist, album, title)
                            _currentAlbumArtUrl.value = url
                        }
                        
                        val qItem = QueueItem(
                            uri = item.localConfiguration?.uri.toString(),
                            title = item.mediaMetadata.title?.toString() ?: "Unknown",
                            isPlaying = false 
                        )
                        val newList = _recentlyPlayed.value.toMutableList()
                        newList.removeAll { it.uri == qItem.uri }
                        newList.add(0, qItem)
                        if (newList.size > 10) {
                            newList.removeAt(newList.size - 1)
                        }
                        _recentlyPlayed.value = newList
                    }
                }
                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    updateQueue()
                    savePlaybackState(playlist)
                }
                override fun onVolumeChanged(volume: Float) {
                    _volume.value = volume
                }
                override fun onRepeatModeChanged(repeatMode: Int) {
                    _repeatMode.value = repeatMode
                }
                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _shuffleModeEnabled.value = shuffleModeEnabled
                }
            })

            val mediaItems = playlist.map { file ->
                val metadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(file.title)
                    .setArtist(file.artist)
                    .setAlbumTitle(file.album)
                    .build()
                MediaItem.Builder()
                    .setUri(file.path)
                    .setMediaMetadata(metadata)
                    .build()
            }
            
            controller.setMediaItems(mediaItems)
            controller.seekTo(initialIndex, 0L)
            controller.prepare()
            if (autoPlay) {
                controller.play()
            }
            
            _duration.value = controller.duration.coerceAtLeast(0)
            updateQueue()
        }
    }

    private fun savePlaybackState(playlist: List<AudioEntity>) {
        val paths = playlist.map { it.path }
        val index = mediaController?.currentMediaItemIndex ?: 0
        prefs.edit()
            .putStringSet("playlist_paths", paths.toSet()) // though set loses order, let's keep CSV
            .putString("playlist_paths_csv", paths.joinToString(separator = "||"))
            .putInt("current_index", index)
            .apply()
    }

    fun saveCurrentPosition() {
        if (mediaController != null) {
            prefs.edit().putLong("current_position", mediaController?.currentPosition ?: 0L).apply()
        }
    }

    // Called from MainActivity to check if we can restore state
    fun tryRestoreState(context: Context, fullAudioList: List<AudioEntity>) {
        if (initialized) return
        val csv = prefs.getString("playlist_paths_csv", null)
        if (!csv.isNullOrEmpty()) {
            val paths = csv.split("||")
            val restoredPlaylist = paths.mapNotNull { p -> fullAudioList.find { it.path == p } }
            if (restoredPlaylist.isNotEmpty()) {
                val idx = prefs.getInt("current_index", 0)
                val pos = prefs.getLong("current_position", 0L)
                initializeController(context, restoredPlaylist, idx, autoPlay = false)
                mediaController?.seekTo(idx, pos)
                _currentPosition.value = pos
            }
        }
    }

    private fun updateQueue() {
        val controller = mediaController ?: return
        val count = controller.mediaItemCount
        val currentIndex = controller.currentMediaItemIndex
        val items = mutableListOf<QueueItem>()
        for (i in 0 until count) {
            val mediaItem = controller.getMediaItemAt(i)
            items.add(
                QueueItem(
                    uri = mediaItem.localConfiguration?.uri.toString(),
                    title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                    isPlaying = i == currentIndex
                )
            )
        }
        _currentQueue.value = items
    }

    fun playPause() {
        if (mediaController?.isPlaying == true) {
            mediaController?.pause()
            saveCurrentPosition()
        } else {
            mediaController?.play()
        }
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        saveCurrentPosition()
    }
    
    fun setVolume(volume: Float) {
        mediaController?.volume = volume
        _volume.value = volume
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackParameters(androidx.media3.common.PlaybackParameters(speed))
        _playbackSpeed.value = speed
    }
    
    fun setShuffleModeEnabled(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
        _shuffleModeEnabled.value = enabled
    }

    fun setRepeatMode(repeatMode: Int) {
        mediaController?.repeatMode = repeatMode
        _repeatMode.value = repeatMode
    }
    
    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }
    
    fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun stopPlayback() {
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _isPlaying.value = false
    }

    fun playQueueItem(index: Int) {
        mediaController?.seekTo(index, 0L)
        mediaController?.play()
    }

    fun removeQueueItem(index: Int) {
        mediaController?.removeMediaItem(index)
    }

    fun moveQueueItem(from: Int, to: Int) {
        if (from == to) return
        val finalTo = if (to > from) to + 1 else to
        mediaController?.moveMediaItem(from, finalTo)
    }

    private var sleepTimerJob: kotlinx.coroutines.Job? = null
    private val _sleepTimerRemaining = MutableStateFlow(0L)
    val sleepTimerRemaining: StateFlow<Long> = _sleepTimerRemaining

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemaining.value = 0
            return
        }
        sleepTimerJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            var ms = minutes * 60 * 1000L
            while (ms > 0) {
                _sleepTimerRemaining.value = ms
                kotlinx.coroutines.delay(1000)
                ms -= 1000
            }
            _sleepTimerRemaining.value = 0
            mediaController?.pause()
        }
    }

    private var lastSavedPosition = 0L

    fun updateProgress() {
        mediaController?.let {
            if (it.isPlaying) {
                _currentPosition.value = it.currentPosition
                if (kotlin.math.abs(it.currentPosition - lastSavedPosition) > 5000) {
                    saveCurrentPosition()
                    lastSavedPosition = it.currentPosition
                }
            }
        }
    }

    override fun onCleared() {
        mediaController?.release()
        super.onCleared()
    }
}
