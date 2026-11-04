package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.AudioEntity
import com.example.ui.viewmodel.AudioPlayerViewModel
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

// Clean title helper: strips raw file extensions, replaces underscores/hyphens, capitalizes nicely
fun cleanAudioTitle(rawTitle: String?, rawPath: String): String {
    val initialTitle = rawTitle?.takeIf { it.isNotBlank() }
        ?: (Uri.parse(rawPath).lastPathSegment ?: rawPath)
    
    var t = initialTitle
    val extensions = listOf(".mp3", ".m4a", ".wav", ".flac", ".aac", ".ogg", ".opus", ".wma")
    extensions.forEach { ext ->
        if (t.endsWith(ext, ignoreCase = true)) {
            t = t.substring(0, t.length - ext.length)
        }
    }

    t = t.replace(Regex("^[0-9]{1,3}[._\\-\\s]+"), "")
    t = t.replace("_", " ").replace("-", " ")
    t = t.trim().replace(Regex("\\s+"), " ")

    return t.ifBlank { "Unknown Track" }
}

fun cleanArtistName(artist: String?, album: String?): String {
    val cleanArtist = artist?.takeIf { it.isNotBlank() && it != "<unknown>" && it != "Unknown" } ?: "Unknown Artist"
    val cleanAlbum = album?.takeIf { it.isNotBlank() && it != "<unknown>" && it != "Unknown" }
    return if (cleanAlbum != null) "$cleanArtist · $cleanAlbum" else cleanArtist
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultPlayerTheme(
    currentAudio: AudioEntity?,
    decodedPath: String,
    imageRequest: ImageRequest,
    playerViewModel: AudioPlayerViewModel,
    mediaViewModel: MediaViewModel,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    onLyricsClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onSpeedClick: () -> Unit = {}
) {
    BasePlayerLayout(
        currentAudio = currentAudio,
        decodedPath = decodedPath,
        imageRequest = imageRequest,
        playerViewModel = playerViewModel,
        mediaViewModel = mediaViewModel,
        isPlaying = isPlaying,
        isShuffle = isShuffle,
        repeatMode = repeatMode,
        currentPosition = currentPosition,
        duration = duration,
        volume = volume,
        themeStyle = ThemeStyle.DEFAULT,
        onLyricsClick = onLyricsClick,
        onEqualizerClick = onEqualizerClick,
        onQueueClick = onQueueClick,
        onSleepTimerClick = onSleepTimerClick,
        onSpeedClick = onSpeedClick
    )
}

@Composable
fun CircularPlayerTheme(
    currentAudio: AudioEntity?,
    decodedPath: String,
    imageRequest: ImageRequest,
    playerViewModel: AudioPlayerViewModel,
    mediaViewModel: MediaViewModel,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    onLyricsClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onSpeedClick: () -> Unit = {}
) {
    BasePlayerLayout(
        currentAudio = currentAudio,
        decodedPath = decodedPath,
        imageRequest = imageRequest,
        playerViewModel = playerViewModel,
        mediaViewModel = mediaViewModel,
        isPlaying = isPlaying,
        isShuffle = isShuffle,
        repeatMode = repeatMode,
        currentPosition = currentPosition,
        duration = duration,
        volume = volume,
        themeStyle = ThemeStyle.VINYL,
        onLyricsClick = onLyricsClick,
        onEqualizerClick = onEqualizerClick,
        onQueueClick = onQueueClick,
        onSleepTimerClick = onSleepTimerClick,
        onSpeedClick = onSpeedClick
    )
}

@Composable
fun PurpleGlassTheme(
    currentAudio: AudioEntity?,
    decodedPath: String,
    imageRequest: ImageRequest,
    playerViewModel: AudioPlayerViewModel,
    mediaViewModel: MediaViewModel,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    onLyricsClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onSpeedClick: () -> Unit = {}
) {
    BasePlayerLayout(
        currentAudio = currentAudio,
        decodedPath = decodedPath,
        imageRequest = imageRequest,
        playerViewModel = playerViewModel,
        mediaViewModel = mediaViewModel,
        isPlaying = isPlaying,
        isShuffle = isShuffle,
        repeatMode = repeatMode,
        currentPosition = currentPosition,
        duration = duration,
        volume = volume,
        themeStyle = ThemeStyle.PURPLE_GLASS,
        onLyricsClick = onLyricsClick,
        onEqualizerClick = onEqualizerClick,
        onQueueClick = onQueueClick,
        onSleepTimerClick = onSleepTimerClick,
        onSpeedClick = onSpeedClick
    )
}

@Composable
fun TransparentGlassTheme(
    currentAudio: AudioEntity?,
    decodedPath: String,
    imageRequest: ImageRequest,
    playerViewModel: AudioPlayerViewModel,
    mediaViewModel: MediaViewModel,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    onLyricsClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onSpeedClick: () -> Unit = {}
) {
    BasePlayerLayout(
        currentAudio = currentAudio,
        decodedPath = decodedPath,
        imageRequest = imageRequest,
        playerViewModel = playerViewModel,
        mediaViewModel = mediaViewModel,
        isPlaying = isPlaying,
        isShuffle = isShuffle,
        repeatMode = repeatMode,
        currentPosition = currentPosition,
        duration = duration,
        volume = volume,
        themeStyle = ThemeStyle.MINIMAL_GLASS,
        onLyricsClick = onLyricsClick,
        onEqualizerClick = onEqualizerClick,
        onQueueClick = onQueueClick,
        onSleepTimerClick = onSleepTimerClick,
        onSpeedClick = onSpeedClick
    )
}

enum class ThemeStyle {
    DEFAULT, VINYL, PURPLE_GLASS, MINIMAL_GLASS
}

@Composable
private fun BasePlayerLayout(
    currentAudio: AudioEntity?,
    decodedPath: String,
    imageRequest: ImageRequest,
    playerViewModel: AudioPlayerViewModel,
    mediaViewModel: MediaViewModel,
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    themeStyle: ThemeStyle,
    onLyricsClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onSpeedClick: () -> Unit
) {
    var swipeOffsetX by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        swipeOffsetX += delta
    }

    val trackTitle = cleanAudioTitle(currentAudio?.title, currentAudio?.path ?: decodedPath)
    val artistName = cleanArtistName(currentAudio?.artist, currentAudio?.album)

    val primaryAccent = when (themeStyle) {
        ThemeStyle.DEFAULT -> Color(0xFFF0B849) // Golden Yellow
        ThemeStyle.VINYL -> Color(0xFFFF9F1C) // Deep Warm Amber
        ThemeStyle.PURPLE_GLASS -> Color(0xFFA855F7) // Vibrant Neon Purple
        ThemeStyle.MINIMAL_GLASS -> Color(0xFF38BDF8) // Crisp Electric Cyan
    }

    // Double tap overlay state for seeking +/- 10s
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Key responsive layout Column filling vertical height without empty spaces!
    Column(
        modifier = Modifier
            .fillMaxSize()
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (swipeOffsetX > 120f) {
                        playerViewModel.skipToPrevious()
                    } else if (swipeOffsetX < -120f) {
                        playerViewModel.skipToNext()
                    }
                    swipeOffsetX = 0f
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Artwork Container (Weighted weight(1f) ensures artwork expands/shrinks to fit screen perfectly)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PlayerArtworkContainer(
                trackTitle = trackTitle,
                imageRequest = imageRequest,
                isPlaying = isPlaying,
                themeStyle = themeStyle,
                primaryAccent = primaryAccent,
                onDoubleTapLeft = {
                    val target = (currentPosition - 10000L).coerceAtLeast(0L)
                    playerViewModel.seekTo(target)
                    seekFeedbackText = "-10s"
                    coroutineScope.launch {
                        delay(700)
                        seekFeedbackText = null
                    }
                },
                onDoubleTapRight = {
                    val target = (currentPosition + 10000L).coerceAtMost(duration.coerceAtLeast(0L))
                    playerViewModel.seekTo(target)
                    seekFeedbackText = "+10s"
                    coroutineScope.launch {
                        delay(700)
                        seekFeedbackText = null
                    }
                }
            )

            // Seek gesture overlay feedback badge
            androidx.compose.animation.AnimatedVisibility(
                visible = seekFeedbackText != null,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, primaryAccent)
                ) {
                    Text(
                        text = seekFeedbackText ?: "",
                        color = primaryAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Track Title, Artist & Like Heart Button
        PlayerTrackInfoRow(
            trackTitle = trackTitle,
            artistName = artistName,
            isLiked = currentAudio?.isLiked == true,
            primaryAccent = primaryAccent,
            onLikeToggle = {
                currentAudio?.let {
                    mediaViewModel.toggleAudioLike(it.path, !it.isLiked)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Progress Slider & Scrubber
        PlayerScrubberColumn(
            currentPosition = currentPosition,
            duration = duration,
            primaryAccent = primaryAccent,
            onSeek = { playerViewModel.seekTo(it) }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Playback Controls Row (Shuffle, Previous, Play/Pause, Next, Repeat)
        PlayerControlButtonsRow(
            isPlaying = isPlaying,
            isShuffle = isShuffle,
            repeatMode = repeatMode,
            primaryAccent = primaryAccent,
            onPlayPause = { playerViewModel.playPause() },
            onSkipPrevious = { playerViewModel.skipToPrevious() },
            onSkipNext = { playerViewModel.skipToNext() },
            onShuffleToggle = { playerViewModel.setShuffleModeEnabled(!isShuffle) },
            onRepeatToggle = {
                val nextMode = when (repeatMode) {
                    androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                    androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                    else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                }
                playerViewModel.setRepeatMode(nextMode)
            }
        )

        Spacer(modifier = Modifier.height(22.dp))

        // 5. Bottom Action Chips Bar (Lyrics, Equalizer, Sleep Timer, Speed, Queue)
        val sleepTimerRemaining by playerViewModel.sleepTimerRemaining.collectAsState()
        val playbackSpeed by playerViewModel.playbackSpeed.collectAsState()

        PlayerActionChipsRow(
            primaryAccent = primaryAccent,
            sleepTimerRemaining = sleepTimerRemaining,
            playbackSpeed = playbackSpeed,
            onLyricsClick = onLyricsClick,
            onEqualizerClick = onEqualizerClick,
            onQueueClick = onQueueClick,
            onSleepTimerClick = onSleepTimerClick,
            onSpeedClick = onSpeedClick
        )

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun PlayerArtworkContainer(
    trackTitle: String,
    imageRequest: ImageRequest,
    isPlaying: Boolean,
    themeStyle: ThemeStyle,
    primaryAccent: Color,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit
) {
    // Continuous rotation for vinyl or subtle vinyl disc center
    val infiniteTransition = rememberInfiniteTransition(label = "artwork_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_anim"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    // Pulse animation for ambient glow
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2) {
                            onDoubleTapLeft()
                        } else {
                            onDoubleTapRight()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Ambient double-layer blurred halo glow
        Box(
            modifier = Modifier
                .size(260.dp)
                .blur(36.dp)
                .background(primaryAccent.copy(alpha = if (isPlaying) glowPulse else 0.2f), CircleShape)
        )

        when (themeStyle) {
            ThemeStyle.VINYL, ThemeStyle.DEFAULT -> {
                // Spinning Vinyl Record Design
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(CircleShape)
                        .rotate(currentRotation)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF322E2B),
                                    Color(0xFF1E1A17),
                                    Color(0xFF120F0D)
                                )
                            )
                        )
                        .border(
                            BorderStroke(
                                1.5.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF423D38),
                                        primaryAccent.copy(alpha = 0.5f),
                                        Color(0xFF423D38)
                                    )
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Concentric vinyl grooves
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val c = center
                        val r = size.minDimension / 2
                        drawCircle(color = Color(0xFF25201C), radius = r * 0.88f, center = c)
                        drawCircle(color = Color(0xFF1B1715), radius = r * 0.72f, center = c)
                        drawCircle(color = Color(0xFF2B2521), radius = r * 0.56f, center = c)
                    }

                    // Center Label / Album Artwork Disc
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(primaryAccent)
                            .border(BorderStroke(2.dp, Color(0xFF120F0D)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        var isImageError by remember(imageRequest) { mutableStateOf(false) }

                        if (!isImageError) {
                            SubcomposeAsyncImage(
                                model = imageRequest,
                                contentDescription = "Album Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                onError = { isImageError = true }
                            )
                        }

                        if (isImageError) {
                            // High-end fallback visualizer
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize().background(
                                    brush = Brush.verticalGradient(
                                        listOf(primaryAccent, Color(0xFF120F0D))
                                    )
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = trackTitle.firstOrNull()?.uppercase() ?: "🎶",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        // Vinyl center spindle hole
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0D0F10))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)), CircleShape)
                        )
                    }
                }
            }

            ThemeStyle.PURPLE_GLASS, ThemeStyle.MINIMAL_GLASS -> {
                // Elevated Glassmorphic Square Artwork Card
                Surface(
                    modifier = Modifier
                        .size(270.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    color = if (themeStyle == ThemeStyle.PURPLE_GLASS) Color(0x338B5CF6) else Color(0x22FFFFFF),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                primaryAccent.copy(alpha = 0.7f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        var isImageError by remember(imageRequest) { mutableStateOf(false) }

                        if (!isImageError) {
                            SubcomposeAsyncImage(
                                model = imageRequest,
                                contentDescription = "Album Artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                onError = { isImageError = true }
                            )
                        }

                        if (isImageError) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                primaryAccent.copy(alpha = 0.35f),
                                                Color(0xFF161819)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.GraphicEq,
                                        contentDescription = null,
                                        tint = primaryAccent,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = trackTitle,
                                        color = Color(0xFFE8E8E6),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTrackInfoRow(
    trackTitle: String,
    artistName: String,
    isLiked: Boolean,
    primaryAccent: Color,
    onLikeToggle: () -> Unit
) {
    var isLikedState by remember(isLiked) { mutableStateOf(isLiked) }
    val heartScale by animateFloatAsState(
        targetValue = if (isLikedState) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "heart_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = trackTitle,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                ),
                color = Color(0xFFE8E8E6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = artistName,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Color(0xFF92999D),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Heart Like Button
        Surface(
            modifier = Modifier
                .size(46.dp)
                .scale(heartScale)
                .clip(CircleShape)
                .clickable {
                    isLikedState = !isLikedState
                    onLikeToggle()
                },
            shape = CircleShape,
            color = Color(0xFF1E2224),
            border = BorderStroke(1.dp, if (isLikedState) primaryAccent.copy(alpha = 0.5f) else Color(0xFF2C3235))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isLikedState) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isLikedState) primaryAccent else Color(0xFFE8E8E6),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScrubberColumn(
    currentPosition: Long,
    duration: Long,
    primaryAccent: Color,
    onSeek: (Long) -> Unit
) {
    var dragPosition by remember { mutableStateOf<Long?>(null) }
    val displayPosition = dragPosition ?: currentPosition

    val progress = if (duration > 0) (displayPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = progress,
            onValueChange = {
                dragPosition = (it * duration).toLong()
            },
            onValueChangeFinished = {
                dragPosition?.let { onSeek(it) }
                dragPosition = null
            },
            colors = SliderDefaults.colors(
                thumbColor = primaryAccent,
                activeTrackColor = primaryAccent,
                inactiveTrackColor = Color(0xFF282D30)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTimeMs(displayPosition),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF92999D)
            )

            val remainingMs = (duration - displayPosition).coerceAtLeast(0L)
            Text(
                text = formatTimeMs(duration.coerceAtLeast(0L)),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF92999D)
            )
        }
    }
}

@Composable
private fun PlayerControlButtonsRow(
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    primaryAccent: Color,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit
) {
    // Press scale animation for Play/Pause button
    var isPressed by remember { mutableStateOf(false) }
    val playButtonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "play_scale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle Button with active state indicator
        IconButton(onClick = onShuffleToggle) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) primaryAccent else Color(0x66E8E8E6),
                    modifier = Modifier.size(24.dp)
                )
                if (isShuffle) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(primaryAccent)
                    )
                }
            }
        }

        // Previous Button
        IconButton(onClick = onSkipPrevious) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous Track",
                tint = Color(0xFFE8E8E6),
                modifier = Modifier.size(34.dp)
            )
        }

        // Center Play/Pause Circle Button
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(playButtonScale)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryAccent, primaryAccent.copy(alpha = 0.85f))
                    )
                )
                .clickable {
                    isPressed = true
                    onPlayPause()
                },
            contentAlignment = Alignment.Center
        ) {
            LaunchedEffect(isPressed) {
                if (isPressed) {
                    delay(120)
                    isPressed = false
                }
            }

            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.Black,
                modifier = Modifier.size(38.dp)
            )
        }

        // Next Button
        IconButton(onClick = onSkipNext) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next Track",
                tint = Color(0xFFE8E8E6),
                modifier = Modifier.size(34.dp)
            )
        }

        // Repeat Button with active mode indicator
        IconButton(onClick = onRepeatToggle) {
            val isRepeatActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF
            val repeatIcon = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = repeatIcon,
                    contentDescription = "Repeat",
                    tint = if (isRepeatActive) primaryAccent else Color(0x66E8E8E6),
                    modifier = Modifier.size(24.dp)
                )
                if (isRepeatActive) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(primaryAccent)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerActionChipsRow(
    primaryAccent: Color,
    sleepTimerRemaining: Long,
    playbackSpeed: Float,
    onLyricsClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onQueueClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onSpeedClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lyrics Chip
        PlayerChipPill(
            icon = Icons.Outlined.Description,
            label = "Lyrics",
            primaryAccent = primaryAccent,
            onClick = onLyricsClick
        )

        // Equalizer Chip
        PlayerChipPill(
            icon = Icons.Outlined.Tune,
            label = "Equalizer",
            primaryAccent = primaryAccent,
            onClick = onEqualizerClick
        )

        // Sleep Timer Chip
        val timerText = if (sleepTimerRemaining > 0) {
            val mins = (sleepTimerRemaining / 60000).coerceAtLeast(1)
            "${mins}m"
        } else {
            "Sleep"
        }

        PlayerChipPill(
            icon = Icons.Outlined.Bedtime,
            label = timerText,
            isActive = sleepTimerRemaining > 0,
            primaryAccent = primaryAccent,
            onClick = onSleepTimerClick
        )

        // Speed Selector Chip
        val speedText = if (playbackSpeed != 1.0f) "${playbackSpeed}x" else "Speed"
        PlayerChipPill(
            icon = Icons.Outlined.Speed,
            label = speedText,
            isActive = playbackSpeed != 1.0f,
            primaryAccent = primaryAccent,
            onClick = onSpeedClick
        )

        // Queue Chip
        PlayerChipPill(
            icon = Icons.Outlined.QueueMusic,
            label = "Queue",
            primaryAccent = primaryAccent,
            onClick = onQueueClick
        )
    }
}

@Composable
private fun PlayerChipPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean = false,
    primaryAccent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) primaryAccent.copy(alpha = 0.2f) else Color(0xFF1E2224),
        border = BorderStroke(1.dp, if (isActive) primaryAccent else Color(0xFF2C3235))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) primaryAccent else primaryAccent,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE8E8E6)
            )
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}
