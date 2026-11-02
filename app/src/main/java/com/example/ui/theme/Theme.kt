package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = StreamXAccentOrange,
    onPrimary = Color.White,
    secondary = StreamXTextSecondary,
    background = StreamXDark,
    surface = StreamXSurface,
    surfaceVariant = StreamXSecondarySurface,
    onBackground = StreamXTextPrimary,
    onSurface = StreamXTextPrimary,
    onSurfaceVariant = StreamXTextSecondary,
    outline = StreamXDivider,
    outlineVariant = StreamXDivider
)

@Composable
fun MyApplicationTheme(
    primaryColor: Color = StreamXAccentOrange,
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme.copy(primary = primaryColor)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
