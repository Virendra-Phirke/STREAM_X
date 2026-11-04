package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.AudioEntity
import com.example.ui.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    viewModel: MediaViewModel,
    onBack: () -> Unit,
    onAudioClick: (String) -> Unit
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlistName = if (playlistId == -1) "Liked Music" else playlists.find { it.id == playlistId }?.name ?: "Playlist"
    val audiosFlow = remember(playlistId) { 
        if (playlistId == -1) viewModel.likedAudio else viewModel.getAudiosForPlaylist(playlistId) 
    }
    val audios by audiosFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedAudios = remember { mutableStateListOf<String>() }
    var selectedAudioForInfo by remember { mutableStateOf<AudioEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F10))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFE8E8E6)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = playlistName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFE8E8E6),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isSelectionMode) {
                    IconButton(onClick = {
                        viewModel.removeAudiosFromPlaylist(playlistId, selectedAudios.toList())
                        selectedAudios.clear()
                        isSelectionMode = false
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Remove Selected",
                            tint = Color(0xFFFF653F)
                        )
                    }
                    TextButton(onClick = {
                        selectedAudios.clear()
                        isSelectionMode = false
                    }) {
                        Text("Cancel", color = Color(0xFF92999D))
                    }
                }
            }

            // Playlist Controls Row (Play All / Shuffle)
            if (audios.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${audios.size} tracks",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color(0xFF92999D)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onAudioClick(audios.first().path) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF653F)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play All",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play All", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val randomTrack = audios.shuffled().firstOrNull()
                                randomTrack?.let { onAudioClick(it.path) }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle",
                                tint = Color(0xFFE8E8E6),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Shuffle", fontSize = 13.sp, color = Color(0xFFE8E8E6))
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF25292A), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }

            // Song List
            if (audios.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161819)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25292A)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Playlist is Empty",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFE8E8E6)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add songs to this playlist from the Audio screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92999D)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(audios, key = { it.path }) { audio ->
                        val isSelected = selectedAudios.contains(audio.path)
                        PlaylistTrackRow(
                            audio = audio,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    if (isSelected) selectedAudios.remove(audio.path) else selectedAudios.add(audio.path)
                                    if (selectedAudios.isEmpty()) isSelectionMode = false
                                } else {
                                    onAudioClick(audio.path)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedAudios.add(audio.path)
                                }
                            },
                            onLikeToggle = {
                                viewModel.toggleAudioLike(audio.path, !audio.isLiked)
                            },
                            onInfoClick = {
                                selectedAudioForInfo = audio
                            }
                        )
                    }
                }
            }
        }

        // Info Dialog
        selectedAudioForInfo?.let { audio ->
            AlertDialog(
                onDismissRequest = { selectedAudioForInfo = null },
                containerColor = Color(0xFF161819),
                titleContentColor = Color(0xFFE8E8E6),
                textContentColor = Color(0xFF92999D),
                title = { Text("Audio Details", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Title: ${audio.title}")
                        Text("Artist: ${audio.artist}")
                        Text("Album: ${audio.album}")
                        Text("Path: ${audio.path}")
                        Text("Duration: ${formatDuration(audio.duration)}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedAudioForInfo = null }) {
                        Text("Close", color = Color(0xFFFF653F))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistTrackRow(
    audio: AudioEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLikeToggle: () -> Unit,
    onInfoClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(if (isSelected) Color(0xFF242829) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFFFF653F),
                    uncheckedColor = Color(0xFF92999D)
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        // Album Art Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF242829)),
            contentAlignment = Alignment.Center
        ) {
            val sArtworkUri = android.net.Uri.parse("content://media/external/audio/albumart")
            val albumArtUri = android.content.ContentUris.withAppendedId(sArtworkUri, audio.albumId)

            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .crossfade(true)
                    .build(),
                contentDescription = audio.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = Color(0xFF2C4A52), size = size)
                        drawLine(
                            color = Color(0xFF3B626C),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 6f
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Artist Column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audio.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFFE8E8E6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = audio.artist.takeIf { it != "<unknown>" } ?: "Unknown Artist",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Color(0xFF92999D),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Duration Text
        Text(
            text = formatDuration(audio.duration),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = Color(0xFF92999D)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Heart Like Toggle
        IconButton(
            onClick = onLikeToggle,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (audio.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Like",
                tint = if (audio.isLiked) Color(0xFFFF653F) else Color(0xFF92999D),
                modifier = Modifier.size(20.dp)
            )
        }

        // Circular Info Icon
        IconButton(
            onClick = onInfoClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Details",
                tint = Color(0xFF92999D),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
