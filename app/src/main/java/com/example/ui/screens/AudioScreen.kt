package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.AudioEntity
import com.example.ui.viewmodel.MediaViewModel
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.text.style.TextAlign

data class AudioSampleItem(
    val title: String,
    val artist: String,
    val duration: String,
    val isLiked: Boolean,
    val coverType: String,
    val indexLetter: String? = null,
    val path: String = "",
)

@Composable
fun AudioScreen(
    viewModel: MediaViewModel,
    onAudioClick: (String) -> Unit,
) {
    val audios by viewModel.audios.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf("Name") }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedAudioForInfo by remember { mutableStateOf<AudioEntity?>(null) }

    // Launcher for file picker
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            onAudioClick(it.toString())
        }
    }

    val displayList = remember(audios, searchQuery, sortOrder) {
        audios.filter {
            searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true)
        }.let { list ->
            when (sortOrder) {
                "Artist" -> list.sortedBy { it.artist.lowercase() }
                "Album" -> list.sortedBy { it.album.lowercase() }
                "Duration" -> list.sortedByDescending { it.duration }
                else -> list.sortedBy { it.title.lowercase() }
            }
        }.mapIndexed { idx, entity ->
            val coverType = when (idx % 7) {
                0 -> "CIRCLE_DOT"
                1 -> "EQUALIZER_BARS"
                2 -> "DIAGONAL_STRIPES"
                3 -> "GRID_DOTS"
                4 -> "SPLIT_ORANGE"
                5 -> "SPLIT_CREAM"
                else -> "RADIO_WAVES"
            }
            val showIndexLetter = idx == 0 || entity.title.firstOrNull()?.uppercaseChar() != audios.getOrNull(idx - 1)?.title?.firstOrNull()?.uppercaseChar()
            AudioSampleItem(
                title = entity.title,
                artist = entity.artist.takeIf { it != "<unknown>" } ?: "Unknown Artist",
                duration = formatDuration(entity.duration),
                isLiked = entity.isLiked,
                coverType = coverType,
                indexLetter = if (showIndexLetter) entity.title.firstOrNull()?.uppercase() else null,
                path = entity.path,
            )
        }
    }

    val totalSongsCount = audios.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F10)),
    ) {
        if (audios.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                AudioHeader(
                    totalSongs = 0,
                    searchQuery = searchQuery,
                    sortOrder = sortOrder,
                    sortMenuExpanded = sortMenuExpanded,
                    onSearchQueryChange = { searchQuery = it },
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
                            title = "Checking audio files...",
                            subtitle = "Scanning device storage for music content",
                        )
                    } else {
                        EmptyAudioState(
                            onBrowseFiles = { fileLauncher.launch(arrayOf("audio/*", "video/*")) },
                            onRescan = { viewModel.forceScanMedia() },
                        )
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // Main Song List Column
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    item {
                        AudioHeader(
                            totalSongs = totalSongsCount,
                            searchQuery = searchQuery,
                            sortOrder = sortOrder,
                            sortMenuExpanded = sortMenuExpanded,
                            onSearchQueryChange = { searchQuery = it },
                            onSortToggle = { sortMenuExpanded = !sortMenuExpanded },
                            onSortOrderSelect = { selectedSort ->
                                sortOrder = selectedSort
                                sortMenuExpanded = false
                            },
                        )
                    }

                    items(displayList, key = { it.title + it.path }) { item ->
                        AudioListItem(
                            item = item,
                            onClick = {
                                if (item.path.isNotEmpty()) {
                                    onAudioClick(item.path)
                                } else {
                                    fileLauncher.launch(arrayOf("audio/*", "video/*"))
                                }
                            },
                            onLikeToggle = {
                                if (item.path.isNotEmpty()) {
                                    viewModel.toggleAudioLike(item.path, !item.isLiked)
                                }
                            },
                        )
                    }
                }

                // Fast Index Scroll Sidebar on Right Margin
                Column(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight()
                        .padding(top = 100.dp, bottom = 16.dp, end = 4.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    listOf("A", "D", "G", "J", "M", "P", "S", "V", "Y", "#").forEach { letter ->
                        Text(
                            text = letter,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color(0xFF92999D),
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
                },
            )
        }
    }
}

@Composable
fun AudioHeader(
    totalSongs: Int,
    searchQuery: String,
    sortOrder: String,
    sortMenuExpanded: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSortToggle: () -> Unit,
    onSortOrderSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
    ) {
        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val ecoModeEnabled by com.example.ui.viewmodel.ThemeManager.ecoModeEnabled.collectAsStateWithLifecycle()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Audio",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color(0xFFE8E8E6),
                )
                if (ecoModeEnabled) {
                    EcoModeBadge()
                }
            }

            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More Options",
                tint = Color(0xFFE8E8E6),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search songs or artists", color = Color(0xFF92999D), fontSize = 15.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF92999D),
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = Color(0xFFE8E8E6))
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF161819),
                unfocusedContainerColor = Color(0xFF161819),
                focusedBorderColor = Color(0xFF25292A),
                unfocusedBorderColor = Color(0xFF25292A),
                focusedTextColor = Color(0xFFE8E8E6),
                unfocusedTextColor = Color(0xFFE8E8E6),
            ),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sorting Row
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
                        text = "↑",
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
                    listOf("Name", "Artist", "Album", "Duration").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = Color(0xFFE8E8E6)) },
                            onClick = { onSortOrderSelect(option) },
                        )
                    }
                }
            }

            Text(
                text = "$totalSongs songs",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Color(0xFF92999D),
            )
        }
    }
}

