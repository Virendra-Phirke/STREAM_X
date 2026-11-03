package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DriveFolderUpload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.VideoEntity
import com.example.ui.viewmodels.MediaViewModel
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun VideosScreen(
    viewModel: MediaViewModel,
    onVideoClick: (String) -> Unit,
) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    var selectedVideoForInfo by remember { mutableStateOf<VideoEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf("Date modified") }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var currentFolder by remember { mutableStateOf<String?>(null) }

    // System File Picker
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            onVideoClick(it.toString())
        }
    }

    // Extract real folder paths from scanned video entities
    val folderMap = remember(videos) {
        val map = mutableMapOf<String, Int>()
        videos.forEach { video ->
            val parentPath = video.path.substringBeforeLast('/', "")
            val folderName = if (parentPath.isNotEmpty()) parentPath.substringAfterLast('/') else "Storage"
            if (folderName.isNotEmpty()) {
                map[folderName] = (map[folderName] ?: 0) + 1
            }
        }
        map
    }

    // Filter and Sort Real Videos
    val filteredVideos = remember(videos, searchQuery, currentFolder, sortOrder) {
        videos.filter { video ->
            val parentPath = video.path.substringBeforeLast('/', "")
            val folderName = if (parentPath.isNotEmpty()) parentPath.substringAfterLast('/') else "Storage"
            val matchesFolder = (currentFolder == null || folderName.equals(currentFolder, ignoreCase = true))
            val matchesSearch = (searchQuery.isBlank() || video.title.contains(searchQuery, ignoreCase = true))
            matchesFolder && matchesSearch
        }.let { list ->
            when (sortOrder) {
                "Name" -> list.sortedBy { it.title.lowercase() }
                "Size" -> list.sortedByDescending { it.size }
                "Duration" -> list.sortedByDescending { it.duration }
                else -> list.sortedByDescending { it.id }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F10)),
    ) {
        if (videos.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                MainHeader(
                    itemCount = 0,
                    isSearchExpanded = isSearchExpanded,
                    searchQuery = searchQuery,
                    sortOrder = sortOrder,
                    sortMenuExpanded = sortMenuExpanded,
                    onSearchToggle = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) searchQuery = ""
                    },
                    onSearchQueryChange = { searchQuery = it },
                    onUploadClick = { fileLauncher.launch(arrayOf("video/*", "audio/*")) },
                    onSortToggle = { sortMenuExpanded = !sortMenuExpanded },
                    onSortOrderSelect = { selectedSort ->
                        sortOrder = selectedSort
                        sortMenuExpanded = false
                    },
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isScanning) {
                        MediaScanningLoader(
                            title = "Checking media files...",
                            subtitle = "Scanning device storage for video content",
                        )
                    } else {
                        EmptyVideosState(
                            onBrowseFiles = { fileLauncher.launch(arrayOf("video/*", "audio/*")) },
                            onRescan = { viewModel.forceScanMedia() },
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                // Main Header
                item {
                    MainHeader(
                        itemCount = filteredVideos.size,
                        isSearchExpanded = isSearchExpanded,
                        searchQuery = searchQuery,
                        sortOrder = sortOrder,
                        sortMenuExpanded = sortMenuExpanded,
                        onSearchToggle = {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) searchQuery = ""
                        },
                        onSearchQueryChange = { searchQuery = it },
                        onUploadClick = { fileLauncher.launch(arrayOf("video/*", "audio/*")) },
                        onSortToggle = { sortMenuExpanded = !sortMenuExpanded },
                        onSortOrderSelect = { selectedSort ->
                            sortOrder = selectedSort
                            sortMenuExpanded = false
                        },
                    )
                }

                // Real Folder Rows
                if (currentFolder == null && folderMap.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            folderMap.forEach { (folderName, count) ->
                                FolderRow(
                                    name = folderName,
                                    itemCount = count.toString(),
                                    onClick = { currentFolder = folderName },
                                )
                            }
                        }
                    }
                }

                // Real Video Items List
                item {
                    VideoSectionHeader(title = if (currentFolder != null) "VIDEOS IN $currentFolder" else "ALL VIDEOS")
                }

                items(filteredVideos, key = { it.path }) { video ->
                    RealVideoListItem(
                        video = video,
                        onClick = { onVideoClick(video.path) },
                        onInfoClick = { selectedVideoForInfo = video },
                    )
                }
            }
        }

        // Info Dialog
        selectedVideoForInfo?.let { video ->
            AlertDialog(
                onDismissRequest = { selectedVideoForInfo = null },
                containerColor = Color(0xFF161819),
                titleContentColor = Color(0xFFE8E8E6),
                textContentColor = Color(0xFF92999D),
                title = { Text("Video Details", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Title: ${video.title}")
                        Text("Path: ${video.path}")
                        Text("Resolution: ${video.resolution.ifEmpty { "1080p" }}")
                        Text("Size: ${formatSize(video.size)}")
                        Text("Duration: ${formatDuration(video.duration)}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedVideoForInfo = null }) {
                        Text("Close", color = Color(0xFFFF653F))
                    }
                },
            )
        }
    }
}

