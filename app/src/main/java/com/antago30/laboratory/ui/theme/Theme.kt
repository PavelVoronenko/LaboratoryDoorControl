package com.antago30.laboratory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    background = DarkBg,
    surface = CardBg,
    onBackground = Text,
    onSurface = Text,
    error = Outdoor

)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    background = DarkBg,
    surface = CardBg,
    onBackground = Text,
    onSurface = Text,
    error = Outdoor
)

@Composable
fun LaboratoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}