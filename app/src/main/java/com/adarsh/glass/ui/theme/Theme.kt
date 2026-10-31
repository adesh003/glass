package com.adarsh.glass.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = GlassAccent,
    background = GlassDark,
    surface = GlassSurface,
    onBackground = GlassOnDark,
    onSurface = GlassOnSurface,
)

private val LightColors = lightColorScheme(
    primary = GlassAccent,
)

@Composable
fun GlassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = GlassTypography,
        content = content,
    )
}
