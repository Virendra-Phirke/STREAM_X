package com.example.ui.components

import android.media.audiofx.Equalizer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(
    audioSessionId: Int,
    onDismissRequest: () -> Unit
) {
    var equalizer by remember { mutableStateOf<Equalizer?>(null) }
    var presetNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentPreset by remember { mutableStateOf<Short>(-1) }
    var bandLevels by remember { mutableStateOf<ShortArray>(ShortArray(0)) }
    var minLevel by remember { mutableStateOf<Short>(0) }
    var maxLevel by remember { mutableStateOf<Short>(0) }
    var bands by remember { mutableStateOf<Short>(0) }
    
    DisposableEffect(audioSessionId) {
        if (audioSessionId != 0) {
            try {
                val eq = Equalizer(0, audioSessionId)
                eq.enabled = true
                equalizer = eq
                
                val names = mutableListOf<String>()
                for (i in 0 until eq.numberOfPresets) {
                    names.add(eq.getPresetName(i.toShort()))
                }
                presetNames = names
                currentPreset = eq.currentPreset
                
                bands = eq.numberOfBands
                val levelRange = eq.bandLevelRange
                minLevel = levelRange[0]
                maxLevel = levelRange[1]
                
                val levels = ShortArray(bands.toInt())
                for (i in 0 until bands) {
                    levels[i] = eq.getBandLevel(i.toShort())
                }
                bandLevels = levels
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onDispose {
            equalizer?.enabled = false
            equalizer?.release()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(Modifier.padding(16.dp).padding(bottom = 32.dp).fillMaxWidth().fillMaxHeight(0.8f)) {
            Text("Equalizer", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            
            if (presetNames.isEmpty() && bands.toInt() == 0) {
                Text("Equalizer not available", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    if (bands > 0) {
                        item {
                            Text("Custom Bands", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                for (i in 0 until bands) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        var bandVal by remember(bandLevels) { mutableStateOf(bandLevels.getOrNull(i)?.toFloat() ?: 0f) }
                                        Text(
                                            text = try { "${equalizer?.getCenterFreq(i.toShort())?.div(1000)}Hz" } catch (e: Exception) { "" },
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.width(60.dp)
                                        )
                                        Slider(
                                            value = bandVal,
                                            onValueChange = { 
                                                bandVal = it
                                                try {
                                                    equalizer?.setBandLevel(i.toShort(), it.toInt().toShort())
                                                    currentPreset = -1
                                                } catch (e: Exception) {}
                                            },
                                            valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Presets", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    
                    items(presetNames.size) { index ->
                        val mode = presetNames[index]
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        equalizer?.usePreset(index.toShort())
                                        currentPreset = index.toShort()
                                        // update bands
                                        if (equalizer != null) {
                                            val levels = ShortArray(bands.toInt())
                                            for (i in 0 until bands) {
                                                levels[i] = equalizer!!.getBandLevel(i.toShort())
                                            }
                                            bandLevels = levels
                                        }
                                    } catch (e: Exception) {}
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(mode, modifier = Modifier.weight(1f))
                            if (index.toShort() == currentPreset) {
                                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
