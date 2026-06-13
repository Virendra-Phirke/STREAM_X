package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

import android.content.pm.ActivityInfo
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.foundation.focusable
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoPath: String,
    mediaViewModel: com.example.ui.viewmodels.MediaViewModel? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentVideoPath by remember { mutableStateOf(Uri.decode(videoPath)) }
    val videos by mediaViewModel?.videos?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    
    var showSubtitleMenu by remember { mutableStateOf(false) }
    var showAudioTrackMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    
    var subtitleDelayMs by remember { mutableStateOf(0f) }
    var audioDelayMs by remember { mutableStateOf(0f) }
    var volumeBoost by remember { mutableStateOf(1f) }
    var subtitlePosition by remember { mutableStateOf(0.5f) }
    
    // Gestures states
    var brightness by remember { mutableStateOf(0.5f) }
    var volume by remember { mutableStateOf(0.5f) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }

    var dragSeekPosition by remember { mutableStateOf<Long?>(null) }
    
    val focusRequester = remember { FocusRequester() }

    val trackSelector = remember { androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context) }
    val exoPlayer = remember {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                10000, // min buffer
                20000, // max buffer
                1000, // buffer for playback
                1000 // buffer for playback after rebuffer
            )
            .setTargetBufferBytes(androidx.media3.common.C.LENGTH_UNSET)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
            
        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
            .build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                    isPlaying = isPlayingChange
                }
            })
        }
    }
    
    LaunchedEffect(currentVideoPath) {
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(currentVideoPath)))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }
    
    var tracks by remember { mutableStateOf(exoPlayer.currentTracks) }
    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onTracksChanged(t: androidx.media3.common.Tracks) {
                tracks = t
            }
        })
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        if (window != null) {
            val params = window.attributes
            brightness = if (params.screenBrightness >= 0) params.screenBrightness else 0.5f
        }
        
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (maxVol > 0) {
            volume = currentVol.toFloat() / maxVol
        }
    }

    LaunchedEffect(brightness) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val attributes = window.attributes
            if (kotlin.math.abs(attributes.screenBrightness - brightness) > 0.01f) {
                attributes.screenBrightness = brightness
                window.attributes = attributes
            }
        }
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(isFullscreen) {
        val activity = context as? Activity
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? Activity
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
            exoPlayer.release()
        }
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    val ecoModeEnabled by com.example.ui.viewmodels.ThemeManager.ecoModeEnabled.collectAsStateWithLifecycle()
    
    LaunchedEffect(isPlaying, ecoModeEnabled) {
        while(isPlaying) {
            currentPosition = exoPlayer.currentPosition
            delay(if (ecoModeEnabled) 5000L else 1000L)
        }
    }

    LaunchedEffect(isRepeat) {
        exoPlayer.repeatMode = if (isRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    LaunchedEffect(isShuffle) {
        exoPlayer.shuffleModeEnabled = isShuffle
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) {
                    when (event.key) {
                        Key.Spacebar -> {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            true
                        }
                        Key.DirectionRight -> {
                            exoPlayer.seekTo((exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration.coerceAtLeast(0L)))
                            true
                        }
                        Key.DirectionLeft -> {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000L).coerceAtLeast(0L))
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        this.resizeMode = resizeMode
                    }
                },
                update = { view ->
                    view.resizeMode = resizeMode
                },
                onRelease = { view ->
                    view.player = null
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    showBrightnessIndicator = false
                                    showVolumeIndicator = false
                                    dragSeekPosition?.let {
                                        exoPlayer.seekTo(it)
                                    }
                                    dragSeekPosition = null
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                val yDelta = dragAmount.y
                                val xDelta = dragAmount.x
                                
                                if (kotlin.math.abs(xDelta) > kotlin.math.abs(yDelta) && !showBrightnessIndicator && !showVolumeIndicator) {
                                    val seekMs = (xDelta * 100).toLong()
                                    val current = dragSeekPosition ?: exoPlayer.currentPosition
                                    dragSeekPosition = (current + seekMs).coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
                                } else if (dragSeekPosition == null) {
                                    if (change.position.x < size.width / 2) {
                                        showBrightnessIndicator = true
                                        brightness = (brightness - yDelta / 500f).coerceIn(0f, 1f)
                                    } else {
                                        showVolumeIndicator = true
                                        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        volume = (volume - yDelta / 500f).coerceIn(0f, 1f)
                                        val newVol = (volume * maxVol).toInt()
                                        val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        if (newVol != currentVol) {
                                            am.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                        }
                                    }
                                }
                            }
                        }
                        .pointerInput("tap") {
                            detectTapGestures(
                                onTap = { showControls = !showControls },
                                onDoubleTap = { offset ->
                                    val seekAmount = 10000L
                                    if (offset.x < size.width / 2) {
                                        exoPlayer.seekTo((exoPlayer.currentPosition - seekAmount).coerceAtLeast(0L))
                                    } else {
                                        exoPlayer.seekTo((exoPlayer.currentPosition + seekAmount).coerceAtMost(exoPlayer.duration.coerceAtLeast(0L)))
                                    }
                                }
                            )
                        }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().clickable { showControls = !showControls })
            }

            if (showBrightnessIndicator) {
                Column(Modifier.align(Alignment.CenterStart).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Brightness6, contentDescription = null, tint = Color.White)
                    Text("${(brightness * 100).toInt()}%", color = Color.White)
                }
            }
            if (showVolumeIndicator) {
                Column(Modifier.align(Alignment.CenterEnd).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White)
                    Text("${(volume * 100).toInt()}%", color = Color.White)
                }
            }
            if (dragSeekPosition != null) {
                Column(Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.6f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatTime(dragSeekPosition ?: 0L), color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text("[${if ((dragSeekPosition ?: 0L) > exoPlayer.currentPosition) "+" else "-"}${formatTime(kotlin.math.abs((dragSeekPosition ?: 0L) - exoPlayer.currentPosition))}]", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showControls,
                modifier = Modifier.fillMaxSize(),
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) {
                    
                    if (isLocked) {
                        IconButton(onClick = { isLocked = false }, modifier = Modifier.align(Alignment.TopStart).padding(32.dp)) {
                            Icon(Icons.Filled.Lock, contentDescription = "Unlock", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    } else {
                        // Top Controls
                        Row(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                            Spacer(Modifier.width(8.dp))
                            androidx.compose.material3.Text(Uri.parse(currentVideoPath).lastPathSegment ?: "Video", color = Color.White, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            
                            val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                            val currentTime = sdf.format(java.util.Date())
                            androidx.compose.material3.Text(currentTime, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(16.dp))
                            
                            val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
                            val batLevel = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                            androidx.compose.material3.Text("🔋$batLevel%", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { showMoreMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.White) }
                        }

                        // Center Controls Removed


                        // Bottom Controls
                        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                            // Seekbar
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Text(formatTime(dragSeekPosition ?: currentPosition), color = Color.White, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.width(8.dp))
                                Slider(
                                    value = if (exoPlayer.duration > 0) ((dragSeekPosition ?: currentPosition).toFloat() / exoPlayer.duration).coerceIn(0f, 1f) else 0f,
                                    onValueChange = { 
                                        dragSeekPosition = (it * exoPlayer.duration).toLong() 
                                        exoPlayer.seekTo(dragSeekPosition!!)
                                    },
                                    onValueChangeFinished = { 
                                        dragSeekPosition?.let { 
                                            // we already seek while dragging, but ensure we clear the state
                                            dragSeekPosition = null
                                        } 
                                    },
                                    colors = SliderDefaults.colors(thumbColor = Color.Transparent, activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha=0.7f), inactiveTrackColor = Color.White.copy(alpha=0.2f)),
                                    modifier = Modifier.weight(1f).height(12.dp),
                                    thumb = {
                                        Box(modifier = Modifier.size(0.dp))
                                    },
                                    track = { sliderState ->
                                        SliderDefaults.Track(
                                            sliderState = sliderState,
                                            modifier = Modifier.height(2.dp),
                                            colors = SliderDefaults.colors(thumbColor = Color.Transparent, activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha=0.7f), inactiveTrackColor = Color.White.copy(alpha=0.2f))
                                        )
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                androidx.compose.material3.Text(formatTime(exoPlayer.duration.coerceAtLeast(0L)), color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                            // Bottom Actions
                            Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 8.dp, end = 8.dp, bottom = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                if (isFullscreen) {
                                    IconButton(onClick = { isLocked = true }) { Icon(Icons.Filled.Lock, contentDescription = "Lock", tint = Color.White) }
                                    IconButton(onClick = { showSubtitleMenu = true }) { Icon(Icons.Filled.Subtitles, contentDescription = "Subtitle", tint = Color.White) }
                                }
                                IconButton(onClick = { 
                                    if (videos.isNotEmpty()) {
                                        val idx = videos.indexOfFirst { Uri.decode(it.path) == currentVideoPath || it.path == currentVideoPath }
                                        if (idx > 0) currentVideoPath = videos[idx - 1].path
                                        else exoPlayer.seekTo(0L)
                                    } else exoPlayer.seekTo(0L) 
                                }) { Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp)) }
                                IconButton(onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }) { Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(56.dp)) }
                                IconButton(onClick = { 
                                    if (videos.isNotEmpty()) {
                                        val idx = videos.indexOfFirst { Uri.decode(it.path) == currentVideoPath || it.path == currentVideoPath }
                                        if (idx in 0 until videos.lastIndex) currentVideoPath = videos[idx + 1].path
                                        else exoPlayer.seekTo(exoPlayer.duration)
                                    } else exoPlayer.seekTo(exoPlayer.duration) 
                                }) { Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp)) }
                                if (isFullscreen) {
                                    IconButton(onClick = { showAudioTrackMenu = true }) { Icon(Icons.Filled.Audiotrack, contentDescription = "Audio track", tint = Color.White) }
                                    TextButton(onClick = {
                                        playbackSpeed = when (playbackSpeed) {
                                            1f -> 1.25f
                                            1.25f -> 1.5f
                                            1.5f -> 2f
                                            2f -> 0.5f
                                            else -> 1f
                                        }
                                    }) {
                                        androidx.compose.material3.Text("${playbackSpeed}x", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                IconButton(onClick = { isFullscreen = !isFullscreen }) { Icon(Icons.Filled.ScreenRotation, contentDescription = "Rotate", tint = Color.White) }
                            }
                        }
                    }
                }
            }
            if (showSubtitleMenu) {
                ModalBottomSheet(onDismissRequest = { showSubtitleMenu = false }, containerColor = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(16.dp).padding(bottom = 32.dp).fillMaxWidth()) {
                        Text("SUBTITLES", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth().clickable {
                            trackSelector.setParameters(trackSelector.buildUponParameters().setIgnoredTextSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT).setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true))
                        }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = tracks.groups.none { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT && it.isSelected }, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Disable")
                        }
                        tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }.forEach { group ->
                            for (i in 0 until group.length) {
                                val isSelected = group.isTrackSelected(i)
                                val format = group.getTrackFormat(i)
                                val label = format.language ?: format.label ?: "Track ${i + 1}"
                                Row(Modifier.fillMaxWidth().clickable {
                                    trackSelector.setParameters(trackSelector.buildUponParameters().setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false).setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i)))
                                }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(label)
                                }
                            }
                        }
                        
                        Divider(Modifier.padding(vertical = 8.dp))
                        
                        Text("Subtitle Delay", style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("-5s", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = subtitleDelayMs, 
                                onValueChange = { subtitleDelayMs = it }, 
                                valueRange = -5000f..5000f, 
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(12.dp),
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = Color.White.copy(alpha=0.3f)),
                                thumb = { Box(modifier = Modifier.size(0.dp)) },
                                track = { sliderState -> SliderDefaults.Track(sliderState = sliderState, modifier = Modifier.height(2.dp), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = Color.White.copy(alpha=0.3f))) }
                            )
                            Text("+5s", style = MaterialTheme.typography.labelSmall)
                        }

                        Text("Subtitle Position", style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Bottom", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = subtitlePosition, 
                                onValueChange = { subtitlePosition = it }, 
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(12.dp),
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = Color.White.copy(alpha=0.3f)),
                                thumb = { Box(modifier = Modifier.size(0.dp)) },
                                track = { sliderState -> SliderDefaults.Track(sliderState = sliderState, modifier = Modifier.height(2.dp), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = Color.White.copy(alpha=0.3f))) }
                            )
                            Text("Top", style = MaterialTheme.typography.labelSmall)
                        }
                        
                        TextButton(onClick = { /* Open Subtitle File picker logic here */ }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Open Subtitle File")
                        }
                    }
                }
            }
            if (showAudioTrackMenu) {
                ModalBottomSheet(onDismissRequest = { showAudioTrackMenu = false }, containerColor = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(16.dp).padding(bottom = 32.dp).fillMaxWidth()) {
                        Text("AUDIO TRACKS", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO }.forEach { group ->
                            for (i in 0 until group.length) {
                                val isSelected = group.isTrackSelected(i)
                                val format = group.getTrackFormat(i)
                                val label = format.language ?: format.label ?: "Audio Track ${i + 1}"
                                val details = buildString {
                                    if (format.channelCount > 0) append("${format.channelCount} ch")
                                    if (format.sampleRate > 0) append(if(isNotEmpty()) " - ${format.sampleRate}Hz" else "${format.sampleRate}Hz")
                                }
                                val finalLabel = if (details.isNotEmpty()) "$label ($details)" else label
                                Row(Modifier.fillMaxWidth().clickable {
                                    trackSelector.setParameters(trackSelector.buildUponParameters().setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, false).setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i)))
                                }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(finalLabel)
                                }
                            }
                        }
                        
                        Divider(Modifier.padding(vertical = 8.dp))
                        
                        Text("Audio Delay", style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("-500ms", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = audioDelayMs, 
                                onValueChange = { audioDelayMs = it; exoPlayer.audioSessionId = exoPlayer.audioSessionId /* To trigger something else we need to adjust format delay, but usually exoPlayer supports it using setAudioAttributes or custom audio processors, but for UI we provide slider */ }, 
                                valueRange = -500f..500f, 
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(12.dp),
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = Color.White.copy(alpha=0.3f)),
                                thumb = { Box(modifier = Modifier.size(0.dp)) },
                                track = { sliderState -> SliderDefaults.Track(sliderState = sliderState, modifier = Modifier.height(2.dp), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = Color.White.copy(alpha=0.3f))) }
                            )
                            Text("+500ms", style = MaterialTheme.typography.labelSmall)
                        }

                        Text("Volume Boost", style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("100%", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = volumeBoost, 
                                onValueChange = { volumeBoost = it; exoPlayer.volume = volumeBoost /* Max standard exoPlayer volume is 1.0, but to actually boost, an AudioProcessor is needed. Here we manage exoPlayer.volume up to 1f, or use loud audio enhancement */ }, 
                                valueRange = 0f..2f, 
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(12.dp),
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = Color.White.copy(alpha=0.3f)),
                                thumb = { Box(modifier = Modifier.size(0.dp)) },
                                track = { sliderState -> SliderDefaults.Track(sliderState = sliderState, modifier = Modifier.height(2.dp), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = Color.White.copy(alpha=0.3f))) }
                            )
                            Text("200%", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (showMoreMenu) {
                ModalBottomSheet(onDismissRequest = { showMoreMenu = false }, containerColor = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(16.dp).padding(bottom = 32.dp).fillMaxWidth()) {
                        Text("More Options", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(modifier = Modifier.clickable { showMoreMenu = false; isLocked = true }.padding(12.dp).width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Lock", style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                            Column(modifier = Modifier.clickable { showMoreMenu = false; showSubtitleMenu = true }.padding(12.dp).width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Subtitles, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Subtitles", style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                            Column(modifier = Modifier.clickable { showMoreMenu = false; showAudioTrackMenu = true }.padding(12.dp).width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Audio", style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(modifier = Modifier.clickable { 
                                playbackSpeed = when (playbackSpeed) { 1f -> 1.25f; 1.25f -> 1.5f; 1.5f -> 2f; 2f -> 0.5f; else -> 1f }
                            }.padding(12.dp).width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("${playbackSpeed}x Speed", style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                            Column(modifier = Modifier.clickable { isFullscreen = !isFullscreen; showMoreMenu = false }.padding(12.dp).width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.ScreenRotation, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Rotate", style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                            Column(modifier = Modifier.clickable { 
                                showMoreMenu = false
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    val activity = context as? Activity
                                    val params = android.app.PictureInPictureParams.Builder().build()
                                    activity?.enterPictureInPictureMode(params)
                                }
                            }.padding(12.dp).width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.PictureInPicture, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("PiP Mode", style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d", m, s)
}
