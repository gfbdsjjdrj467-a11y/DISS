package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StrikeDarkColorScheme = darkColorScheme(
    primary = StrikeAmber,
    onPrimary = ObsidianBg,
    primaryContainer = GunmetalElevated,
    onPrimaryContainer = StrikeAmberBright,
    secondary = TacticalCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = GunmetalCard,
    onSecondaryContainer = TacticalCyan,
    tertiary = AlphaBlue,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SteelSurface,
    onSurface = TextPrimary,
    surfaceVariant = GunmetalCard,
    onSurfaceVariant = TextSecondary,
    outline = TacticalBorder,
    error = AlertCrimson,
    onError = TextPrimary
)

@Composable
fun StrikeArenaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StrikeDarkColorScheme,
        typography = Typography,
        content = content
    )
}
