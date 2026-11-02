package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.pow
import com.example.data.model.VideoEntity
import com.example.ui.viewmodels.MediaViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VideosScreen(
    viewModel: MediaViewModel,
    onVideoClick: (String) -> Unit
) {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            onVideoClick(it.toString())
        }
    }

    val permissionString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(permissionString)

    val videos by viewModel.videos.collectAsStateWithLifecycle()
    
    var sortOrder by remember { mutableStateOf("Name") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedVideoForInfo by remember { mutableStateOf<VideoEntity?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var inSelectionMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<String>() }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    
    val commonRoot = remember(videos) {
        if (videos.isEmpty()) "/" else {
            var prefix = videos.first().path.substringBeforeLast('/')
            for (v in videos) {
                val p = v.path.substringBeforeLast('/')
                while (!p.startsWith(prefix) && prefix.isNotEmpty()) {
                    val lastSlash = prefix.lastIndexOf('/')
                    if (lastSlash == -1) {
                        prefix = ""
                        break
                    }
                    prefix = prefix.substring(0, lastSlash)
                }
            }
            if (prefix.isEmpty()) "/" else prefix
        }
    }
    
    var currentPath by remember(commonRoot) { mutableStateOf<String>(commonRoot) }
    
    BackHandler(enabled = currentPath != commonRoot && !isSearchExpanded) {
        val lastSlash = currentPath.lastIndexOf('/')
        if (lastSlash != -1 && currentPath.substring(0, lastSlash).startsWith(commonRoot)) {
            currentPath = currentPath.substring(0, lastSlash)
        } else {
            currentPath = commonRoot
        }
    }

    val itemsToDisplay = remember(videos, currentPath, sortOrder, searchQuery, isSearchExpanded) {
        val isSearching = searchQuery.isNotBlank() && isSearchExpanded
        val filteredVideos = if (isSearching) {
            videos.filter { it.title.contains(searchQuery, ignoreCase = true) }
        } else videos
        
        val sortedVideos = when (sortOrder) {
            "Size" -> filteredVideos.sortedByDescending { it.size }
            "Duration" -> filteredVideos.sortedByDescending { it.duration }
            "Date Modified" -> filteredVideos.sortedByDescending { it.lastPlayed }
            else -> filteredVideos.sortedBy { it.title.lowercase() }
        }
        
        if (isSearching) {
            sortedVideos
        } else {
            val currentPrefix = if (currentPath == "/") "/" else "$currentPath/"
            val folders = mutableSetOf<String>()
            val vids = mutableListOf<VideoEntity>()
            for (v in sortedVideos) {
                if (v.path.startsWith(currentPrefix)) {
                    val remaining = v.path.substring(currentPrefix.length)
                    val slashIndex = remaining.indexOf('/')
                    if (slashIndex == -1) {
                        vids.add(v)
                    } else {
                        folders.add(currentPrefix + remaining.substring(0, slashIndex))
                    }
                }
            }
            
            val sortedFolders = folders.sortedBy { it.substringAfterLast('/').lowercase() }
            sortedFolders + vids
        }
    }

    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            viewModel.scanMedia()
            viewModel.observeMediaChanges(context)
        } else {
            permissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (!permissionState.status.isGranted) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Button(onClick = { permissionState.launchPermissionRequest() }) {
                    Text("Grant Storage Permission")
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        if (inSelectionMode) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                IconButton(onClick = { 
                                    inSelectionMode = false
                                    selectedItems.clear()
                                }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Cancel selection", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                Text("${selectedItems.size} selected", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row {
                                IconButton(onClick = { showPlaylistDialog = true }) {
                                    Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add to Playlist", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = { 
                                    // mock delete
                                    inSelectionMode = false
                                    selectedItems.clear()
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        } else if (isSearchExpanded) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search folders & videos") },
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
                            // Breadcrumb
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val remaining = currentPath.removePrefix(commonRoot)
                                val parts = remaining.split("/").filter { it.isNotEmpty() }
                                
                                item {
                                    Text(
                                        text = "Internal Storage",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (parts.isEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { currentPath = commonRoot }.padding(8.dp)
                                    )
                                }
                                var acc = commonRoot
                                parts.forEachIndexed { index, part ->
                                    item {
                                        Text("/", modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        acc += "/$part"
                                        val pathForClick = acc // capture for lambda
                                        Text(
                                            text = part,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (index == parts.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable { currentPath = pathForClick }.padding(8.dp)
                                        )
                                    }
                                }
                            }
                            
                            IconButton(onClick = { isSearchExpanded = true }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { launcher.launch(arrayOf("video/*", "audio/*")) }) {
                                Icon(Icons.Filled.Folder, contentDescription = "Browse Files", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    if (!isSearchExpanded && !inSelectionMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sort: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { expanded = true }) {
                                    Text(sortOrder, color = MaterialTheme.colorScheme.primary)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(text = { Text("Name") }, onClick = { sortOrder = "Name"; expanded = false })
                                    DropdownMenuItem(text = { Text("Date Modified") }, onClick = { sortOrder = "Date Modified"; expanded = false })
                                    DropdownMenuItem(text = { Text("Size") }, onClick = { sortOrder = "Size"; expanded = false })
                                    DropdownMenuItem(text = { Text("Duration") }, onClick = { sortOrder = "Duration"; expanded = false })
                                }
                            }
                        }
                    }
                    if (videos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyVideosState(
                                onBrowseFiles = { launcher.launch(arrayOf("video/*", "audio/*")) },
                                onRescan = { viewModel.scanMedia() }
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(itemsToDisplay, key = { if (it is String) it else (it as VideoEntity).path }) { item ->
                                if (item is String) {
                                    FolderItem(
                                        path = item, 
                                        isSelected = selectedItems.contains(item),
                                        inSelectionMode = inSelectionMode,
                                        onSelect = { 
                                            if (selectedItems.contains(item)) selectedItems.remove(item) 
                                            else selectedItems.add(item)
                                            if (selectedItems.isEmpty()) inSelectionMode = false 
                                        },
                                        onClick = { currentPath = item },
                                        onLongClick = { 
                                            inSelectionMode = true
                                            selectedItems.add(item)
                                        }
                                    )
                                } else if (item is VideoEntity) {
                                    VideoItem(
                                        video = item, 
                                        isSelected = selectedItems.contains(item.path),
                                        inSelectionMode = inSelectionMode,
                                        onSelect = { 
                                            if (selectedItems.contains(item.path)) selectedItems.remove(item.path) 
                                            else selectedItems.add(item.path)
                                            if (selectedItems.isEmpty()) inSelectionMode = false
                                        },
                                        onClick = { onVideoClick(item.path) }, 
                                        onLongClick = { 
                                            inSelectionMode = true
                                            selectedItems.add(item.path)
                                        },
                                        onInfoClick = { selectedVideoForInfo = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        selectedVideoForInfo?.let { video ->
            AlertDialog(
                onDismissRequest = { selectedVideoForInfo = null },
                title = { Text("Video Details", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Title: ${video.title}")
                        Text("Path: ${video.path}")
                        Text("Duration: ${formatDuration(video.duration)}")
                        Text("Resolution: ${video.resolution}")
                        Text("Size: ${formatSize(video.size)}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedVideoForInfo = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FolderItem(path: String, isSelected: Boolean, inSelectionMode: Boolean, onSelect: () -> Unit, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    val title = if (path.substringAfterLast('/') == "") "Internal Storage" else path.substringAfterLast('/')
    val icon = if (path.substringAfterLast('/') == "") Icons.Filled.SdStorage else Icons.Filled.Folder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (inSelectionMode) {
                        onSelect()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    if (!inSelectionMode) {
                        onLongClick()
                    }
                }
            ),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = "Folder", tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (inSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onSelect() })
            }
        }
    }
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (kotlin.math.log10(size.toDouble()) / kotlin.math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

fun formatDuration(duration: Long): String {
    val totalSeconds = duration / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VideoItem(video: VideoEntity, isSelected: Boolean, inSelectionMode: Boolean, onSelect: () -> Unit, onClick: () -> Unit, onLongClick: () -> Unit, onInfoClick: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (inSelectionMode) {
                        onSelect()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    if (!inSelectionMode) {
                        onLongClick()
                    }
                }
            )
            .testTag("video_item_${video.id}"),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageLoader = remember {
                ImageLoader.Builder(context)
                    .components {
                        add(coil.decode.VideoFrameDecoder.Factory())
                    }
                    .build()
            }
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 16.dp)
                    .clip(RoundedCornerShape(8.dp))

                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(video.path)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Optional overlay icon
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val minutes = (video.duration / 1000) / 60
                val seconds = (video.duration / 1000) % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (inSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onSelect() })
            } else {
                IconButton(onClick = onInfoClick) {
                    Icon(Icons.Filled.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun EmptyVideosState(
    onBrowseFiles: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.VideocamOff,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Videos Found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No video files discovered on your device storage. Open a file directly or rescan your folders.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBrowseFiles,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Browse Files")
                }

                OutlinedButton(
                    onClick = onRescan,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Rescan")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyVideosStatePreview() {
    MaterialTheme {
        EmptyVideosState(
            onBrowseFiles = {},
            onRescan = {}
        )
    }
}
