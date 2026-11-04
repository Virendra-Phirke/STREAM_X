package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PlaylistEntity
import com.example.ui.viewmodel.MediaViewModel

data class PlaylistSampleItem(
    val id: Int,
    val name: String,
    val songCount: String,
    val coverType: String,
    val isLiked: Boolean = false
)

@Composable
fun PlaylistsScreen(
    viewModel: MediaViewModel,
    onPlaylistClick: (Int) -> Unit
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val likedAudio by viewModel.likedAudio.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistNameInput by remember { mutableStateOf("") }
    
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedPlaylists = remember { mutableStateListOf<Int>() }
    var sortOrder by remember { mutableStateOf("Recently played") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Real Playlist Data only
    val realPlaylistItems = remember(playlists, likedAudio) {
        val likedItem = PlaylistSampleItem(
            id = -1,
            name = "Liked music",
            songCount = "${likedAudio.size} songs",
            coverType = "LIKED_MUSIC",
            isLiked = true
        )
        val userLists = playlists.mapIndexed { idx, entity ->
            val cover = when (idx % 4) {
                0 -> "COMMUTE"
                1 -> "LATE_NIGHT"
                2 -> "CLASSICAL"
                else -> "WORKOUT"
            }
            PlaylistSampleItem(
                id = entity.id,
                name = entity.name,
                songCount = "0 songs",
                coverType = cover
            )
        }
        listOf(likedItem) + userLists
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F10))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Section
            PlaylistsHeader(
                totalCount = realPlaylistItems.size,
                isSelectionMode = isSelectionMode,
                selectedCount = selectedPlaylists.size,
                sortOrder = sortOrder,
                sortMenuExpanded = sortMenuExpanded,
                onSelectionModeToggle = { 
                    isSelectionMode = !isSelectionMode
                    if (!isSelectionMode) selectedPlaylists.clear()
                },
                onEditClick = {
                    val idToEdit = selectedPlaylists.firstOrNull()
                    if (idToEdit != null) {
                        showEditDialog = playlists.find { it.id == idToEdit }
                        playlistNameInput = showEditDialog?.name ?: ""
                    }
                },
                onDeleteClick = {
                    viewModel.deletePlaylists(selectedPlaylists.toList())
                    selectedPlaylists.clear()
                    isSelectionMode = false
                },
                onSortToggle = { sortMenuExpanded = !sortMenuExpanded },
                onSortOrderSelect = { selectedSort ->
                    sortOrder = selectedSort
                    sortMenuExpanded = false
                }
            )

            // 2-Column Grid of Real Playlists
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(realPlaylistItems, key = { it.id }) { item ->
                    val isSelected = selectedPlaylists.contains(item.id)
                    PlaylistCard(
                        item = item,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                if (isSelected) selectedPlaylists.remove(item.id) else selectedPlaylists.add(item.id)
                                if (selectedPlaylists.isEmpty()) isSelectionMode = false
                            } else {
                                onPlaylistClick(item.id)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode && item.id > 0) {
                                isSelectionMode = true
                                selectedPlaylists.add(item.id)
                            }
                        }
                    )
                }
            }
        }

        // Absolute Positioned Floating Action Button for New Playlist
        ExtendedFloatingActionButton(
            onClick = {
                playlistNameInput = ""
                showCreateDialog = true
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New Playlist",
                    tint = Color.White
                )
            },
            text = {
                Text(
                    text = "New playlist",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            },
            containerColor = Color(0xFFFF653F),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
        )

        // Create / Edit Dialog
        if (showCreateDialog || showEditDialog != null) {
            val isEdit = showEditDialog != null
            AlertDialog(
                onDismissRequest = {
                    showCreateDialog = false
                    showEditDialog = null
                },
                containerColor = Color(0xFF161819),
                titleContentColor = Color(0xFFE8E8E6),
                textContentColor = Color(0xFF92999D),
                title = { Text(if (isEdit) "Edit Playlist" else "Create Playlist", style = MaterialTheme.typography.titleLarge) },
                text = {
                    OutlinedTextField(
                        value = playlistNameInput,
                        onValueChange = { playlistNameInput = it },
                        label = { Text("Playlist Name", color = Color(0xFF92999D)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE8E8E6),
                            unfocusedTextColor = Color(0xFFE8E8E6),
                            focusedBorderColor = Color(0xFFFF653F),
                            unfocusedBorderColor = Color(0xFF25292A)
                        ),
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
                        Text(if (isEdit) "Save" else "Create", color = Color(0xFFFF653F))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCreateDialog = false
                        showEditDialog = null
                    }) {
                        Text("Cancel", color = Color(0xFF92999D))
                    }
                }
            )
        }
    }
}

