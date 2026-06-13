package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodels.MediaViewModel
import com.example.data.model.PlaylistEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(viewModel: MediaViewModel, onPlaylistClick: (Int) -> Unit) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistNameInput by remember { mutableStateOf("") }
    
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedPlaylists = remember { mutableStateListOf<Int>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlists") },
                actions = {
                    if (isSelectionMode) {
                        if (selectedPlaylists.size == 1) {
                            IconButton(onClick = { 
                                val idToEdit = selectedPlaylists.first()
                                showEditDialog = playlists.find { it.id == idToEdit }
                                playlistNameInput = showEditDialog?.name ?: ""
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Playlist")
                            }
                        }
                        IconButton(onClick = { 
                            viewModel.deletePlaylists(selectedPlaylists)
                            selectedPlaylists.clear()
                            isSelectionMode = false 
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Selected")
                        }
                        IconButton(onClick = { 
                            selectedPlaylists.clear()
                            isSelectionMode = false 
                        }) {
                            Text("Cancel", modifier = Modifier.padding(end = 8.dp))
                        }
                    } else {
                        IconButton(onClick = { 
                            playlistNameInput = ""
                            showCreateDialog = true 
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "New Playlist")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                PlaylistItem(
                    playlist = com.example.data.model.PlaylistEntity(id = -1, name = "Liked Music"),
                    isSelected = false,
                    selectionMode = false,
                    onClick = { if (!isSelectionMode) onPlaylistClick(-1) },
                    onLongClick = {}
                )
            }
            if (playlists.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No playlists yet", color = Color.Gray)
                    }
                }
            } else {
                items(playlists, key = { it.id }) { playlist ->
                    val isSelected = selectedPlaylists.contains(playlist.id)
                    PlaylistItem(
                        playlist = playlist,
                        isSelected = isSelected,
                        selectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                if (isSelected) selectedPlaylists.remove(playlist.id) else selectedPlaylists.add(playlist.id)
                                if (selectedPlaylists.isEmpty()) isSelectionMode = false
                            } else {
                                onPlaylistClick(playlist.id)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedPlaylists.add(playlist.id)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog || showEditDialog != null) {
        val isEdit = showEditDialog != null
        AlertDialog(
            onDismissRequest = { 
                showCreateDialog = false
                showEditDialog = null 
            },
            title = { Text(if (isEdit) "Edit Playlist" else "Create Playlist") },
            text = {
                OutlinedTextField(
                    value = playlistNameInput,
                    onValueChange = { playlistNameInput = it },
                    label = { Text("Playlist Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (playlistNameInput.isNotBlank()) {
                        if (isEdit) {
                            showEditDialog?.let { viewModel.updatePlaylistName(it.id, playlistNameInput) }
                            isSelectionMode = false
                            selectedPlaylists.clear()
                        } else {
                            viewModel.createPlaylist(playlistNameInput)
                        }
                    }
                    showCreateDialog = false
                    showEditDialog = null
                }) {
                    Text(if (isEdit) "Save" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCreateDialog = false
                    showEditDialog = null 
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlaylistItem(
    playlist: PlaylistEntity,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(backgroundColor),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            if (selectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
            } else {
                Box(modifier = Modifier.size(48.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(playlist.name, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
