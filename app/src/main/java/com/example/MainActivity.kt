package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.MiniPlayer
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AudioPlayerViewModel
import com.example.ui.viewmodel.MediaViewModel
import com.example.ui.viewmodel.MediaViewModelFactory
import com.example.ui.viewmodel.ThemeManager

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val imageLoader = coil.ImageLoader.Builder(this)
            .components {
                add(coil.decode.VideoFrameDecoder.Factory())
                add(com.example.ui.components.AudioCoverFetcher.Factory())
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)
        
        enableEdgeToEdge()
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.statusBars())

        setContent {
            val primaryColorFlow by ThemeManager.primaryColor.collectAsStateWithLifecycle()
            val ecoModeEnabled by ThemeManager.ecoModeEnabled.collectAsStateWithLifecycle()
            val primaryColor by animateColorAsState(
                targetValue = primaryColorFlow,
                animationSpec = tween(durationMillis = if (ecoModeEnabled) 0 else 700),
                label = "color_transition"
            )

            MyApplicationTheme(primaryColor = primaryColor) {
                val navController = rememberNavController()

                val container = (applicationContext as StreamXApplication).container
                val factory = MediaViewModelFactory(container.mediaRepository)
                val mediaViewModel: MediaViewModel = viewModel(factory = factory)
                val audioPlayerViewModel: AudioPlayerViewModel = viewModel()

                val audios by mediaViewModel.audios.collectAsStateWithLifecycle()
                LaunchedEffect(audios) {
                    if (audios.isNotEmpty()) {
                        audioPlayerViewModel.tryRestoreState(applicationContext, audios)
                    }
                }
                
                val context = LocalContext.current
                val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(
                        android.Manifest.permission.READ_MEDIA_AUDIO,
                        android.Manifest.permission.READ_MEDIA_VIDEO,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                } else {
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
                
                var permissionsGranted by remember { mutableStateOf(false) }
                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    permissionsGranted = result.values.all { it }
                    if (permissionsGranted) {
                        mediaViewModel.scanMedia()
                        mediaViewModel.observeMediaChanges(context)
                    }
                }

                LaunchedEffect(Unit) {
                    val hasPerms = permissions.all {
                        androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                    if (hasPerms) {
                        permissionsGranted = true
                        mediaViewModel.scanMedia()
                        mediaViewModel.observeMediaChanges(context)
                    } else {
                        permissionLauncher.launch(permissions)
                    }
                }

                if (!permissionsGranted) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Please grant media permissions to proceed.", color = Color.White)
                    }
                    return@MyApplicationTheme
                }

                Scaffold(
                    bottomBar = { 
                        Column {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            val isPlayer = currentRoute?.startsWith("player") == true || currentRoute?.startsWith("audio_player") == true
                            if (!isPlayer) {
                                MiniPlayer(
                                    playerViewModel = audioPlayerViewModel,
                                    mediaViewModel = mediaViewModel,
                                    onNavigateToPlayer = { path ->
                                        navController.navigate(Screen.AudioPlayer.createRoute(path))
                                    }
                                )
                            }
                            StreamXBottomNavigation(navController = navController)
                        } 
                    },
                    containerColor = Color(0xFF0D0F10),
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Videos.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Videos.route) {
                            VideosScreen(viewModel = mediaViewModel) { path ->
                                navController.navigate(Screen.Player.createRoute(path))
                            }
                        }
                        composable(Screen.Audio.route) {
                            AudioScreen(viewModel = mediaViewModel) { path ->
                                navController.navigate(Screen.AudioPlayer.createRoute(path, restart = true))
                            }
                        }
                        composable(Screen.Playlists.route) {
                            PlaylistsScreen(viewModel = mediaViewModel) { playlistId ->
                                navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                            }
                        }
                        composable(
                            route = Screen.PlaylistDetail.route,
                            arguments = listOf(navArgument("id") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val playlistId = backStackEntry.arguments?.getInt("id") ?: -1
                            PlaylistDetailScreen(
                                playlistId = playlistId,
                                viewModel = mediaViewModel,
                                onBack = { navController.popBackStack() },
                                onAudioClick = { path -> navController.navigate(Screen.AudioPlayer.createRoute(path, restart = true)) }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(playerViewModel = audioPlayerViewModel)
                        }
                        composable(Screen.Player.route) { backStackEntry ->
                            val path = backStackEntry.arguments?.getString("path") ?: ""
                            PlayerScreen(videoPath = path, mediaViewModel = mediaViewModel, onBack = { navController.popBackStack() })
                        }
                        composable(
                            Screen.AudioPlayer.route,
                            arguments = listOf(
                                navArgument("path") { type = NavType.StringType },
                                navArgument("restart") { type = NavType.BoolType; defaultValue = false }
                            )
                        ) { backStackEntry ->
                            val path = backStackEntry.arguments?.getString("path") ?: ""
                            val restart = backStackEntry.arguments?.getBoolean("restart") ?: false
                            AudioPlayerScreen(
                                audioPath = path, 
                                mediaViewModel = mediaViewModel,
                                playerViewModel = audioPlayerViewModel, 
                                restart = restart,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamXBottomNavigation(navController: NavHostController) {
    val items = listOf(
        Screen.Videos,
        Screen.Audio,
        Screen.Playlists,
        Screen.Settings
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom nav when in player screens
    if (currentRoute?.startsWith("player") == true || currentRoute?.startsWith("audio_player") == true) {
        return
    }

    Surface(
        color = Color(0xFF0D0F10),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    val activeColor = Color(0xFFE8E8E6)
                    val inactiveColor = Color(0xFF92999D)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0xFF242829) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) activeColor else inactiveColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = screen.title, 
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) activeColor else inactiveColor
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
