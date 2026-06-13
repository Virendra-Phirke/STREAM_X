package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.data.model.AudioEntity
import com.example.ui.viewmodel.AudioPlayerViewModel
import com.example.ui.viewmodels.MediaViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

@Composable
fun DefaultPlayerTheme(
    currentAudio: AudioEntity?,
    decodedPath: String,
    imageRequest: ImageRequest,
    playerViewModel: AudioPlayerViewModel,
    mediaViewModel: MediaViewModel,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    currentPosition: Long,
    duration: Long,
    volume: Float
) {
    var swipeOffsetX by remember { mutableStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        swipeOffsetX += delta
    }
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsStateWithLifecycle()
    
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (swipeOffsetX > 100f) {
                        playerViewModel.skipToPrevious()
                    } else if (swipeOffsetX < -100f) {
                        playerViewModel.skipToNext()
                    }
                    swipeOffsetX = 0f
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            content = {
                val state = painter.state
                if (state is AsyncImagePainter.State.Error || state is AsyncImagePainter.State.Empty) {
                    com.example.ui.components.AnimatedPlaceholderAlbumArt(modifier = Modifier.fillMaxSize())
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
        )
        Box(modifier = Modifier.fillMaxSize()
            .background(Color.Transparent)
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp)))
    }
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentAudio?.title ?: Uri.parse(decodedPath).lastPathSegment ?: "Unknown Track",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                blurRadius = 10f
                            )
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentAudio?.artist?.takeIf { it != "<unknown>" } ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = { 
                    currentAudio?.let {
                        mediaViewModel.toggleAudioLike(it.path, !it.isLiked)
                    }
                }) {
                    Icon(
                        if (currentAudio?.isLiked == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, 
                        contentDescription = "Favorite", 
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val sessionId by com.example.service.PlaybackService.audioSessionId.collectAsStateWithLifecycle(initialValue = 0)
            Spacer(modifier = Modifier.height(30.dp))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            var isShuffleState by remember { mutableStateOf(isShuffle) }
            PlaybackControlComponent(
                isPlaying = isPlaying,
                isShuffle = isShuffleState,
                repeatMode = repeatMode,
                currentPosition = currentPosition,
                duration = duration,
                volume = volume,
                playbackSpeed = playbackSpeed,
                onPlayPause = { playerViewModel.playPause() },
                onSeek = { playerViewModel.seekTo(it) },
                onVolumeChange = { playerViewModel.setVolume(it) },
                onSpeedChange = { playerViewModel.setPlaybackSpeed(it) },
                onSkipNext = { playerViewModel.skipToNext() },
                onSkipPrevious = { playerViewModel.skipToPrevious() },
                onShuffleToggle = { 
                    isShuffleState = !isShuffleState
                    playerViewModel.setShuffleModeEnabled(isShuffleState)
                },
                onRepeatToggle = {
                    val nextMode = when (repeatMode) {
                        androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                        androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                        else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                    }
                    playerViewModel.setRepeatMode(nextMode)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CircularPlayerTheme(
    currentAudio: AudioEntity?,
    decodedPath: String,
    imageRequest: ImageRequest,
    playerViewModel: AudioPlayerViewModel,
    mediaViewModel: MediaViewModel,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    currentPosition: Long,
    duration: Long,
    volume: Float
) {
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsStateWithLifecycle()
    var swipeOffsetX by remember { mutableStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        swipeOffsetX += delta
    }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentAudio?.title ?: Uri.parse(decodedPath).lastPathSegment ?: "Unknown Track",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light, color = Color.White),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentAudio?.artist?.takeIf { it != "<unknown>" } ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.7f))
                )
            }
            IconButton(onClick = { currentAudio?.let { mediaViewModel.toggleAudioLike(it.path, !it.isLiked) } }) {
                Icon(
                    if (currentAudio?.isLiked == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, 
                    contentDescription = "Favorite", 
                    tint = if(currentAudio?.isLiked == true) Color(0xFFFF6B6B) else Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Box(
            modifier = Modifier.size(300.dp)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (swipeOffsetX > 100f) {
                            playerViewModel.skipToPrevious()
                        } else if (swipeOffsetX < -100f) {
                            playerViewModel.skipToNext()
                        }
                        swipeOffsetX = 0f
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
            ) {
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    content = {
                        val state = painter.state
                        if (state is AsyncImagePainter.State.Error || state is AsyncImagePainter.State.Empty) {
                            com.example.ui.components.AnimatedPlaceholderAlbumArt(modifier = Modifier.fillMaxSize())
                        } else {
                            SubcomposeAsyncImageContent()
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        var isShuffleState by remember { mutableStateOf(isShuffle) }
        
        PlaybackControlComponent(
            isPlaying = isPlaying,
            isShuffle = isShuffleState,
            repeatMode = repeatMode,
            currentPosition = currentPosition,
            duration = duration,
            volume = volume,
            playbackSpeed = playbackSpeed,
            onPlayPause = { playerViewModel.playPause() },
            onSeek = { playerViewModel.seekTo(it) },
            onVolumeChange = { playerViewModel.setVolume(it) },
            onSpeedChange = { playerViewModel.setPlaybackSpeed(it) },
            onSkipNext = { playerViewModel.skipToNext() },
            onSkipPrevious = { playerViewModel.skipToPrevious() },
            onShuffleToggle = { 
                isShuffleState = !isShuffleState
                playerViewModel.setShuffleModeEnabled(isShuffleState)
            },
            onRepeatToggle = {
                val nextMode = when (repeatMode) {
                    androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                    androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                    else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                }
                playerViewModel.setRepeatMode(nextMode)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PurpleGlassTheme(
    currentAudio: AudioEntity?,
    decodedPath: String,
    imageRequest: ImageRequest,
    playerViewModel: AudioPlayerViewModel,
    mediaViewModel: MediaViewModel,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    currentPosition: Long,
    duration: Long,
    volume: Float
) {
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsStateWithLifecycle()
    var swipeOffsetX by remember { mutableStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        swipeOffsetX += delta
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(40.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.4f), Color(0xFF4C1D95).copy(alpha = 0.6f))))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(40.dp))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Horizontal,
                        onDragStopped = {
                            if (swipeOffsetX > 100f) {
                                playerViewModel.skipToPrevious()
                            } else if (swipeOffsetX < -100f) {
                                playerViewModel.skipToNext()
                            }
                            swipeOffsetX = 0f
                        }
                    )
            ) {
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    content = {
                        val state = painter.state
                        if (state is AsyncImagePainter.State.Error || state is AsyncImagePainter.State.Empty) {
                            com.example.ui.components.AnimatedPlaceholderAlbumArt(modifier = Modifier.fillMaxSize())
                        } else {
                            SubcomposeAsyncImageContent()
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentAudio?.title ?: Uri.parse(decodedPath).lastPathSegment ?: "Unknown Track",
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentAudio?.artist?.takeIf { it != "<unknown>" } ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f))
                    )
                }
                IconButton(onClick = { currentAudio?.let { mediaViewModel.toggleAudioLike(it.path, !it.isLiked) } }) {
                    Icon(
                        if (currentAudio?.isLiked == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, 
                        contentDescription = "Favorite", 
                        tint = if(currentAudio?.isLiked == true) Color(0xFFFF6B6B) else Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            var isShuffleState by remember { mutableStateOf(isShuffle) }
            PlaybackControlComponent(
                isPlaying = isPlaying, isShuffle = isShuffleState, repeatMode = repeatMode,
                currentPosition = currentPosition, duration = duration, volume = volume, playbackSpeed = playbackSpeed,
                onPlayPause = { playerViewModel.playPause() }, onSeek = { playerViewModel.seekTo(it) },
                onVolumeChange = { playerViewModel.setVolume(it) }, onSpeedChange = { playerViewModel.setPlaybackSpeed(it) },
                onSkipNext = { playerViewModel.skipToNext() }, onSkipPrevious = { playerViewModel.skipToPrevious() },
                onShuffleToggle = { isShuffleState = !isShuffleState; playerViewModel.setShuffleModeEnabled(isShuffleState) },
                onRepeatToggle = { 
                    val nextMode = when (repeatMode) {
                        androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                        androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                        else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                    }
                    playerViewModel.setRepeatMode(nextMode)
                }
            )
        }
    }
}

@Composable
fun TransparentGlassTheme(
    currentAudio: AudioEntity?,
    decodedPath: String,
    imageRequest: ImageRequest,
    playerViewModel: AudioPlayerViewModel,
    mediaViewModel: MediaViewModel,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    currentPosition: Long,
    duration: Long,
    volume: Float
) {
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsStateWithLifecycle()
    var swipeOffsetX by remember { mutableStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        swipeOffsetX += delta
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Horizontal,
                        onDragStopped = {
                            if (swipeOffsetX > 100f) {
                                playerViewModel.skipToPrevious()
                            } else if (swipeOffsetX < -100f) {
                                playerViewModel.skipToNext()
                            }
                            swipeOffsetX = 0f
                        }
                    )
            ) {
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    content = {
                        val state = painter.state
                        if (state is AsyncImagePainter.State.Error || state is AsyncImagePainter.State.Empty) {
                            com.example.ui.components.AnimatedPlaceholderAlbumArt(modifier = Modifier.fillMaxSize())
                        } else {
                            SubcomposeAsyncImageContent()
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentAudio?.title ?: Uri.parse(decodedPath).lastPathSegment ?: "Unknown Track",
                        style = MaterialTheme.typography.headlineMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentAudio?.artist?.takeIf { it != "<unknown>" } ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f))
                    )
                }
                IconButton(onClick = { currentAudio?.let { mediaViewModel.toggleAudioLike(it.path, !it.isLiked) } }) {
                    Icon(
                        if (currentAudio?.isLiked == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, 
                        contentDescription = "Favorite", 
                        tint = if(currentAudio?.isLiked == true) Color(0xFFFF6B6B) else Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            var isShuffleState by remember { mutableStateOf(isShuffle) }
            PlaybackControlComponent(
                isPlaying = isPlaying, isShuffle = isShuffleState, repeatMode = repeatMode,
                currentPosition = currentPosition, duration = duration, volume = volume, playbackSpeed = playbackSpeed,
                onPlayPause = { playerViewModel.playPause() }, onSeek = { playerViewModel.seekTo(it) },
                onVolumeChange = { playerViewModel.setVolume(it) }, onSpeedChange = { playerViewModel.setPlaybackSpeed(it) },
                onSkipNext = { playerViewModel.skipToNext() }, onSkipPrevious = { playerViewModel.skipToPrevious() },
                onShuffleToggle = { isShuffleState = !isShuffleState; playerViewModel.setShuffleModeEnabled(isShuffleState) },
                onRepeatToggle = { 
                    val nextMode = when (repeatMode) {
                        androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                        androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                        else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                    }
                    playerViewModel.setRepeatMode(nextMode)
                }
            )
        }
    }
}
