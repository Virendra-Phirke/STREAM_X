package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = StreamXPrimary,
    onPrimary = StreamXOnPrimary,
    secondary = StreamXSecondary,
    background = StreamXDark,
    surface = StreamXSurface,
    surfaceVariant = StreamXSurfaceVariant,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    onBackground = StreamXTextPrimary,
    onSurface = StreamXTextPrimary,
    onSurfaceVariant = StreamXTextSecondary,
    outline = StreamXBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun MyApplicationTheme(
    primaryColor: androidx.compose.ui.graphics.Color = StreamXPrimary,
    darkTheme: Boolean = true, // Force Dark theme
    dynamicColor: Boolean = false, // Disable dynamic colors
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = primaryColor,
        onPrimary = StreamXOnPrimary,
        secondary = StreamXSecondary,
        background = StreamXDark,
        surface = StreamXSurface,
        surfaceVariant = StreamXSurfaceVariant,
        onSecondary = androidx.compose.ui.graphics.Color.Black,
        onBackground = StreamXTextPrimary,
        onSurface = StreamXTextPrimary,
        onSurfaceVariant = StreamXTextSecondary,
        outline = StreamXBorder
    )

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
