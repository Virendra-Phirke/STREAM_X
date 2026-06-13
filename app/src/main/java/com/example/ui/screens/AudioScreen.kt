package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AudioEntity
import com.example.ui.viewmodels.MediaViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioScreen(
    viewModel: MediaViewModel,
    onAudioClick: (String) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            onAudioClick(it.toString())
        }
    }

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val permissionState = rememberMultiplePermissionsState(permissionsToRequest)

    val audios by viewModel.audios.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    
    var sortOrder by remember { mutableStateOf("Name") }
    var searchQuery by remember { mutableStateOf("") }
    
    var selectedAudioForInfo by remember { mutableStateOf<AudioEntity?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedAudios = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    
    var isSearchExpanded by remember { mutableStateOf(false) }
    
    val sortedAudios by remember {
        androidx.compose.runtime.derivedStateOf {
            val filtered = if (searchQuery.isBlank()) audios else audios.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
            when (sortOrder) {
                "Duration" -> filtered.sortedByDescending { it.duration }
                "Artist" -> filtered.sortedBy { it.artist.lowercase() }
                "Album" -> filtered.sortedBy { it.album.lowercase() }
                else -> filtered.sortedBy { it.title.lowercase() }
            }
        }
    }

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            viewModel.scanMedia()
        } else {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (!permissionState.allPermissionsGranted) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                    Text("Grant Permissions")
                }
            }
        } else {
            if (audios.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("No music found")
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    if (isSelectionMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${selectedAudios.size} selected")
                            Row {
                                IconButton(onClick = { showPlaylistDialog = true }) {
                                    Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add to Playlist", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = { 
                                    isSelectionMode = false
                                    selectedAudios.clear()
                                }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isSearchExpanded) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search title, artist, album") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    leadingIcon = { 
                                        IconButton(onClick = { 
                                            isSearchExpanded = false
                                            searchQuery = ""
                                        }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Close search")
                                        }
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Filled.Clear, contentDescription = "Clear text")
                                            }
                                        }
                                    }
                                )
                            } else {
                                Box(modifier = Modifier.weight(1f))
                                IconButton(onClick = { isSearchExpanded = true }) {
                                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = { launcher.launch(arrayOf("audio/*", "video/*")) }) {
                                    Icon(androidx.compose.material.icons.Icons.Filled.Folder, contentDescription = "Browse Files", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        if (!isSearchExpanded) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sort: ", style = MaterialTheme.typography.bodyMedium)
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(onClick = { expanded = true }) {
                                        Text(sortOrder)
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        DropdownMenuItem(text = { Text("Name") }, onClick = { sortOrder = "Name"; expanded = false })
                                        DropdownMenuItem(text = { Text("Artist") }, onClick = { sortOrder = "Artist"; expanded = false })
                                        DropdownMenuItem(text = { Text("Album") }, onClick = { sortOrder = "Album"; expanded = false })
                                        DropdownMenuItem(text = { Text("Duration") }, onClick = { sortOrder = "Duration"; expanded = false })
                                    }
                                }
                            }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedAudios, key = { it.path }) { audio ->
                            val isSelected = selectedAudios.contains(audio.path)
                            AudioItem(
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
                                },
                                onInfoClick = { selectedAudioForInfo = audio }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedAudioForInfo?.let { audio ->
        AlertDialog(
            onDismissRequest = { selectedAudioForInfo = null },
            title = { Text("Audio Details", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Title: ${audio.title}")
                    Text("Artist: ${audio.artist}")
                    Text("Album: ${audio.album}")
                    Text("Path: ${audio.path}")
                    Text("Duration: ${formatDuration(audio.duration)}")
                    Text("Size: ${formatSize(audio.size)}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAudioForInfo = null }) {
                    Text("Close")
                }
            }
        )
    }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Add to Playlist") },
            text = {
                LazyColumn {
                    item {
                        Button(
                            onClick = { 
                                showPlaylistDialog = false
                                showCreatePlaylistDialog = true 
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create New Playlist")
                        }
                    }
                    items(playlists) { playlist ->
                        Text(
                            text = playlist.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addAudiosToPlaylist(playlist.id, selectedAudios.toList())
                                    showPlaylistDialog = false
                                    isSelectionMode = false
                                    selectedAudios.clear()
                                }
                                .padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        viewModel.createPlaylist(newPlaylistName, selectedAudios.toList())
                        newPlaylistName = ""
                        showCreatePlaylistDialog = false
                        isSelectionMode = false
                        selectedAudios.clear()
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AudioItem(audio: AudioEntity, onClick: () -> Unit, onLongClick: () -> Unit, isSelected: Boolean, selectionMode: Boolean, onInfoClick: () -> Unit) {
    val context = LocalContext.current
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val sArtworkUri = android.net.Uri.parse("content://media/external/audio/albumart")
                    val albumArtUri = android.content.ContentUris.withAppendedId(sArtworkUri, audio.albumId)
                    
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(albumArtUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Filled.Audiotrack)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audio.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val minutes = (audio.duration / 1000) / 60
                val seconds = (audio.duration / 1000) % 60
                val sizeMb = audio.size / (1024 * 1024f)
                val formattedSize = if (sizeMb > 0) String.format("%.1f MB", sizeMb) else ""
                Text(
                    text = "${audio.artist.takeIf { it != "<unknown>" } ?: "Unknown"} • ${String.format("%02d:%02d", minutes, seconds)} • $formattedSize",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onInfoClick) {
                Icon(Icons.Filled.Info, contentDescription = "Info")
            }
        }
    }
}
