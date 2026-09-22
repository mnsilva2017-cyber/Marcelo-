package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WhatsAppGreenLight,
    onPrimary = Color.Black,
    primaryContainer = WhatsAppDark,
    onPrimaryContainer = Color.White,
    secondary = ProPurple,
    onSecondary = Color.White,
    secondaryContainer = ProPurpleDark,
    onSecondaryContainer = Color.White,
    tertiary = ProAccentPink,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = WhatsAppGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F8E8),
    onPrimaryContainer = WhatsAppDark,
    secondary = ProPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEADBFF),
    onSecondaryContainer = ProPurpleDark,
    tertiary = ProAccentPink,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark for sticker neon/vibrant preview
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