@Composable
fun MainHeader(
    itemCount: Int,
    isSearchExpanded: Boolean,
    searchQuery: String,
    sortOrder: String,
    sortMenuExpanded: Boolean,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onUploadClick: () -> Unit,
    onSortToggle: () -> Unit,
    onSortOrderSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
    ) {
        // Title Row with Search and Import Icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSearchExpanded) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search videos & folders", color = Color(0xFF92999D)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE8E8E6),
                        unfocusedTextColor = Color(0xFFE8E8E6),
                        focusedBorderColor = Color(0xFFFF653F),
                        unfocusedBorderColor = Color(0xFF25292A),
                    ),
                    trailingIcon = {
                        IconButton(onClick = onSearchToggle) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close Search", tint = Color(0xFFE8E8E6))
                        }
                    },
                )
            } else {
                Text(
                    text = "Videos",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color(0xFFE8E8E6),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFE8E8E6),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onSearchToggle() },
                    )
                    Icon(
                        imageVector = Icons.Outlined.DriveFolderUpload,
                        contentDescription = "Upload Folder",
                        tint = Color(0xFFE8E8E6),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onUploadClick() },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Sorting Row with Dropdown Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { onSortToggle() },
                ) {
                    Text(
                        text = sortOrder,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color(0xFFE8E8E6),
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "↓",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color(0xFFE8E8E6),
                    )
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = "Sort Options",
                        tint = Color(0xFFE8E8E6),
                        modifier = Modifier.size(18.dp),
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = onSortToggle,
                    modifier = Modifier.background(Color(0xFF161819)),
                ) {
                    listOf("Date modified", "Name", "Size", "Duration").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = Color(0xFFE8E8E6)) },
                            onClick = { onSortOrderSelect(option) },
                        )
                    }
                }
            }

            Text(
                text = "$itemCount items",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Color(0xFF92999D),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Thin Divider Underneath
        HorizontalDivider(color = Color(0xFF25292A), thickness = 1.dp)
    }
}

@Composable
fun FolderRow(
    name: String,
    itemCount: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = null,
            tint = Color(0xFFE8E8E6),
            modifier = Modifier.size(22.dp),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = Color(0xFFE8E8E6),
            modifier = Modifier.weight(1f),
        )

        Text(
            text = itemCount,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = Color(0xFF92999D),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF92999D),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun VideoSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        ),
        color = Color(0xFF92999D),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp),
    )
}

@Composable
fun RealVideoListItem(
    video: VideoEntity,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Real Video Frame Thumbnail with Duration Badge
        Box(
            modifier = Modifier
                .width(112.dp)
                .height(63.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF242829)),
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(video.path)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF242829)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFFFF653F),
                            strokeWidth = 2.dp,
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2D4A3E)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = Color(0xFFE8E8E6),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
            )

            // Duration Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                color = Color.Black.copy(alpha = 0.82f),
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = formatDuration(video.duration),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Metadata Column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = Color(0xFFE8E8E6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${video.resolution.ifEmpty { "1080p" }} · ${formatSize(video.size)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Color(0xFF92999D),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Circular Info Icon
        IconButton(
            onClick = onInfoClick,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Details",
                tint = Color(0xFF92999D),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun MediaScanningLoader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(44.dp),
            color = Color(0xFFFF653F),
            strokeWidth = 3.dp,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = Color(0xFFE8E8E6),
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = Color(0xFF92999D),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun EmptyVideosState(
    onBrowseFiles: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161819)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25292A)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = Color(0xFF242829),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFFFF653F),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Videos Found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE8E8E6),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No video files discovered on your device storage. Pick a file directly or rescan your folders.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF92999D),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onBrowseFiles,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF653F)),
                ) {
                    Text(text = "Browse Files", color = Color.White)
                }

                OutlinedButton(
                    onClick = onRescan,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(text = "Rescan", color = Color(0xFFE8E8E6))
                }
            }
        }
    }
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

fun formatDuration(duration: Long): String {
    val totalSeconds = duration / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@Preview(showBackground = true)
@Composable
fun VideosScreenPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0F10)),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MainHeader(
                    itemCount = 0,
                    isSearchExpanded = false,
                    searchQuery = "",
                    sortOrder = "Date modified",
                    sortMenuExpanded = false,
                    onSearchToggle = {},
                    onSearchQueryChange = {},
                    onUploadClick = {},
                    onSortToggle = {},
                    onSortOrderSelect = {},
                )
            }
        }
    }
}
