package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri
import androidx.compose.foundation.border

data class QueueItem(
    val uri: String,
    val title: String,
    val isPlaying: Boolean
)

@Composable
fun MediaQueueSidebar(
    isVisible: Boolean,
    queue: List<QueueItem>,
    recentlyPlayed: List<QueueItem> = emptyList(),
    onClose: () -> Unit,
    onItemClick: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onMoveItem: (from: Int, to: Int) -> Unit,
    onPlayRecent: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/xmpegurl")) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            val writer = java.io.OutputStreamWriter(os)
                            writer.write("#EXTM3U\n")
                            queue.forEach { item ->
                                writer.write("#EXTINF:-1,${item.title}\n")
                                writer.write("${item.uri}\n")
                            }
                            writer.flush()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.fillMaxHeight().width(350.dp)
    ) {
        Surface(
            color = Color(0xE61A1A1A),
            modifier = Modifier.fillMaxSize().border(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current Queue", style = MaterialTheme.typography.titleLarge)
                    Row {
                        IconButton(onClick = { 
                            exportLauncher.launch("queue.m3u")
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Export Queue as M3U")
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Filled.Close, contentDescription = "Close Queue")
                        }
                    }
                }
                Divider(color = Color.White.copy(alpha = 0.1f))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    itemsIndexed(queue) { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(index) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.isPlaying) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Playing", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = item.title,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (item.isPlaying) MaterialTheme.colorScheme.primary else Color.White
                            )
                            Row {
                                IconButton(onClick = { if (index > 0) onMoveItem(index, index - 1) }) {
                                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move Up", tint = Color.White)
                                }
                                IconButton(onClick = { if (index < queue.size - 1) onMoveItem(index, index + 1) }) {
                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move Down", tint = Color.White)
                                }
                                IconButton(onClick = { onRemoveItem(index) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color.White)
                                }
                            }
                        }
                    }
                    if (recentlyPlayed.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Recently Played", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color.White.copy(alpha = 0.1f))
                        }
                        itemsIndexed(recentlyPlayed) { _, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlayRecent(item.uri) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.title,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
