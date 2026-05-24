package com.example.lingoFlix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WarmColorScheme = lightColorScheme(
    primary = WarmPrimary,
    secondary = WarmSecondary,
    tertiary = WarmTertiary,
    background = WarmBg,
    surface = WarmSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = WarmOnBg,
    onSurface = WarmOnBg
)

@Composable
fun LingoFlixTheme(
    darkTheme: Boolean = false, // We'll stick to warm colors for now
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WarmColorScheme,
        typography = Typography,
        content = content
    )
}
