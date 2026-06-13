package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AudioPlayerViewModel
import com.example.ui.viewmodels.MediaViewModel
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(
    playerViewModel: AudioPlayerViewModel,
    mediaViewModel: MediaViewModel,
    onNavigateToPlayer: (String) -> Unit
) {
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val mediaIndex by playerViewModel.currentMediaItemIndex.collectAsStateWithLifecycle()
    val audios by mediaViewModel.audios.collectAsStateWithLifecycle()
    val currentQueue by playerViewModel.currentQueue.collectAsStateWithLifecycle()
    val currentPosition by playerViewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by playerViewModel.duration.collectAsStateWithLifecycle()
    
    // Only show if mediaController is loaded and has duration (is valid)
    if (playerViewModel.mediaController != null && audios.isNotEmpty() && mediaIndex in audios.indices && currentQueue.isNotEmpty()) {
        val currentAudio = audios[mediaIndex]
        val context = LocalContext.current
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xE61A1A1A))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    var actionTriggered = false
                    detectDragGestures(
                        onDragStart = { actionTriggered = false },
                        onDragEnd = { actionTriggered = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (actionTriggered) return@detectDragGestures
                            
                            val x = dragAmount.x
                            val y = dragAmount.y
                            if (kotlin.math.abs(x) > kotlin.math.abs(y)) {
                                if (x > 30) {
                                    playerViewModel.skipToPrevious()
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    actionTriggered = true
                                } else if (x < -30) {
                                    playerViewModel.skipToNext()
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    actionTriggered = true
                                }
                            } else {
                                if (y < -30) {
                                    onNavigateToPlayer(currentAudio.path)
                                    actionTriggered = true
                                }
                            }
                        }
                    )
                }
                .clickable { onNavigateToPlayer(currentAudio.path) }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A2A2A))
                    ) {
                        val albumArtUrl by playerViewModel.currentAlbumArtUrl.collectAsStateWithLifecycle()
                        val sArtworkUri = android.net.Uri.parse("content://media/external/audio/albumart")
                        val coverUri = currentAudio.albumId.let { android.content.ContentUris.withAppendedId(sArtworkUri, it) }
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(albumArtUrl ?: coverUri ?: currentAudio.path)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Mini Player Art",
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
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentAudio.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = currentAudio.artist.takeIf { it != "<unknown>" } ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                    
                    IconButton(onClick = { playerViewModel.skipToPrevious() }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { playerViewModel.playPause() }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { playerViewModel.skipToNext() }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { playerViewModel.stopPlayback() }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Non-interactive progress line
                val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
