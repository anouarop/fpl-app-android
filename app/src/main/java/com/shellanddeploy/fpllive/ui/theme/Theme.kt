package com.shellanddeploy.fpllive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Indigo500,
    onPrimary = Color.White,
    primaryContainer = Indigo400,
    onPrimaryContainer = Slate950,
    secondary = Emerald400,
    onSecondary = Slate950,
    background = Slate900,
    onBackground = Slate200,
    surface = Slate800,
    onSurface = Slate200,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,
    outline = Slate600,
    error = DifficultyRed,
    onError = Color.White,
)

private val LightColors = lightColorScheme(
    primary = Indigo500,
    onPrimary = Color.White,
    primaryContainer = Indigo200,
    onPrimaryContainer = Slate950,
    secondary = Emerald500,
    onSecondary = Color.White,
    background = Slate100,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate200,
    onSurfaceVariant = Slate700,
    outline = Slate400,
    error = DifficultyRed,
    onError = Color.White,
)

@Composable
fun FplLiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content,
    )
}