@Composable
fun AudioListItem(
    item: AudioSampleItem,
    onClick: () -> Unit,
    onLikeToggle: () -> Unit,
) {
    val isPlayingTrack = item.title == "Evening raga, Bhimpalasi"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail Cover Art
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF242829)),
        ) {
            if (item.path.isNotEmpty()) {
                val context = LocalContext.current
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.path)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = {
                        AudioCoverIllustration(type = item.coverType)
                    },
                )
            } else {
                AudioCoverIllustration(type = item.coverType)
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Artist Column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = if (isPlayingTrack) Color(0xFFFF653F) else Color(0xFFE8E8E6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = item.artist,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Color(0xFF92999D),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Duration Text
        Text(
            text = item.duration,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = Color(0xFF92999D),
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Like Heart Icon
        IconButton(
            onClick = onLikeToggle,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = if (item.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Like",
                tint = if (item.isLiked) Color(0xFFFF653F) else Color(0xFF92999D),
                modifier = Modifier.size(20.dp),
            )
        }

        // Fast Index Letter indicator (if present)
        item.indexLetter?.let { letter ->
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = letter,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = Color(0xFF92999D),
            )
        }
    }
}

@Composable
fun AudioCoverIllustration(type: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            when (type) {
                "CIRCLE_DOT" -> {
                    drawRect(color = Color(0xFF4A3228), size = Size(w, h))
                    drawCircle(color = Color(0xFFF0B884), radius = h * 0.20f, center = Offset(w * 0.60f, h * 0.40f))
                }
                "EQUALIZER_BARS" -> {
                    drawRect(color = Color(0xFF1E2224), size = Size(w, h))
                    val barWidth = w * 0.12f
                    val barGap = w * 0.08f
                    val heights = listOf(0.4f, 0.75f, 0.5f, 0.85f)
                    heights.forEachIndexed { i, factor ->
                        val x = w * 0.18f + i * (barWidth + barGap)
                        val barH = h * factor
                        drawRect(
                            color = Color(0xFFFF653F),
                            topLeft = Offset(x, h - barH - 8f),
                            size = Size(barWidth, barH),
                        )
                    }
                }
                "DIAGONAL_STRIPES" -> {
                    drawRect(color = Color(0xFF2C4A52), size = Size(w, h))
                    val stripeWidth = w * 0.25f
                    for (i in -2..4) {
                        drawLine(
                            color = Color(0xFF3B626C),
                            start = Offset(i * stripeWidth, 0f),
                            end = Offset((i + 2) * stripeWidth, h),
                            strokeWidth = 8f,
                        )
                    }
                }
                "GRID_DOTS" -> {
                    drawRect(color = Color(0xFF383220), size = Size(w, h))
                    val rows = 4
                    val cols = 4
                    val dx = w / (cols + 1)
                    val dy = h / (rows + 1)
                    for (r in 1..rows) {
                        for (col in 1..cols) {
                            drawCircle(
                                color = Color(0xFFF0C868),
                                radius = 3f,
                                center = Offset(col * dx, r * dy),
                            )
                        }
                    }
                }
                "SPLIT_ORANGE" -> {
                    drawRect(color = Color(0xFFFF5733), size = Size(w * 0.45f, h))
                    drawRect(color = Color(0xFF2B3032), topLeft = Offset(w * 0.45f, 0f), size = Size(w * 0.55f, h))
                }
                "SPLIT_CREAM" -> {
                    drawRect(color = Color(0xFFF0E6D2), size = Size(w, h * 0.45f))
                    drawRect(color = Color(0xFF2D383C), topLeft = Offset(0f, h * 0.45f), size = Size(w, h * 0.55f))
                }
                "RADIO_WAVES" -> {
                    drawRect(color = Color(0xFF382A52), size = Size(w, h))
                    drawArc(
                        color = Color(0xFF9D78E0),
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(w * 0.1f, h * 0.1f),
                        size = Size(w * 1.3f, h * 1.3f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f),
                    )
                }
                else -> {
                    drawRect(color = Color(0xFF242829), size = Size(w, h))
                }
            }
        }
    }
}

@Composable
fun EmptyAudioState(
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
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFFFF653F),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Audio Files Found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE8E8E6),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No music or audio tracks discovered on your device storage. Pick a file directly or rescan your folders.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF92999D),
                textAlign = TextAlign.Center,
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

@Preview(showBackground = true)
@Composable
fun AudioScreenPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0F10)),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AudioHeader(
                    totalSongs = 214,
                    searchQuery = "",
                    sortOrder = "Name",
                    sortMenuExpanded = false,
                    onSearchQueryChange = {},
                    onSortToggle = {},
                    onSortOrderSelect = {},
                )
                AudioListItem(
                    item = AudioSampleItem("Aaj Jaane Ki Zid Na Karo", "Farida Khanum", "5:41", true, "CIRCLE_DOT"),
                    onClick = {},
                    onLikeToggle = {},
                )
            }
        }
    }
}
