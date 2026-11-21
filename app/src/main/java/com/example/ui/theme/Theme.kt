package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AmoledRedColorScheme = darkColorScheme(
    primary = LiveRed,
    onPrimary = HighlightWhite,
    primaryContainer = DeepRed,
    onPrimaryContainer = HighlightWhite,
    secondary = Crimson,
    onSecondary = HighlightWhite,
    tertiary = HighlightWhite,
    onTertiary = AmoledBlack,
    background = AmoledBlack,
    onBackground = HighlightWhite,
    surface = ObsidianGlassSurface,
    onSurface = HighlightWhite,
    surfaceVariant = ObsidianGlassElevated,
    onSurfaceVariant = SecondaryText,
    outline = GlassHairlineRed,
    outlineVariant = GlassHairlineWhite
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AmoledRedColorScheme,
        typography = Typography,
        content = content
    )
}
