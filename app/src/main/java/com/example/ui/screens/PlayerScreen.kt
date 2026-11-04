package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.RepeatOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoPath: String,
    mediaViewModel: MediaViewModel? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentVideoPath by remember { mutableStateOf(Uri.decode(videoPath)) }
    val videos by mediaViewModel?.videos?.collectAsStateWithLifecycle(initialValue = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // UNIQUE FEATURE 1: Smart A-B Seamless Loop
    var loopPointA by remember { mutableStateOf<Long?>(null) }
    var loopPointB by remember { mutableStateOf<Long?>(null) }
    var isABLoopActive by remember { mutableStateOf(false) }

    // UNIQUE FEATURE 2: Night Dialogue & Vocal Clarity Boost
    var isNightDialogueBoost by remember { mutableStateOf(false) }
    
    var showSubtitleMenu by remember { mutableStateOf(false) }
    var showAudioTrackMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    
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
            .setBufferDurationsMs(10000, 20000, 1000, 1000)
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
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    error.printStackTrace()
                }
            })
        }
    }

    // A-B Loop Enforcement Logic
    LaunchedEffect(currentPosition, isABLoopActive, loopPointA, loopPointB) {
        if (isABLoopActive && loopPointA != null && loopPointB != null) {
            val start = loopPointA!!
            val end = loopPointB!!
            if (currentPosition >= end || currentPosition < start) {
                exoPlayer.seekTo(start)
            }
        }
    }
    
    LaunchedEffect(currentVideoPath) {
        val videoUri = try {
            when {
                currentVideoPath.startsWith("content://") ||
                currentVideoPath.startsWith("file://") ||
                currentVideoPath.startsWith("http://") ||
                currentVideoPath.startsWith("https://") -> Uri.parse(currentVideoPath)
                else -> Uri.fromFile(java.io.File(currentVideoPath))
            }
        } catch (e: Exception) {
            Uri.parse(currentVideoPath)
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri))
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
            if (abs(attributes.screenBrightness - brightness) > 0.01f) {
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

    val ecoModeEnabled by com.example.ui.viewmodel.ThemeManager.ecoModeEnabled.collectAsStateWithLifecycle()
    
    LaunchedEffect(isPlaying, ecoModeEnabled) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            delay(if (ecoModeEnabled) 2000L else 500L)
        }
    }

    LaunchedEffect(isRepeat) {
        exoPlayer.repeatMode = if (isRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
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
                } else false
            },
        color = Color(0xFF0D0F10)
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
                                    dragSeekPosition?.let { exoPlayer.seekTo(it) }
                                    dragSeekPosition = null
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                val yDelta = dragAmount.y
                                val xDelta = dragAmount.x
                                
                                if (abs(xDelta) > abs(yDelta) && !showBrightnessIndicator && !showVolumeIndicator) {
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

            // Gesture Overlay Indicators
            if (showBrightnessIndicator) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterStart).padding(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF161819).copy(alpha = 0.88f)
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Brightness6, contentDescription = null, tint = Color(0xFFFF653F), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("${(brightness * 100).toInt()}%", color = Color(0xFFE8E8E6), fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (showVolumeIndicator) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF161819).copy(alpha = 0.88f)
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = Color(0xFFFF653F), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("${(volume * 100).toInt()}%", color = Color(0xFFE8E8E6), fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (dragSeekPosition != null) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF161819).copy(alpha = 0.9f)
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatTime(dragSeekPosition ?: 0L), color = Color(0xFFE8E8E6), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("[${if ((dragSeekPosition ?: 0L) > exoPlayer.currentPosition) "+" else "-"}${formatTime(abs((dragSeekPosition ?: 0L) - exoPlayer.currentPosition))}]", color = Color(0xFFFF653F), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            AnimatedVisibility(
                visible = showControls,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))) {
                    
                    if (isLocked) {
                        IconButton(onClick = { isLocked = false }, modifier = Modifier.align(Alignment.TopStart).padding(32.dp)) {
                            Icon(Icons.Filled.Lock, contentDescription = "Unlock", tint = Color(0xFFFF653F), modifier = Modifier.size(32.dp))
                        }
                    } else {
                        // Top Controls Header
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFE8E8E6))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                Uri.parse(currentVideoPath).lastPathSegment ?: "Video",
                                color = Color(0xFFE8E8E6),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            // Night Dialogue Boost Feature Badge
                            if (isNightDialogueBoost) {
                                Surface(
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6)),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Vocal Boost", color = Color(0xFF8B5CF6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                            Text(sdf.format(Date()), color = Color(0xFF92999D), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(12.dp))
                            
                            val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
                            val batLevel = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                            Text("🔋$batLevel%", color = Color(0xFF92999D), style = MaterialTheme.typography.bodyMedium)
                            
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color(0xFFE8E8E6))
                            }
                        }

                        // Center Playback Controls
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { 
                                if (videos.isNotEmpty()) {
                                    val idx = videos.indexOfFirst { Uri.decode(it.path) == currentVideoPath || it.path == currentVideoPath }
                                    if (idx > 0) currentVideoPath = videos[idx - 1].path
                                    else exoPlayer.seekTo(0L)
                                } else exoPlayer.seekTo(0L) 
                            }) {
                                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color(0xFFE8E8E6), modifier = Modifier.size(40.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF653F))
                                    .clickable { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }

                            IconButton(onClick = { 
                                if (videos.isNotEmpty()) {
                                    val idx = videos.indexOfFirst { Uri.decode(it.path) == currentVideoPath || it.path == currentVideoPath }
                                    if (idx in 0 until videos.lastIndex) currentVideoPath = videos[idx + 1].path
                                    else exoPlayer.seekTo(exoPlayer.duration)
                                } else exoPlayer.seekTo(exoPlayer.duration) 
                            }) {
                                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color(0xFFE8E8E6), modifier = Modifier.size(40.dp))
                            }
                        }

                        // Bottom Controls Row & Seekbar
                        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                            // A-B Seamless Loop Badge Indicator (if set)
                            if (loopPointA != null || isABLoopActive) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0xFFFF653F).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF653F))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.RepeatOn, contentDescription = null, tint = Color(0xFFFF653F), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = if (isABLoopActive && loopPointA != null && loopPointB != null)
                                                    "A-B Loop: ${formatTime(loopPointA!!)} → ${formatTime(loopPointB!!)}"
                                                else
                                                    "Point A Set: ${formatTime(loopPointA!!)} (Tap B to loop)",
                                                color = Color(0xFFFF653F),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "Clear A-B Loop",
                                                tint = Color(0xFFFF653F),
                                                modifier = Modifier.size(16.dp).clickable {
                                                    loopPointA = null
                                                    loopPointB = null
                                                    isABLoopActive = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Seekbar
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(formatTime(dragSeekPosition ?: currentPosition), color = Color(0xFFE8E8E6), style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.width(8.dp))
                                Slider(
                                    value = if (exoPlayer.duration > 0) ((dragSeekPosition ?: currentPosition).toFloat() / exoPlayer.duration).coerceIn(0f, 1f) else 0f,
                                    onValueChange = { 
                                        dragSeekPosition = (it * exoPlayer.duration).toLong() 
                                        exoPlayer.seekTo(dragSeekPosition!!)
                                    },
                                    onValueChangeFinished = { dragSeekPosition = null },
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFF653F),
                                        activeTrackColor = Color(0xFFFF653F),
                                        inactiveTrackColor = Color(0xFF25292A)
                                    ),
                                    modifier = Modifier.weight(1f).height(12.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(formatTime(exoPlayer.duration.coerceAtLeast(0L)), color = Color(0xFF92999D), style = MaterialTheme.typography.labelMedium)
                            }

                            // Bottom Actions Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { isLocked = true }) {
                                    Icon(Icons.Filled.Lock, contentDescription = "Lock", tint = Color(0xFFE8E8E6))
                                }

                                // UNIQUE FEATURE: A-B Loop Trigger Button
                                OutlinedButton(
                                    onClick = {
                                        if (loopPointA == null) {
                                            loopPointA = currentPosition
                                            Toast.makeText(context, "Loop Point A set at ${formatTime(currentPosition)}", Toast.LENGTH_SHORT).show()
                                        } else if (loopPointB == null) {
                                            if (currentPosition > loopPointA!!) {
                                                loopPointB = currentPosition
                                                isABLoopActive = true
                                                Toast.makeText(context, "A-B Loop Active!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Point B must be after Point A", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            loopPointA = null
                                            loopPointB = null
                                            isABLoopActive = false
                                            Toast.makeText(context, "A-B Loop Cleared", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isABLoopActive) Color(0xFFFF653F) else Color(0xFF242829),
                                        contentColor = if (isABLoopActive) Color.White else Color(0xFFE8E8E6)
                                    )
                                ) {
                                    Text(
                                        text = when {
                                            isABLoopActive -> "A-B On"
                                            loopPointA != null -> "Set B"
                                            else -> "A-B Loop"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(onClick = { showSubtitleMenu = true }) {
                                    Icon(Icons.Filled.Subtitles, contentDescription = "Subtitle", tint = Color(0xFFE8E8E6))
                                }

                                IconButton(onClick = { showAudioTrackMenu = true }) {
                                    Icon(Icons.Filled.Audiotrack, contentDescription = "Audio track", tint = Color(0xFFE8E8E6))
                                }

                                TextButton(onClick = {
                                    playbackSpeed = when (playbackSpeed) {
                                        1f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2f
                                        2f -> 0.5f
                                        else -> 1f
                                    }
                                }) {
                                    Text("${playbackSpeed}x", color = Color(0xFFFF653F), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }

                                IconButton(onClick = { isFullscreen = !isFullscreen }) {
                                    Icon(Icons.Filled.ScreenRotation, contentDescription = "Rotate", tint = Color(0xFFE8E8E6))
                                }
                            }
                        }
                    }
                }
            }

            // Subtitle Selection Sheet
            if (showSubtitleMenu) {
                ModalBottomSheet(
                    onDismissRequest = { showSubtitleMenu = false },
                    containerColor = Color(0xFF161819)
                ) {
                    Column(Modifier.padding(20.dp).fillMaxWidth()) {
                        Text("SUBTITLES", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF653F), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                trackSelector.setParameters(
                                    trackSelector.buildUponParameters()
                                        .setIgnoredTextSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                                        .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                                )
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = tracks.groups.none { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT && it.isSelected }, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Disable", color = Color(0xFFE8E8E6))
                        }
                        tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }.forEach { group ->
                            for (i in 0 until group.length) {
                                val isSelected = group.isTrackSelected(i)
                                val format = group.getTrackFormat(i)
                                val label = format.language ?: format.label ?: "Track ${i + 1}"
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        trackSelector.setParameters(
                                            trackSelector.buildUponParameters()
                                                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                                                .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i))
                                        )
                                    }.padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(label, color = Color(0xFFE8E8E6))
                                }
                            }
                        }
                    }
                }
            }

            // Audio Track & Vocal Boost Sheet
            if (showAudioTrackMenu) {
                ModalBottomSheet(
                    onDismissRequest = { showAudioTrackMenu = false },
                    containerColor = Color(0xFF161819)
                ) {
                    Column(Modifier.padding(20.dp).fillMaxWidth()) {
                        Text("AUDIO TRACKS & VOCAL BOOST", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF653F), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))

                        // Night Dialogue Boost Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF242829))
                                .clickable { isNightDialogueBoost = !isNightDialogueBoost }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Night Dialogue Clarity Boost", color = Color(0xFFE8E8E6), fontWeight = FontWeight.Bold)
                                    Text("Intelligently boost dialogue frequencies", color = Color(0xFF92999D), fontSize = 12.sp)
                                }
                            }
                            Switch(
                                checked = isNightDialogueBoost,
                                onCheckedChange = { isNightDialogueBoost = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF8B5CF6)
                                )
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO }.forEach { group ->
                            for (i in 0 until group.length) {
                                val isSelected = group.isTrackSelected(i)
                                val format = group.getTrackFormat(i)
                                val label = format.language ?: format.label ?: "Audio Track ${i + 1}"
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        trackSelector.setParameters(
                                            trackSelector.buildUponParameters()
                                                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, false)
                                                .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i))
                                        )
                                    }.padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = isSelected, onClick = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(label, color = Color(0xFFE8E8E6))
                                }
                            }
                        }
                    }
                }
            }

            // More Options Sheet
            if (showMoreMenu) {
                ModalBottomSheet(
                    onDismissRequest = { showMoreMenu = false },
                    containerColor = Color(0xFF161819)
                ) {
                    Column(Modifier.padding(20.dp).fillMaxWidth()) {
                        Text("Player Features", style = MaterialTheme.typography.titleLarge, color = Color(0xFFE8E8E6), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(
                                modifier = Modifier
                                    .clickable { showMoreMenu = false; isLocked = true }
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFFF653F), modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(6.dp))
                                Text("Lock Screen", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE8E8E6))
                            }

                            Column(
                                modifier = Modifier
                                    .clickable { showMoreMenu = false; isNightDialogueBoost = !isNightDialogueBoost }
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(6.dp))
                                Text("Vocal Boost", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE8E8E6))
                            }

                            Column(
                                modifier = Modifier
                                    .clickable { showMoreMenu = false; showSubtitleMenu = true }
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.Subtitles, contentDescription = null, tint = Color(0xFFE8E8E6), modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(6.dp))
                                Text("Subtitles", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE8E8E6))
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
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}
