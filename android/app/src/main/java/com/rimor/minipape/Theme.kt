package com.rimor.minipape

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB5C7FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4169E1),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF6DE1FF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF07566A),
    onSecondaryContainer = Color(0xFFD0F5FF),
    tertiary = Color(0xFFFFA9CD),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF8A2855),
    onTertiaryContainer = Color(0xFFFFE8F0),
    background = Color(0xFF070A14),
    onBackground = Color(0xFFF4F5FF),
    surface = Color(0xFF070A14),
    onSurface = Color(0xFFF4F5FF),
    surfaceVariant = Color(0xFF242B42),
    onSurfaceVariant = Color(0xFFC5CBE0),
    surfaceContainer = Color(0xFF111728),
    surfaceContainerHigh = Color(0xFF192238),
    surfaceContainerHighest = Color(0xFF222D48),
    outline = Color(0xFF8792B3),
)

@Composable
fun MiniPapeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
