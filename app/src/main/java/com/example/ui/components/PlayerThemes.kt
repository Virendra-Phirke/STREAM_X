package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.data.model.AudioEntity
import com.example.ui.viewmodel.AudioPlayerViewModel
import com.example.ui.viewmodels.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    var swipeOffsetX by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        swipeOffsetX += delta
    }
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161819)),
        border = BorderStroke(1.dp, Color(0xFF25292A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Artwork Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF182232)),
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
                            AnimatedPlaceholderAlbumArt(modifier = Modifier.fillMaxSize())
                        } else {
                            SubcomposeAsyncImageContent()
                        }
                    }
                )

                // Neon Gradient Border Outline
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.dp,
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6), Color(0xFFEC4899))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title, Subtitle, and Heart Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentAudio?.title ?: Uri.parse(decodedPath).lastPathSegment ?: "Unknown Track",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFE8E8E6),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentAudio?.artist?.takeIf { it != "<unknown>" } ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color(0xFF92999D),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        currentAudio?.let {
                            mediaViewModel.toggleAudioLike(it.path, !it.isLiked)
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (currentAudio?.isLiked == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (currentAudio?.isLiked == true) Color(0xFFFF653F) else Color(0xFF92999D),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seekbar and Controls
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
                }
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
    DefaultPlayerTheme(
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
        volume = volume
    )
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
    DefaultPlayerTheme(
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
        volume = volume
    )
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
    DefaultPlayerTheme(
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
        volume = volume
    )
}
