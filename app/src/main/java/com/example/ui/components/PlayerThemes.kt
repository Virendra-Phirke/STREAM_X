package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.data.model.AudioEntity
import com.example.ui.viewmodel.AudioPlayerViewModel
import com.example.ui.viewmodels.MediaViewModel

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

    var dragPosition by remember { mutableStateOf<Long?>(null) }
    val displayPosition = dragPosition ?: currentPosition

    val trackTitle = currentAudio?.title ?: Uri.parse(decodedPath).lastPathSegment ?: "Unknown Track"
    val artistName = currentAudio?.artist?.takeIf { it != "<unknown>" } ?: "Unknown Artist"
    val initialLetter = trackTitle.firstOrNull()?.uppercase() ?: "E"

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Center Concentric Vinyl Disc Artwork Box
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(Color(0xFF2B2724)),
            contentAlignment = Alignment.Center
        ) {
            // Inner Concentric Vinyl Rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = center
                val r = size.minDimension / 2
                drawCircle(color = Color(0xFF1E1A17), radius = r * 0.78f, center = c)
                drawCircle(color = Color(0xFF161311), radius = r * 0.54f, center = c)
            }

            // Center Gold Disc with Track Initial or Cover
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0B849)),
                contentAlignment = Alignment.Center
            ) {
                var isImageError by remember(imageRequest) { mutableStateOf(false) }

                if (!isImageError) {
                    SubcomposeAsyncImage(
                        model = imageRequest,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = { isImageError = true }
                    )
                }

                if (isImageError) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initialLetter,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = Color(0xFF161311)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Track Title & Heart Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trackTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFE8E8E6),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$artistName · Sandhya",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
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
                    tint = if (currentAudio?.isLiked == true) Color(0xFFFF653F) else Color(0xFFE8E8E6),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Seekbar Row
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = if (duration > 0) (displayPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                onValueChange = {
                    dragPosition = (it * duration).toLong()
                    playerViewModel.seekTo(dragPosition!!)
                },
                onValueChangeFinished = { dragPosition = null },
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFF0B849),
                    activeTrackColor = Color(0xFFF0B849),
                    inactiveTrackColor = Color(0xFF33383A)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(displayPosition),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = Color(0xFF92999D)
                )

                Text(
                    text = formatTime(duration.coerceAtLeast(0L)),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = Color(0xFF92999D)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Playback Control Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { playerViewModel.setShuffleModeEnabled(!isShuffle) }) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) Color(0xFFF0B849) else Color(0xFF92999D)
                )
            }

            IconButton(onClick = { playerViewModel.skipToPrevious() }) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color(0xFFE8E8E6),
                    modifier = Modifier.size(32.dp)
                )
            }

            // Center Gold Play/Pause Button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0B849))
                    .clickable { playerViewModel.playPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = { playerViewModel.skipToNext() }) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color(0xFFE8E8E6),
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = {
                    val nextMode = when (repeatMode) {
                        androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                        androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                        else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                    }
                    playerViewModel.setRepeatMode(nextMode)
                }
            ) {
                val icon = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat
                val tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) Color(0xFFF0B849) else Color(0xFF92999D)
                Icon(icon, contentDescription = "Repeat", tint = tint)
            }
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

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d", m, s)
}
