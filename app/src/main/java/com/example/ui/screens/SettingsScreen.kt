package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AudioPlayerViewModel
import com.example.ui.viewmodels.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    playerViewModel: AudioPlayerViewModel
) {
    val selectedColor by ThemeManager.primaryColor.collectAsStateWithLifecycle()
    var showColorDialog by remember { mutableStateOf(false) }

    val enableCrossfade by ThemeManager.enableCrossfade.collectAsStateWithLifecycle()
    val enableDynamicBackground by ThemeManager.enableDynamicBackground.collectAsStateWithLifecycle()
    val ecoModeEnabled by ThemeManager.ecoModeEnabled.collectAsStateWithLifecycle()
    val currentPlayerTheme by ThemeManager.playerUiTheme.collectAsStateWithLifecycle()

    val sleepTimerRemaining by playerViewModel.sleepTimerRemaining.collectAsStateWithLifecycle()
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var expandedThemeMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F10))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp)
        ) {
            // Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFE8E8E6)
                    )

                    TextButton(onClick = { ThemeManager.resetSettings() }) {
                        Text("Reset", color = Color(0xFFFF653F), fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF25292A), thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Section 1: APPEARANCE & THEME
            item {
                SettingsSectionHeader(title = "THEME & APPEARANCE")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161819)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25292A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Accent Color",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFE8E8E6)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select primary accent color for UI highlights.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = Color(0xFF92999D)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Theme Color Palette Circles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ThemeManager.themes.take(5).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { ThemeManager.setPrimaryColor(color) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedColor == color) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Custom Palette Button
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF242829))
                                    .border(1.dp, Color(0xFF33383A), CircleShape)
                                    .clickable { showColorDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Palette,
                                    contentDescription = "Custom Color",
                                    tint = Color(0xFFFF653F),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = Color(0xFF25292A), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(20.dp))

                        // Player UI Theme Dropdown Selector
                        Text(
                            text = "Player Theme Style",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFE8E8E6)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Customize media player visual presentation.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = Color(0xFF92999D)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Box {
                            OutlinedButton(
                                onClick = { expandedThemeMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25292A)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF242829),
                                    contentColor = Color(0xFFE8E8E6)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(currentPlayerTheme, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color(0xFF92999D)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expandedThemeMenu,
                                onDismissRequest = { expandedThemeMenu = false },
                                modifier = Modifier.background(Color(0xFF161819))
                            ) {
                                listOf("Default", "Circular Retrograde", "Purple Glass", "Transparent Glass").forEach { theme ->
                                    DropdownMenuItem(
                                        text = { Text(theme, color = Color(0xFFE8E8E6)) },
                                        onClick = {
                                            ThemeManager.setPlayerUiTheme(theme)
                                            expandedThemeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section 2: PLAYBACK & AUTOMATION
            item {
                SettingsSectionHeader(title = "PLAYBACK & AUTOMATION")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161819)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25292A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Sleep Timer Item
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showSleepTimerDialog = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sleep Timer",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFE8E8E6)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (sleepTimerRemaining > 0) {
                                    val mins = sleepTimerRemaining / 60000
                                    val secs = (sleepTimerRemaining % 60000) / 1000
                                    Text(
                                        text = "Playback will stop in $mins:${String.format("%02d", secs)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = Color(0xFFFF653F),
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "Automatically stop playback after set time.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = Color(0xFF92999D)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = "Sleep Timer",
                                tint = Color(0xFFFF653F),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = Color(0xFF25292A), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(20.dp))

                        // Eco-Mode Item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Eco Mode",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFE8E8E6)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Optimize battery life by limiting UI polling.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = Color(0xFF92999D)
                                )
                            }

                            Switch(
                                checked = ecoModeEnabled,
                                onCheckedChange = { ThemeManager.setEcoModeEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFF653F),
                                    uncheckedThumbColor = Color(0xFF92999D),
                                    uncheckedTrackColor = Color(0xFF242829)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section 3: CREATIVE FEATURES
            item {
                SettingsSectionHeader(title = "CREATIVE FEATURES")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161819)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25292A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Dynamic Background
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dynamic Background",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFE8E8E6)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Extract theme tint from media album art.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = Color(0xFF92999D)
                                )
                            }

                            Switch(
                                checked = enableDynamicBackground,
                                onCheckedChange = { ThemeManager.setEnableDynamicBackground(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFF653F),
                                    uncheckedThumbColor = Color(0xFF92999D),
                                    uncheckedTrackColor = Color(0xFF242829)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = Color(0xFF25292A), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(20.dp))

                        // Crossfade Transitions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Crossfade Transitions",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFE8E8E6)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Smoothly fade audio when switching tracks.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = Color(0xFF92999D)
                                )
                            }

                            Switch(
                                checked = enableCrossfade,
                                onCheckedChange = { ThemeManager.setEnableCrossfade(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFF653F),
                                    uncheckedThumbColor = Color(0xFF92999D),
                                    uncheckedTrackColor = Color(0xFF242829)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Custom Palette Dialog
        if (showColorDialog) {
            CustomColorPickerDialog(
                initialColor = selectedColor,
                onDismiss = { showColorDialog = false },
                onColorSelected = { color ->
                    ThemeManager.setPrimaryColor(color)
                    showColorDialog = false
                }
            )
        }

        // Sleep Timer Dialog
        if (showSleepTimerDialog) {
            val options = listOf(
                0 to "Off",
                1 to "1 Minute",
                5 to "5 Minutes",
                15 to "15 Minutes",
                30 to "30 Minutes",
                60 to "1 Hour",
                120 to "2 Hours"
            )

            val currentMinutes = if (sleepTimerRemaining > 0) (sleepTimerRemaining / 60000).toInt().coerceAtLeast(1) else 0

            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                containerColor = Color(0xFF161819),
                titleContentColor = Color(0xFFE8E8E6),
                textContentColor = Color(0xFF92999D),
                title = { Text("Sleep Timer", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        options.forEach { (minutes, label) ->
                            val isSelected = (minutes == 0 && currentMinutes == 0) || (minutes > 0 && currentMinutes == minutes)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerViewModel.setSleepTimer(minutes)
                                        showSleepTimerDialog = false
                                    }
                                    .padding(vertical = 14.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) Color(0xFFFF653F) else Color(0xFFE8E8E6)
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFFFF653F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("Close", color = Color(0xFFFF653F))
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = Color(0xFF92999D),
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}

@Composable
fun CustomColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var red by remember { mutableFloatStateOf(initialColor.red) }
    var green by remember { mutableFloatStateOf(initialColor.green) }
    var blue by remember { mutableFloatStateOf(initialColor.blue) }

    val selectedColor = Color(red, green, blue, 1f)

    val presets = listOf(
        Color(0xFFFF653F), Color(0xFF8B5CF6), Color(0xFF3B82F6),
        Color(0xFF06B6D4), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFFEF4444), Color(0xFFEC4899)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161819),
        titleContentColor = Color(0xFFE8E8E6),
        title = { Text("Accent Color Palette", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(selectedColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Preview",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Color Presets", style = MaterialTheme.typography.labelMedium, color = Color(0xFF92999D))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presets.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    red = color.red
                                    green = color.green
                                    blue = color.blue
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(selectedColor) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF653F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF92999D))
            }
        }
    )
}
