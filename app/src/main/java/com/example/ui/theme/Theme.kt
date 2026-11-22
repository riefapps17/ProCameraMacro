package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ProCameraColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF332600),
    onPrimaryContainer = GoldAccentLight,
    secondary = RawBadgeColor,
    onSecondary = Color.White,
    tertiary = FocusPeakingGreen,
    background = CameraDarkBackground,
    onBackground = TextPrimary,
    surface = CameraSurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CameraSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CameraControlTrack
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Always default to pro camera dark theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ProCameraColorScheme,
        typography = Typography,
        content = content
    )
}
