package com.warrior.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WarriorDarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentSoft,
    onPrimaryContainer = TextPrimary,
    secondary = SurfaceVariant,
    onSecondary = TextPrimary,
    tertiary = Positive,
    onTertiary = Color.Black,
    error = Negative,
    onError = Color.Black,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = Outline,
    outlineVariant = Outline,
)

/**
 * WARRIOR design system theme. v1 ships dark-first (single dark scheme),
 * matching the approved interactive preview (ui-preview.html).
 */
@Composable
fun WarriorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarriorDarkColors,
        typography = WarriorTypography,
        content = content,
    )
}
