package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.di.appContainer
import com.example.ui.navigation.Screen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.screens.AudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.MediaViewModel
import com.example.ui.viewmodels.MediaViewModelFactory

import com.example.ui.screens.AudioPlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.components.MiniPlayer
import com.example.ui.viewmodels.ThemeManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
            val primaryColor by androidx.compose.animation.animateColorAsState(
                targetValue = primaryColorFlow,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 700),
                label = "color_transition"
            )
            var showShortcutsHelp by remember { androidx.compose.runtime.mutableStateOf(false) }

            MyApplicationTheme(primaryColor = primaryColor) {
                val navController = rememberNavController()

                val container = (applicationContext as StreamXApplication).container
                val factory = MediaViewModelFactory(container.mediaRepository)
                val mediaViewModel: MediaViewModel = viewModel(factory = factory)
                val audioPlayerViewModel: com.example.ui.viewmodel.AudioPlayerViewModel = viewModel()

                val audios by mediaViewModel.audios.collectAsStateWithLifecycle()
                LaunchedEffect(audios) {
                    if (audios.isNotEmpty()) {
                        audioPlayerViewModel.tryRestoreState(applicationContext, audios)
                    }
                }
                
                val context = androidx.compose.ui.platform.LocalContext.current
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
                
                var permissionsGranted by remember { androidx.compose.runtime.mutableStateOf(false) }
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
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Text("Please grant media permissions to proceed.", color = Color.White)
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
                            StreamXBottomNavigation(navController = navController, primaryColor = primaryColor)
                        } 
                    },
                    containerColor = Color(0xFF0F1113),
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Audio.route,
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
                            com.example.ui.screens.PlaylistsScreen(viewModel = mediaViewModel) { playlistId ->
                                navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                            }
                        }
                        composable(
                            route = Screen.PlaylistDetail.route,
                            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.IntType })
                        ) { backStackEntry ->
                            val playlistId = backStackEntry.arguments?.getInt("id") ?: -1
                            com.example.ui.screens.PlaylistDetailScreen(
                                playlistId = playlistId,
                                viewModel = mediaViewModel,
                                onBack = { navController.popBackStack() },
                                onAudioClick = { path -> navController.navigate(Screen.AudioPlayer.createRoute(path, restart = true)) }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(playerViewModel = audioPlayerViewModel, onClose = { navController.popBackStack() })
                        }
                        composable(Screen.Player.route) { backStackEntry ->
                            val path = backStackEntry.arguments?.getString("path") ?: ""
                            PlayerScreen(videoPath = path, mediaViewModel = mediaViewModel, onBack = { navController.popBackStack() })
                        }
                        composable(
                            Screen.AudioPlayer.route,
                            arguments = listOf(
                                androidx.navigation.navArgument("path") { type = androidx.navigation.NavType.StringType },
                                androidx.navigation.navArgument("restart") { type = androidx.navigation.NavType.BoolType; defaultValue = false }
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
                
                if (showShortcutsHelp) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showShortcutsHelp = false },
                        title = { Text("Keyboard Shortcuts") },
                        text = {
                            Column {
                                Text("• Spacebar: Play / Pause")
                                Text("• Left Arrow: Skip Previous")
                                Text("• Right Arrow: Skip Next")
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { showShortcutsHelp = false }) {
                                Text("Close")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StreamXBottomNavigation(navController: NavHostController, primaryColor: Color) {
    val items = listOf(
        Screen.Videos,
        Screen.Audio,
        Screen.Playlists,
        Screen.Settings
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom nav when in player
    if (currentRoute?.startsWith("player") == true || currentRoute?.startsWith("audio_player") == true) {
        return
    }

    androidx.compose.material3.Surface(
        color = Color(0xFF0A0A0A),
        contentColor = Color(0xFFAAAAAA),
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Column {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
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
                        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                    .background(if (isSelected) primaryColor else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) Color.Black else Color.White.copy(alpha = 0.5f)
                                )
                            }
                            if (isSelected) {
                                Text(
                                    text = screen.title, 
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = primaryColor
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