@Composable
fun PlaylistsHeader(
    totalCount: Int,
    isSelectionMode: Boolean,
    selectedCount: Int,
    sortOrder: String,
    sortMenuExpanded: Boolean,
    onSelectionModeToggle: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSortToggle: () -> Unit,
    onSortOrderSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)
    ) {
        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playlists",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFE8E8E6)
            )

            if (isSelectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedCount == 1) {
                        IconButton(onClick = onEditClick) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Color(0xFFE8E8E6))
                        }
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFFF653F))
                    }
                }
            } else {
                IconButton(onClick = onSelectionModeToggle) {
                    Icon(
                        imageVector = Icons.Outlined.CheckBox,
                        contentDescription = "Select Playlists",
                        tint = Color(0xFFE8E8E6),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Sorting Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { onSortToggle() }
                ) {
                    Text(
                        text = sortOrder,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color(0xFFE8E8E6),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "↓",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color(0xFFE8E8E6)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = "Sort Options",
                        tint = Color(0xFFE8E8E6),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = onSortToggle,
                    modifier = Modifier.background(Color(0xFF161819))
                ) {
                    listOf("Recently played", "Name", "Date created").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = Color(0xFFE8E8E6)) },
                            onClick = { onSortOrderSelect(option) }
                        )
                    }
                }
            }

            Text(
                text = "$totalCount playlists",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Color(0xFF92999D)
            )
        }
    }
}

@Composable
fun PlaylistCard(
    item: PlaylistSampleItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // Square Artwork Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF242829)),
            contentAlignment = Alignment.Center
        ) {
            PlaylistCoverIllustration(type = item.coverType)

            if (isSelectionMode && isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckBox,
                        contentDescription = "Selected",
                        tint = Color(0xFFFF653F),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFFE8E8E6),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Subtitle Count
        Text(
            text = item.songCount,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = Color(0xFF92999D),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlaylistCoverIllustration(type: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            when (type) {
                "LIKED_MUSIC" -> {
                    drawRect(color = Color(0xFFFF653F), size = Size(w, h))
                }
                "COMMUTE" -> {
                    drawRect(color = Color(0xFF2C4A52), size = Size(w, h))
                    val stripeWidth = w * 0.22f
                    for (i in -2..5) {
                        drawLine(
                            color = Color(0xFF3B626C),
                            start = Offset(i * stripeWidth, 0f),
                            end = Offset((i + 2) * stripeWidth, h),
                            strokeWidth = 14f
                        )
                    }
                }
                "LATE_NIGHT" -> {
                    drawRect(color = Color(0xFF382A52), size = Size(w, h))
                    drawArc(
                        color = Color(0xFF9D78E0),
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(w * 0.1f, h * 0.1f),
                        size = Size(w * 1.3f, h * 1.3f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 16f)
                    )
                    drawArc(
                        color = Color(0xFF9D78E0).copy(alpha = 0.6f),
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(w * 0.35f, h * 0.35f),
                        size = Size(w * 0.8f, h * 0.8f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12f)
                    )
                }
                "CLASSICAL" -> {
                    drawRect(color = Color(0xFF4A3228), size = Size(w, h))
                    drawCircle(color = Color(0xFFF0B884), radius = h * 0.22f, center = Offset(w * 0.65f, h * 0.42f))
                }
                "WORKOUT" -> {
                    drawRect(color = Color(0xFFFF5733), size = Size(w * 0.45f, h))
                    drawRect(color = Color(0xFF2B3032), topLeft = Offset(w * 0.45f, 0f), size = Size(w * 0.55f, h))
                }
                else -> {
                    drawRect(color = Color(0xFF242829), size = Size(w, h))
                }
            }
        }

        if (type == "LIKED_MUSIC") {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Liked Music",
                    tint = Color(0xFF161819),
                    modifier = Modifier.size(54.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistsScreenPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0F10))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PlaylistsHeader(
                    totalCount = 1,
                    isSelectionMode = false,
                    selectedCount = 0,
                    sortOrder = "Recently played",
                    sortMenuExpanded = false,
                    onSelectionModeToggle = {},
                    onEditClick = {},
                    onDeleteClick = {},
                    onSortToggle = {},
                    onSortOrderSelect = {}
                )
            }
        }
    }
}
