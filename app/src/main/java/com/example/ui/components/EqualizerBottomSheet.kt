package com.example.ui.components

import android.media.audiofx.Equalizer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(
    audioSessionId: Int,
    onDismissRequest: () -> Unit
) {
    var isEqEnabled by remember { mutableStateOf(true) }
    var selectedPresetName by remember { mutableStateOf("Rock") }
    
    val presets = listOf("Flat", "Bass boost", "Rock", "Pop", "Classical", "Jazz")

    // Default 5 band levels (dB)
    var band60Hz by remember { mutableFloatStateOf(5f) }
    var band230Hz by remember { mutableFloatStateOf(3f) }
    var band910Hz by remember { mutableFloatStateOf(-2f) }
    var band36kHz by remember { mutableFloatStateOf(3f) }
    var band14kHz by remember { mutableFloatStateOf(5f) }

    var equalizer by remember { mutableStateOf<Equalizer?>(null) }
    var eqErrorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(audioSessionId) {
        if (audioSessionId != 0) {
            try {
                val eq = Equalizer(0, audioSessionId)
                eq.enabled = true
                equalizer = eq
                eqErrorMessage = null
            } catch (e: Throwable) {
                eqErrorMessage = "Equalizer hardware effect unavailable on this audio session."
                e.printStackTrace()
            }
        } else {
            eqErrorMessage = "Audio session uninitialized. Start playback to enable equalizer."
        }
        onDispose {
            try {
                equalizer?.release()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF161819),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Drag Handle Bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF33383A))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Switch Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Equalizer",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFE8E8E6)
                )

                Switch(
                    checked = isEqEnabled,
                    onCheckedChange = {
                        isEqEnabled = it
                        try {
                            equalizer?.enabled = it
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFF0B849),
                        uncheckedThumbColor = Color(0xFF92999D),
                        uncheckedTrackColor = Color(0xFF242829)
                    )
                )
            }

            eqErrorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFF242829),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        color = Color(0xFFF0B849),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Preset Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { preset ->
                    val isSelected = preset == selectedPresetName
                    Surface(
                        onClick = {
                            selectedPresetName = preset
                            when (preset) {
                                "Flat" -> { band60Hz = 0f; band230Hz = 0f; band910Hz = 0f; band36kHz = 0f; band14kHz = 0f }
                                "Bass boost" -> { band60Hz = 8f; band230Hz = 5f; band910Hz = 1f; band36kHz = 0f; band14kHz = 0f }
                                "Rock" -> { band60Hz = 5f; band230Hz = 3f; band910Hz = -2f; band36kHz = 3f; band14kHz = 5f }
                                "Pop" -> { band60Hz = -1f; band230Hz = 2f; band910Hz = 6f; band36kHz = 3f; band14kHz = -2f }
                                "Classical" -> { band60Hz = 4f; band230Hz = 3f; band910Hz = 2f; band36kHz = 4f; band14kHz = 4f }
                                "Jazz" -> { band60Hz = 3f; band230Hz = 2f; band910Hz = 1f; band36kHz = 2f; band14kHz = 4f }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFF0B849) else Color(0xFF242829),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFFF0B849) else Color(0xFF33383A))
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (isSelected) Color.Black else Color(0xFFE8E8E6),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5 Band Sliders
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val bands = listOf(
                    Triple("60 Hz", band60Hz) { v: Float -> band60Hz = v },
                    Triple("230 Hz", band230Hz) { v: Float -> band230Hz = v },
                    Triple("910 Hz", band910Hz) { v: Float -> band910Hz = v },
                    Triple("3.6 kHz", band36kHz) { v: Float -> band36kHz = v },
                    Triple("14 kHz", band14kHz) { v: Float -> band14kHz = v }
                )

                bands.forEach { (label, value, onValChange) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${if (value > 0) "+" else ""}${value.toInt()} dB",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFE8E8E6)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Vertical Slider
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .width(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Slider(
                                value = value,
                                onValueChange = onValChange,
                                valueRange = -12f..12f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFF0B849),
                                    activeTrackColor = Color(0xFFF0B849),
                                    inactiveTrackColor = Color(0xFF33383A)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF92999D)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Range -12 to +12 dB",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = Color(0xFF92999D)
                )

                OutlinedButton(
                    onClick = {
                        band60Hz = 0f
                        band230Hz = 0f
                        band910Hz = 0f
                        band36kHz = 0f
                        band14kHz = 0f
                        selectedPresetName = "Flat"
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF33383A)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF161819))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Reset",
                        tint = Color(0xFFE8E8E6),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFE8E8E6)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
