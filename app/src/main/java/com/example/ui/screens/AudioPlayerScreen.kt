package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.components.CircularPlayerTheme
import com.example.ui.components.DefaultPlayerTheme
import com.example.ui.components.EqualizerBottomSheet
import com.example.ui.components.MediaQueueSidebar
import com.example.ui.components.PurpleGlassTheme
import com.example.ui.components.TransparentGlassTheme
import com.example.ui.viewmodel.AudioPlayerViewModel
import com.example.ui.viewmodels.MediaViewModel
import com.example.ui.viewmodels.ThemeManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
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
    var showLyricsDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    
    val repeatMode = playerViewModel.repeatMode.collectAsStateWithLifecycle().value
    val isShuffle = playerViewModel.shuffleModeEnabled.collectAsStateWithLifecycle().value
    
    var showPlaylistOptions by remember { mutableStateOf(false) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var showQueueSidebar by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    LaunchedEffect(audios, decodedPath, restart) {
        if (audios.isNotEmpty()) {
            val startIndex = audios.indexOfFirst { it.path == decodedPath }.coerceAtLeast(0)
            playerViewModel.initializeController(context, audios, startIndex, restart = restart)
        }
    }

    val ecoModeEnabled by ThemeManager.ecoModeEnabled.collectAsStateWithLifecycle()
    val sleepTimerRemaining by playerViewModel.sleepTimerRemaining.collectAsStateWithLifecycle()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsStateWithLifecycle()

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
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PLAYING FROM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF92999D)
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF1C1E20),
                                border = BorderStroke(1.dp, Color(0xFF33383A)),
                                onClick = { showQueueSidebar = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlaylistPlay,
                                        contentDescription = null,
                                        tint = Color(0xFFF0B849),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentAudio?.album?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Now Playing Queue",
                                        color = Color(0xFFE8E8E6),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFE8E8E6))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More Options", tint = Color(0xFFE8E8E6))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color(0xFFE8E8E6)
                    )
                )
            },
            containerColor = Color(0xFF0D0F10)
        ) { padding ->
            var dominantColor by remember { mutableStateOf(Color.Black) }
            val animatedColor by androidx.compose.animation.animateColorAsState(
                targetValue = dominantColor,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = if (ecoModeEnabled) 0 else 1000),
                label = "color_anim"
            )

            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0F10))) {
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
                                p?.dominantSwatch?.rgb?.let { c -> dominantColor = Color(c) }
                                    ?: p?.mutedSwatch?.rgb?.let { c -> dominantColor = Color(c) }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .let { m ->
                            if (!ecoModeEnabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                m.blur(28.dp)
                            } else m
                        }
                        .background(Color.Black.copy(alpha = 0.5f))
                )

                Box(
                    modifier = Modifier.fillMaxSize().background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                animatedColor.copy(alpha = 0.45f),
                                Color(0xFF0D0F10).copy(alpha = 0.96f)
                            )
                        )
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    // Center Player Theme Container based on User Theme Preference
                    when (playerUiTheme) {
                        "Circular Retrograde" -> CircularPlayerTheme(
                            currentAudio = currentAudio,
                            decodedPath = decodedPath,
                            imageRequest = imageRequest,
                            playerViewModel = playerViewModel,
                            mediaViewModel = mediaViewModel,
                            isPlaying = isPlaying,
                            isShuffle = isShuffle,
                            repeatMode = repeatMode,
                            currentPosition = currentPosition,
                            duration = duration,
                            volume = volume,
                            onLyricsClick = { showLyricsDialog = true },
                            onEqualizerClick = { showEqualizer = true },
                            onQueueClick = { showQueueSidebar = true },
                            onSleepTimerClick = { showSleepTimerDialog = true },
                            onSpeedClick = { showSpeedDialog = true }
                        )
                        "Purple Glass" -> PurpleGlassTheme(
                            currentAudio = currentAudio,
                            decodedPath = decodedPath,
                            imageRequest = imageRequest,
                            playerViewModel = playerViewModel,
                            mediaViewModel = mediaViewModel,
                            isPlaying = isPlaying,
                            isShuffle = isShuffle,
                            repeatMode = repeatMode,
                            currentPosition = currentPosition,
                            duration = duration,
                            volume = volume,
                            onLyricsClick = { showLyricsDialog = true },
                            onEqualizerClick = { showEqualizer = true },
                            onQueueClick = { showQueueSidebar = true },
                            onSleepTimerClick = { showSleepTimerDialog = true },
                            onSpeedClick = { showSpeedDialog = true }
                        )
                        "Transparent Glass" -> TransparentGlassTheme(
                            currentAudio = currentAudio,
                            decodedPath = decodedPath,
                            imageRequest = imageRequest,
                            playerViewModel = playerViewModel,
                            mediaViewModel = mediaViewModel,
                            isPlaying = isPlaying,
                            isShuffle = isShuffle,
                            repeatMode = repeatMode,
                            currentPosition = currentPosition,
                            duration = duration,
                            volume = volume,
                            onLyricsClick = { showLyricsDialog = true },
                            onEqualizerClick = { showEqualizer = true },
                            onQueueClick = { showQueueSidebar = true },
                            onSleepTimerClick = { showSleepTimerDialog = true },
                            onSpeedClick = { showSpeedDialog = true }
                        )
                        else -> DefaultPlayerTheme(
                            currentAudio = currentAudio,
                            decodedPath = decodedPath,
                            imageRequest = imageRequest,
                            playerViewModel = playerViewModel,
                            mediaViewModel = mediaViewModel,
                            isPlaying = isPlaying,
                            isShuffle = isShuffle,
                            repeatMode = repeatMode,
                            currentPosition = currentPosition,
                            duration = duration,
                            volume = volume,
                            onLyricsClick = { showLyricsDialog = true },
                            onEqualizerClick = { showEqualizer = true },
                            onQueueClick = { showQueueSidebar = true },
                            onSleepTimerClick = { showSleepTimerDialog = true },
                            onSpeedClick = { showSpeedDialog = true }
                        )
                    }
                }
            }

            // Equalizer Sheet
            if (showEqualizer) {
                val sessionId by com.example.service.PlaybackService.audioSessionId.collectAsStateWithLifecycle(initialValue = 0)
                EqualizerBottomSheet(
                    audioSessionId = sessionId,
                    onDismissRequest = { showEqualizer = false }
                )
            }

            // Lyrics Modal Dialog
            if (showLyricsDialog) {
                AlertDialog(
                    onDismissRequest = { showLyricsDialog = false },
                    containerColor = Color(0xFF161819),
                    titleContentColor = Color(0xFFE8E8E6),
                    textContentColor = Color(0xFF92999D),
                    title = { Text("Track Lyrics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎶 Synchronized lyrics for this track will be fetched dynamically.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF92999D)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLyricsDialog = false }) {
                            Text("Close", color = Color(0xFFF0B849))
                        }
                    }
                )
            }

            // Sleep Timer Picker Modal
            if (showSleepTimerDialog) {
                AlertDialog(
                    onDismissRequest = { showSleepTimerDialog = false },
                    containerColor = Color(0xFF161819),
                    titleContentColor = Color(0xFFE8E8E6),
                    title = { Text("Sleep Timer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            val options = listOf(0 to "Turn Off", 15 to "15 minutes", 30 to "30 minutes", 45 to "45 minutes", 60 to "60 minutes", 90 to "90 minutes")
                            options.forEach { (mins, label) ->
                                TextButton(
                                    onClick = {
                                        playerViewModel.setSleepTimer(mins)
                                        showSleepTimerDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = label,
                                        color = if (mins == 0 && sleepTimerRemaining <= 0) Color(0xFF92999D) else Color(0xFFE8E8E6),
                                        fontSize = 16.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSleepTimerDialog = false }) {
                            Text("Cancel", color = Color(0xFF92999D))
                        }
                    }
                )
            }

            // Playback Speed Selector Modal
            if (showSpeedDialog) {
                AlertDialog(
                    onDismissRequest = { showSpeedDialog = false },
                    containerColor = Color(0xFF161819),
                    titleContentColor = Color(0xFFE8E8E6),
                    title = { Text("Playback Speed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            speeds.forEach { speed ->
                                TextButton(
                                    onClick = {
                                        playerViewModel.setPlaybackSpeed(speed)
                                        showSpeedDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${speed}x ${if (speed == 1.0f) "(Normal)" else ""}",
                                        color = if (playbackSpeed == speed) Color(0xFFF0B849) else Color(0xFFE8E8E6),
                                        fontSize = 16.sp,
                                        fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSpeedDialog = false }) {
                            Text("Cancel", color = Color(0xFF92999D))
                        }
                    }
                )
            }

            // More Options Sheet
            if (showMoreMenu) {
                ModalBottomSheet(
                    onDismissRequest = { showMoreMenu = false },
                    containerColor = Color(0xFF161819)
                ) {
                    Column(Modifier.padding(20.dp).fillMaxWidth()) {
                        Text("Track Options", style = MaterialTheme.typography.titleLarge, color = Color(0xFFE8E8E6), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))

                        TextButton(
                            onClick = {
                                showMoreMenu = false
                                showPlaylistOptions = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFFF0B849))
                                Spacer(Modifier.width(12.dp))
                                Text("Add to Playlist", color = Color(0xFFE8E8E6), fontSize = 16.sp)
                            }
                        }

                        TextButton(
                            onClick = {
                                showMoreMenu = false
                                showSleepTimerDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = Color(0xFFF0B849))
                                Spacer(Modifier.width(12.dp))
                                Text("Sleep Timer", color = Color(0xFFE8E8E6), fontSize = 16.sp)
                            }
                        }

                        TextButton(
                            onClick = {
                                showMoreMenu = false
                                showSpeedDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Speed, contentDescription = null, tint = Color(0xFFF0B849))
                                Spacer(Modifier.width(12.dp))
                                Text("Playback Speed", color = Color(0xFFE8E8E6), fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            val allPlaylists by mediaViewModel.playlists.collectAsStateWithLifecycle()

            if (showPlaylistOptions) {
                ModalBottomSheet(
                    onDismissRequest = { showPlaylistOptions = false },
                    containerColor = Color(0xFF161819)
                ) {
                    Column(Modifier.padding(20.dp).padding(bottom = 32.dp).fillMaxWidth()) {
                        Text("Add to Playlist", style = MaterialTheme.typography.titleLarge, color = Color(0xFFE8E8E6), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                showPlaylistOptions = false
                                showCreatePlaylist = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0B849)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Create New Playlist", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(16.dp))
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(allPlaylists.size) { index ->
                                val playlist = allPlaylists[index]
                                Text(
                                    text = playlist.name,
                                    color = Color(0xFFE8E8E6),
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
                    containerColor = Color(0xFF161819),
                    titleContentColor = Color(0xFFE8E8E6),
                    textContentColor = Color(0xFF92999D),
                    title = { Text("Create Playlist") },
                    text = {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("Playlist Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE8E8E6),
                                unfocusedTextColor = Color(0xFFE8E8E6),
                                focusedBorderColor = Color(0xFFF0B849),
                                unfocusedBorderColor = Color(0xFF25292A)
                            ),
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
                            Text("Create", color = Color(0xFFF0B849))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreatePlaylist = false }) {
                            Text("Cancel", color = Color(0xFF92999D))
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
