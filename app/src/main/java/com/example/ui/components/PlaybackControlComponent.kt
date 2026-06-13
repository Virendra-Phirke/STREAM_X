package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlaybackControlComponent(
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    playbackSpeed: Float = 1.0f,
    onSpeedChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var dragPosition by remember { mutableStateOf<Long?>(null) }
    val ecoModeEnabled by com.example.ui.viewmodels.ThemeManager.ecoModeEnabled.collectAsStateWithLifecycle()
    val enableCrossfade by com.example.ui.viewmodels.ThemeManager.enableCrossfade.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayPosition = dragPosition ?: currentPosition
            Text(formatTime(displayPosition), color = Color.White, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(8.dp))
            Slider(
                value = if (duration > 0) (displayPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                onValueChange = { 
                    dragPosition = (it * duration).toLong() 
                    onSeek(dragPosition!!)
                },
                onValueChangeFinished = { 
                    dragPosition?.let { 
                        dragPosition = null 
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
            Text(formatTime(duration.coerceAtLeast(0L)), color = Color.White, style = MaterialTheme.typography.labelMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onShuffleToggle) {
                Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f))
            }
            IconButton(onClick = onSkipPrevious) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = onPlayPause) {
                if (!ecoModeEnabled && enableCrossfade) {
                    androidx.compose.animation.Crossfade(targetState = isPlaying, label = "PlayPauseAnimation") { playing ->
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                } else {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            IconButton(onClick = onSkipNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = onRepeatToggle) {
                val icon = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat
                val tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                Icon(icon, contentDescription = "Repeat", tint = tint)
            }
            TextButton(onClick = {
                val nextSpeed = when {
                    playbackSpeed < 1.0f -> 1.0f
                    playbackSpeed < 1.5f -> 1.5f
                    playbackSpeed < 2.0f -> 2.0f
                    else -> 0.5f
                }
                onSpeedChange(nextSpeed)
            }) {
                Text("${playbackSpeed}x", color = Color.White, style = MaterialTheme.typography.labelMedium)
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
