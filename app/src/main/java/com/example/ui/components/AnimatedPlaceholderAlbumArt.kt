package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.collectAsState
import com.example.ui.viewmodels.ThemeManager

@Composable
fun AnimatedPlaceholderAlbumArt(modifier: Modifier = Modifier) {
    val ecoModeEnabled by ThemeManager.ecoModeEnabled.collectAsState()
    val infiniteTransition = rememberInfiniteTransition()

    // Pulse animation
    val pulseAlpha by if (ecoModeEnabled) androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0.5f) } else infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Spin animation
    val spinRotation by if (ecoModeEnabled) androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) } else infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Outer glow pulse (blur-xl animate-pulse)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(24.dp)
                .background(Color(0xFF3B82F6).copy(alpha = pulseAlpha), RoundedCornerShape(16.dp))
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Spinning gradient border (blur-sm animate-spin)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(4.dp)
                    .rotate(spinRotation)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFFA855F7))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            )

            // Inner dark container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp) // inset-1 equivalent
                    .background(Color(0xFF111827), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Bouncing bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFFA855F7))
                    val delays = listOf(0, 100, 200, 300)
                    
                    colors.forEachIndexed { index, color ->
                        val offset by if (ecoModeEnabled) androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0.5f) } else infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, delayMillis = delays[index], easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(48.dp * (0.5f + offset * 0.5f))
                                .background(color, CircleShape)
                        )
                    }
                }

                // Inner pulse gradient from bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF3B82F6).copy(alpha = 0.1f * pulseAlpha), Color.Transparent)
                            )
                        )
                )
            }
        }
        
        // Pinging dots (corners)
        val pingScale1 by if (ecoModeEnabled) androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(1f) } else infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 1.5f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse))
        val pingAlpha1 by if (ecoModeEnabled) androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) } else infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Restart))
        
        val pingScale2 by if (ecoModeEnabled) androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(1f) } else infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 1.5f, animationSpec = infiniteRepeatable(tween(1000, delayMillis = 100), RepeatMode.Reverse))
        val pingAlpha2 by if (ecoModeEnabled) androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) } else infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(1000, delayMillis = 100), RepeatMode.Restart))
        
        val pingScale3 by if (ecoModeEnabled) androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(1f) } else infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 1.5f, animationSpec = infiniteRepeatable(tween(1000, delayMillis = 200), RepeatMode.Reverse))
        val pingAlpha3 by if (ecoModeEnabled) androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) } else infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(1000, delayMillis = 200), RepeatMode.Restart))
        
        val pingScale4 by if (ecoModeEnabled) androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(1f) } else infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 1.5f, animationSpec = infiniteRepeatable(tween(1000, delayMillis = 300), RepeatMode.Reverse))
        val pingAlpha4 by if (ecoModeEnabled) androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) } else infiniteTransition.animateFloat(initialValue = 1f, targetValue = 0f, animationSpec = infiniteRepeatable(tween(1000, delayMillis = 300), RepeatMode.Restart))

        // top left
        Box(modifier = Modifier.offset(x = (-4).dp, y = (-4).dp).align(Alignment.TopStart).size(8.dp)) {
            Box(Modifier.fillMaxSize().blur(1.dp).background(Color(0xFF3B82F6).copy(alpha=pingAlpha1), CircleShape))
            Box(Modifier.fillMaxSize().background(Color(0xFF3B82F6), CircleShape))
        }
        // top right
        Box(modifier = Modifier.offset(x = 4.dp, y = (-4).dp).align(Alignment.TopEnd).size(8.dp)) {
            Box(Modifier.fillMaxSize().blur(1.dp).background(Color(0xFFA855F7).copy(alpha=pingAlpha2), CircleShape))
            Box(Modifier.fillMaxSize().background(Color(0xFFA855F7), CircleShape))
        }
        // bottom left
        Box(modifier = Modifier.offset(x = (-4).dp, y = 4.dp).align(Alignment.BottomStart).size(8.dp)) {
            Box(Modifier.fillMaxSize().blur(1.dp).background(Color(0xFF06B6D4).copy(alpha=pingAlpha3), CircleShape))
            Box(Modifier.fillMaxSize().background(Color(0xFF06B6D4), CircleShape))
        }
        // bottom right
        Box(modifier = Modifier.offset(x = 4.dp, y = 4.dp).align(Alignment.BottomEnd).size(8.dp)) {
            Box(Modifier.fillMaxSize().blur(1.dp).background(Color(0xFF3B82F6).copy(alpha=pingAlpha4), CircleShape))
            Box(Modifier.fillMaxSize().background(Color(0xFF3B82F6), CircleShape))
        }
    }
}
