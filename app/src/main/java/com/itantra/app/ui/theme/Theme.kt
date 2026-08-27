package com.itantra.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = AmberAlert,
    tertiary = SuccessGreen,
    background = TechDarkBackground,
    surface = TechCardSurface,
    onPrimary = TechDarkBackground,
    onSecondary = TechDarkBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun ITantraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
