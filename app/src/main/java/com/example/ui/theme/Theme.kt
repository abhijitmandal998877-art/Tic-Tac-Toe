package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GameColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF041E26),
    primaryContainer = Color(0xFF003842),
    onPrimaryContainer = NeonCyan,

    secondary = NeonPink,
    onSecondary = Color(0xFF2E0013),
    secondaryContainer = Color(0xFF520624),
    onSecondaryContainer = NeonPink,

    tertiary = NeonPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3B1E6D),
    onTertiaryContainer = NeonPurple,

    background = DarkBackground,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    outline = CardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force modern dark gaming theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GameColorScheme,
        typography = Typography,
        content = content
    )
}
