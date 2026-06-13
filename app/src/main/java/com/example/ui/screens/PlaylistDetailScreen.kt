package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodels.MediaViewModel
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Int,
    viewModel: MediaViewModel,
    onBack: () -> Unit,
    onAudioClick: (String) -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val playlistName = if (playlistId == -1) "Liked Music" else playlists.find { it.id == playlistId }?.name ?: "Playlist"
    val audiosFlow = remember(playlistId) { 
        if (playlistId == -1) viewModel.likedAudio else viewModel.getAudiosForPlaylist(playlistId) 
    }
    val audios by audiosFlow.collectAsState(initial = emptyList())
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedAudios = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { 
                            viewModel.removeAudiosFromPlaylist(playlistId, selectedAudios.toList())
                            selectedAudios.clear()
                            isSelectionMode = false 
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove Selected")
                        }
                        IconButton(onClick = { 
                            selectedAudios.clear()
                            isSelectionMode = false 
                        }) {
                            Text("Cancel", modifier = Modifier.padding(end = 8.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (audios.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Playlist is empty", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(audios, key = { it.path }) { audio ->
                    val isSelected = selectedAudios.contains(audio.path)
                    AudioItemWithSelection(
                        audio = audio,
                        isSelected = isSelected,
                        selectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                if (isSelected) selectedAudios.remove(audio.path) else selectedAudios.add(audio.path)
                            } else {
                                onAudioClick(audio.path)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedAudios.add(audio.path)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioItemWithSelection(
    audio: com.example.data.model.AudioEntity,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(backgroundColor),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    coil.compose.AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(audio.path)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Icon(Icons.Filled.Audiotrack, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(audio.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(audio.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}
