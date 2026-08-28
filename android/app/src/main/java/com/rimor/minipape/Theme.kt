package com.rimor.minipape

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF244FC2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE4FF),
    onPrimaryContainer = Color(0xFF08266E),
    secondary = Color(0xFF006783),
    secondaryContainer = Color(0xFFB9EAFF),
    tertiary = Color(0xFF9A315F),
    tertiaryContainer = Color(0xFFFFD8E7),
    background = Color(0xFFF7F8FF),
    surface = Color(0xFFF7F8FF),
    surfaceContainer = Color(0xFFECEEF8),
    surfaceContainerHigh = Color(0xFFE3E6F2),
    surfaceContainerHighest = Color(0xFFDADDEA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB5C7FF),
    onPrimary = Color(0xFF09245F),
    primaryContainer = Color(0xFF4169E1),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF6DE1FF),
    onSecondary = Color(0xFF003642),
    secondaryContainer = Color(0xFF07566A),
    onSecondaryContainer = Color(0xFFD0F5FF),
    tertiary = Color(0xFFFFA9CD),
    onTertiary = Color(0xFF5C1136),
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
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
