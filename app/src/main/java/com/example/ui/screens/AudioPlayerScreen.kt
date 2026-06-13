package com.example.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodels.MediaViewModel
import com.example.ui.viewmodel.AudioPlayerViewModel
import com.example.ui.viewmodels.ThemeManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.foundation.focusable
import com.example.ui.components.PlaybackControlComponent
import com.example.ui.components.MediaQueueSidebar

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    audioPath: String, 
    mediaViewModel: MediaViewModel,
    playerViewModel: AudioPlayerViewModel,
    restart: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val decodedPath = Uri.decode(audioPath)
    
    val audios by mediaViewModel.audios.collectAsStateWithLifecycle()
    
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by playerViewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by playerViewModel.duration.collectAsStateWithLifecycle()
    val currentMediaItemIndex by playerViewModel.currentMediaItemIndex.collectAsStateWithLifecycle()
    val currentQueue by playerViewModel.currentQueue.collectAsStateWithLifecycle()
    val volume by playerViewModel.volume.collectAsStateWithLifecycle()
    
    val enableDynamicBackground by ThemeManager.enableDynamicBackground.collectAsStateWithLifecycle()
    val playerUiTheme by ThemeManager.playerUiTheme.collectAsStateWithLifecycle()

    var showEqualizer by remember { mutableStateOf(false) }
    var selectedEqMode by remember { mutableStateOf("Normal") }
    
    val repeatMode = playerViewModel.repeatMode.collectAsStateWithLifecycle().value
    val isShuffle = playerViewModel.shuffleModeEnabled.collectAsStateWithLifecycle().value
    
    var showPlaylistOptions by remember { mutableStateOf(false) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var showQueueSidebar by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val eqModes = listOf(
        "Normal", "Classical", "Dance", "Flat", "Folk", "Heavy Metal", 
        "Hip Hop", "Jazz", "Pop", "Rock", "Acoustic", "Bass Booster", 
        "Bass Reducer", "Deep", "Electronic", "Latin", "Lounge", 
        "Piano", "R&B", "Spoken Word", "Treble Booster", 
        "Treble Reducer", "Vocal Booster"
    )

    LaunchedEffect(audios, decodedPath, restart) {
        if (audios.isNotEmpty()) {
            val startIndex = audios.indexOfFirst { it.path == decodedPath }.coerceAtLeast(0)
            playerViewModel.initializeController(context, audios, startIndex, restart = restart)
        }
    }

    val ecoModeEnabled by com.example.ui.viewmodels.ThemeManager.ecoModeEnabled.collectAsStateWithLifecycle()
    
    LaunchedEffect(isPlaying, ecoModeEnabled) {
        while (isPlaying) {
            playerViewModel.updateProgress()
            delay(if (ecoModeEnabled) 5000L else 1000L)
        }
    }
    
    val currentAudio = if (audios.isNotEmpty() && currentMediaItemIndex in audios.indices) audios[currentMediaItemIndex] else null
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp) {
                        when (event.key) {
                            Key.Spacebar -> { playerViewModel.playPause(); true }
                            Key.DirectionRight -> { playerViewModel.seekTo(currentPosition + 10000); true }
                            Key.DirectionLeft -> { playerViewModel.seekTo((currentPosition - 10000).coerceAtLeast(0L)); true }
                            else -> false
                        }
                    } else false
                },
            topBar = {
                TopAppBar(
                    title = { Text("Playing") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showQueueSidebar = !showQueueSidebar }) {
                            Icon(Icons.Filled.QueueMusic, contentDescription = "Queue", tint = Color.White)
                        }
                        IconButton(onClick = { showPlaylistOptions = true }) {
                            Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add to Playlist", tint = Color.White)
                        }
                        IconButton(onClick = { showEqualizer = true }) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = "Equalizer", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            var dominantColor by remember { mutableStateOf(Color.Black) }
            val animatedColor by androidx.compose.animation.animateColorAsState(
                targetValue = dominantColor,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = if (ecoModeEnabled) 0 else 1000),
                label = "color_anim"
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                val albumArtUrl by playerViewModel.currentAlbumArtUrl.collectAsStateWithLifecycle()
                val imageRequest = remember(currentAudio, decodedPath, albumArtUrl) {
                    val sArtworkUri = android.net.Uri.parse("content://media/external/audio/albumart")
                    val coverUri = currentAudio?.albumId?.let { android.content.ContentUris.withAppendedId(sArtworkUri, it) }
                    ImageRequest.Builder(context)
                        .data(albumArtUrl ?: coverUri ?: currentAudio?.path ?: decodedPath)
                        .crossfade(true)
                        .allowHardware(false)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onSuccess = { state ->
                        val drawable = state.result.drawable
                        val b = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        if (b != null && enableDynamicBackground) {
                            androidx.palette.graphics.Palette.from(b).generate { p ->
                                p?.dominantSwatch?.rgb?.let { c ->
                                    dominantColor = Color(c)
                                } ?: p?.mutedSwatch?.rgb?.let { c ->
                                    dominantColor = Color(c)
                                }
                            }
                        } else if (b != null && !enableDynamicBackground) {
                            dominantColor = Color(0xFF141414)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .let { m ->
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                m.blur(24.dp)
                            } else m
                        }
                        .background(Color.Black.copy(alpha = 0.5f))
                )
                Box(modifier = Modifier.fillMaxSize().background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            animatedColor.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                ))
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (playerUiTheme) {
                        "Circular Retrograde" -> {
                            com.example.ui.components.CircularPlayerTheme(
                                currentAudio = currentAudio, decodedPath = decodedPath, imageRequest = imageRequest,
                                playerViewModel = playerViewModel, mediaViewModel = mediaViewModel,
                                isPlaying = isPlaying, 
                                isShuffle = isShuffle, repeatMode = repeatMode, currentPosition = currentPosition,
                                duration = duration, volume = volume
                            )
                        }
                        "Purple Glass" -> {
                            com.example.ui.components.PurpleGlassTheme(
                                currentAudio = currentAudio, decodedPath = decodedPath, imageRequest = imageRequest,
                                playerViewModel = playerViewModel, mediaViewModel = mediaViewModel,
                                isPlaying = isPlaying, 
                                isShuffle = isShuffle, repeatMode = repeatMode, currentPosition = currentPosition,
                                duration = duration, volume = volume
                            )
                        }
                        "Transparent Glass" -> {
                            com.example.ui.components.TransparentGlassTheme(
                                currentAudio = currentAudio, decodedPath = decodedPath, imageRequest = imageRequest,
                                playerViewModel = playerViewModel, mediaViewModel = mediaViewModel,
                                isPlaying = isPlaying, isShuffle = isShuffle, repeatMode = repeatMode,
                                currentPosition = currentPosition, duration = duration, volume = volume
                            )
                        }
                        else -> {
                            com.example.ui.components.DefaultPlayerTheme(
                                currentAudio = currentAudio, decodedPath = decodedPath, imageRequest = imageRequest,
                                playerViewModel = playerViewModel, mediaViewModel = mediaViewModel,
                                isPlaying = isPlaying, 
                                isShuffle = isShuffle, repeatMode = repeatMode, currentPosition = currentPosition,
                                duration = duration, volume = volume
                            )
                        }
                    }
                }
            }
            if (showEqualizer) {
                val sessionId by com.example.service.PlaybackService.audioSessionId.collectAsStateWithLifecycle(initialValue = 0)
                com.example.ui.components.EqualizerBottomSheet(
                    audioSessionId = sessionId,
                    onDismissRequest = { showEqualizer = false }
                )
            }

            val allPlaylists by mediaViewModel.playlists.collectAsStateWithLifecycle()

            if (showPlaylistOptions) {
                ModalBottomSheet(onDismissRequest = { showPlaylistOptions = false }) {
                    Column(Modifier.padding(16.dp).padding(bottom = 32.dp).fillMaxWidth()) {
                        Text("Add to Playlist", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                showPlaylistOptions = false
                                showCreatePlaylist = true 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create New Playlist")
                        }
                        Spacer(Modifier.height(16.dp))
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(allPlaylists.size) { index ->
                                val playlist = allPlaylists[index]
                                Text(
                                    text = playlist.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentAudio?.let { audio ->
                                                mediaViewModel.addAudiosToPlaylist(playlist.id, listOf(audio.path))
                                            }
                                            showPlaylistOptions = false
                                        }
                                        .padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (showCreatePlaylist) {
                AlertDialog(
                    onDismissRequest = { showCreatePlaylist = false },
                    title = { Text("Create Playlist") },
                    text = {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("Playlist Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                val audioPaths = currentAudio?.path?.let { listOf(it) } ?: emptyList()
                                mediaViewModel.createPlaylist(newPlaylistName, audioPaths)
                                newPlaylistName = ""
                                showCreatePlaylist = false
                            }
                        }) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreatePlaylist = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            val recentlyPlayed by playerViewModel.recentlyPlayed.collectAsStateWithLifecycle()
            
            MediaQueueSidebar(
                isVisible = showQueueSidebar,
                queue = currentQueue,
                recentlyPlayed = recentlyPlayed,
                onClose = { showQueueSidebar = false },
                onItemClick = { index -> playerViewModel.playQueueItem(index) },
                onRemoveItem = { index -> playerViewModel.removeQueueItem(index) },
                onMoveItem = { from, to -> playerViewModel.moveQueueItem(from, to) },
                onPlayRecent = { uri -> 
                    val idx = audios.indexOfFirst { it.path == uri }
                    if (idx != -1) {
                        playerViewModel.initializeController(context, audios, idx)
                    }
                }
            )
        }
    }
}
