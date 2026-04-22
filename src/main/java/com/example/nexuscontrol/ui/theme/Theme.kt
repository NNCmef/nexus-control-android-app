package com.example.nexuscontrol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppGreen = Color(0xFF00E676)
val AppGreenDark = Color(0xFF00C853)
val AppBackground = Color(0xFF121212)
val AppSurface = Color(0xFF1E1E1E)
val AppInput = Color(0xFF2A2A2A)
val AppBorder = Color(0xFF444444)
val AppTextPrimary = Color(0xFFE0E0E0)

private val DarkColorScheme = darkColorScheme(
    primary = AppGreen,
    onPrimary = Color(0xFF121212),
    secondary = AppGreenDark,
    background = AppBackground,
    surface = AppSurface,
    onBackground = AppTextPrimary,
    onSurface = AppTextPrimary
)

@Composable
fun NexusControlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
