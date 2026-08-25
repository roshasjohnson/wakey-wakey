package com.roshas.arrivalalert.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Always dark, never dynamic. Wallpaper-derived colours would pull the whole
 * palette away from the near-black-and-one-accent design.
 */
private val WakeyColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    secondary = Accent,
    onSecondary = OnAccent,
    background = Background,
    onBackground = Foreground,
    surface = Background,
    onSurface = Foreground,
    surfaceContainer = Background,
    surfaceContainerHigh = SheetBackground,
    surfaceContainerLow = SheetBackground,
    onSurfaceVariant = Muted,
    outline = Muted,
    outlineVariant = Divider,
    error = Destructive,
    onError = Background
)

@Composable
fun ArrivalAlertTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WakeyColorScheme,
        typography = Typography,
        content = content
    )
}
