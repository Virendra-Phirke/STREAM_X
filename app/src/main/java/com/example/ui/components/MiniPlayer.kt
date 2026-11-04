package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AudioPlayerViewModel
import com.example.ui.viewmodels.MediaViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MiniPlayer(
    playerViewModel: AudioPlayerViewModel,
    mediaViewModel: MediaViewModel,
    onNavigateToPlayer: (String) -> Unit
) {
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val mediaIndex by playerViewModel.currentMediaItemIndex.collectAsStateWithLifecycle()
    val audios by mediaViewModel.audios.collectAsStateWithLifecycle()
    val currentPosition by playerViewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by playerViewModel.duration.collectAsStateWithLifecycle()

    val currentAudio = if (audios.isNotEmpty() && mediaIndex in audios.indices) audios[mediaIndex] else null
    val mediaController = playerViewModel.mediaController

    // Only display MiniPlayer if media is actively playing or loaded in controller
    val hasActiveMedia = mediaController != null && 
            (isPlaying || (currentAudio != null && mediaController.mediaItemCount > 0))

    if (!hasActiveMedia || currentAudio == null) {
        return
    }

    val title = cleanAudioTitle(currentAudio.title, currentAudio.path)
    val artist = currentAudio.artist.takeIf { it != "<unknown>" } ?: "Unknown Artist"
    val currentPath = currentAudio.path

    val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    // Swipe to Dismiss Gesture State
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = 150),
        label = "miniplayer_swipe_offset"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(offsetX) > 220f) {
                            // Swiped far enough - stop playback to dismiss mini player
                            playerViewModel.stopPlayback()
                            offsetX = 0f
                        } else {
                            // Snap back to center
                            offsetX = 0f
                        }
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    }
                )
            }
            .clickable {
                if (abs(offsetX) < 20f && currentPath.isNotEmpty()) {
                    onNavigateToPlayer(currentPath)
                }
            },
        color = Color(0xFF161819)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thin orange playback progress indicator on top
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Color(0xFFFF653F),
                trackColor = Color.Transparent
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vinyl / Waveform album art thumbnail
                MiniPlayerAlbumArt()

                Spacer(modifier = Modifier.width(14.dp))

                // Title and Artist Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFE8E8E6),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = Color(0xFF92999D),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Pause / Play button
                IconButton(
                    onClick = { playerViewModel.playPause() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFFE8E8E6),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Next track button
                IconButton(
                    onClick = { playerViewModel.skipToNext() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color(0xFFE8E8E6),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniPlayerAlbumArt(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF242829)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.85f)) {
            val c = center
            val r = size.minDimension / 2

            // Outer dark ring
            drawCircle(
                color = Color(0xFF33383A),
                radius = r,
                center = c
            )
            // Middle dark surface ring
            drawCircle(
                color = Color(0xFF161819),
                radius = r * 0.72f,
                center = c
            )
            // Accent orange ring
            drawCircle(
                color = Color(0xFFFF653F).copy(alpha = 0.65f),
                radius = r * 0.45f,
                center = c
            )
            // Center bright orange dot
            drawCircle(
                color = Color(0xFFFF653F),
                radius = r * 0.25f,
                center = c
            )
        }
    }
}
