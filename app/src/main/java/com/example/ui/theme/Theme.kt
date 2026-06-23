package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = ElectricGold,
    tertiary = CalmGreen,
    background = CosmicBackground,
    surface = CosmicSurface,
    error = CyberPink,
    onPrimary = CosmicBackground,
    onSecondary = CosmicBackground,
    onBackground = PureWhite,
    onSurface = PureWhite,
    onTertiary = CosmicBackground
)

@Composable
fun AutoControlTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
