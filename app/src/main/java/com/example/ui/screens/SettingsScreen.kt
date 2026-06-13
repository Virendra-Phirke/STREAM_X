package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodels.ThemeManager

import com.example.ui.viewmodel.AudioPlayerViewModel

@Composable
fun SettingsScreen(playerViewModel: AudioPlayerViewModel, onClose: () -> Unit) {
    val selectedColor by ThemeManager.primaryColor.collectAsStateWithLifecycle()
    var showColorDialog by remember { mutableStateOf(false) }
    
        val enableCrossfade by ThemeManager.enableCrossfade.collectAsStateWithLifecycle()
        val enableDynamicBackground by ThemeManager.enableDynamicBackground.collectAsStateWithLifecycle()
        val ecoModeEnabled by ThemeManager.ecoModeEnabled.collectAsStateWithLifecycle()
    
    val sleepTimerRemaining by playerViewModel.sleepTimerRemaining.collectAsStateWithLifecycle()
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Theme & Design", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            TextButton(onClick = { ThemeManager.resetSettings() }) {
                Text("Reset", color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close Settings", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Primary Color", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ThemeManager.themes.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { ThemeManager.setPrimaryColor(color) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedColor == color) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected Theme",
                            tint = Color.Black
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xE61A1A1A))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .clickable { showColorDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Custom Theme",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Player UI Theme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))
        
        val playerThemeOptions = listOf("Default", "Circular Retrograde", "Purple Glass", "Transparent Glass")
        val currentPlayerTheme by ThemeManager.playerUiTheme.collectAsStateWithLifecycle()
        
        var expandedThemeMenu by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { expandedThemeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(currentPlayerTheme)
            }
            DropdownMenu(expanded = expandedThemeMenu, onDismissRequest = { expandedThemeMenu = false }) {
                playerThemeOptions.forEach { theme ->
                    DropdownMenuItem(
                        text = { Text(theme) },
                        onClick = { 
                            ThemeManager.setPlayerUiTheme(theme)
                            expandedThemeMenu = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Playback", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(Color(0xE61A1A1A))
                .border(1.dp, Color.White.copy(alpha = 0.1f), MaterialTheme.shapes.medium)
                .clickable { showSleepTimerDialog = true }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sleep Timer", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        if (sleepTimerRemaining > 0) {
                            val mins = sleepTimerRemaining / 60000
                            val secs = (sleepTimerRemaining % 60000) / 1000
                            Text("Playback will stop in $mins:${String.format("%02d", secs)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("Stop playback automatically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(Color(0xE61A1A1A))
                .border(1.dp, Color.White.copy(alpha = 0.1f), MaterialTheme.shapes.medium)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Eco-Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Save battery by disabling animations & reducing updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = ecoModeEnabled, onCheckedChange = { ThemeManager.setEcoModeEnabled(it) })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Creative Features", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(Color(0xE61A1A1A))
                .border(1.dp, Color.White.copy(alpha = 0.1f), MaterialTheme.shapes.medium)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dynamic Background", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Extract theme colors from album artwork", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = enableDynamicBackground, onCheckedChange = { ThemeManager.setEnableDynamicBackground(it) })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Crossfade Transitions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Smoothly fade audio between songs and play/pause", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = enableCrossfade, onCheckedChange = { ThemeManager.setEnableCrossfade(it) })
                }
            }
        }
        
        if (showColorDialog) {
            CustomColorPickerDialog(
                initialColor = selectedColor,
                onDismiss = { showColorDialog = false },
                onColorSelected = { 
                    ThemeManager.setPrimaryColor(it)
                    showColorDialog = false
                }
            )
        }
        
        if (showSleepTimerDialog) {
            val options = listOf(0 to "Off", 5 to "5 Minutes", 15 to "15 Minutes", 30 to "30 Minutes", 60 to "1 Hour", 120 to "2 Hours")
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                title = { Text("Sleep Timer") },
                text = {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(options.size) { index ->
                            val (minutes, label) = options[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerViewModel.setSleepTimer(minutes)
                                        showSleepTimerDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CustomColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }
    var alpha by remember { mutableFloatStateOf(initialColor.alpha) }
    
    val selectedColor = Color(red, green, blue, alpha)
    
    val presets = listOf(
        Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF06B6D4), 
        Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444),
        Color(0xFFEC4899), Color(0xFF6366F1), Color(0xFF14b8a6)
    )

    fun String.padZero() = if (this.length == 1) "0$this" else this
    val hexCode = "#${(alpha * 255).toInt().toString(16).padZero()}${(red * 255).toInt().toString(16).padZero()}${(green * 255).toInt().toString(16).padZero()}${(blue * 255).toInt().toString(16).padZero()}".uppercase()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(24.dp)),
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text(
                "Theme Color", 
                style = MaterialTheme.typography.titleLarge, 
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Color Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(selectedColor.copy(alpha = Math.max(0.2f, alpha)), selectedColor)
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = hexCode,
                        style = MaterialTheme.typography.titleMedium,
                        color = if ((red * 0.299 + green * 0.587 + blue * 0.114) > 0.5 && alpha > 0.5f) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Presets
                Text("Presets", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets.size) { index ->
                        val color = presets[index]
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    red = color.red
                                    green = color.green
                                    blue = color.blue
                                    alpha = color.alpha
                                }
                                .border(
                                    width = if (selectedColor.red == color.red && selectedColor.green == color.green && selectedColor.blue == color.blue) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Sliders
                Text("Custom", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                
                CustomColorSlider("R", red, Color.Red) { red = it }
                CustomColorSlider("G", green, Color.Green) { green = it }
                CustomColorSlider("B", blue, Color.Blue) { blue = it }
                CustomColorSlider("A", alpha, Color.White) { alpha = it }
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(selectedColor) },
                colors = ButtonDefaults.buttonColors(containerColor = selectedColor.copy(alpha = 1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply", color = if ((red * 0.299 + green * 0.587 + blue * 0.114) > 0.5 && alpha > 0.5f) Color.Black else Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CustomColorSlider(label: String, value: Float, thumbColor: Color, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(32.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.width(20.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = thumbColor,
                activeTrackColor = thumbColor.copy(alpha = 0.7f),
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            ),
            modifier = Modifier.weight(1f),
            thumb = {
                Box(
                    modifier = Modifier.size(16.dp).background(thumbColor, CircleShape).border(1.dp, Color.White, CircleShape)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp),
                    colors = SliderDefaults.colors(thumbColor = thumbColor, activeTrackColor = thumbColor.copy(alpha = 0.7f), inactiveTrackColor = Color.White.copy(alpha = 0.1f))
                )
            }
        )
    }
}
