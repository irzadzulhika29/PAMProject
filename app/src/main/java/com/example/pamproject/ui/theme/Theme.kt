package com.example.pamproject.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = BgDark,
    primaryContainer = BgCard,
    onPrimaryContainer = TextPrimary,
    secondary = PrimaryLight,
    onSecondary = BgDark,
    secondaryContainer = BgCard,
    onSecondaryContainer = TextPrimary,
    tertiary = PrimaryDark,
    onTertiary = BgDark,
    background = Bg,
    onBackground = TextPrimary,
    surface = Bg,
    onSurface = TextPrimary,
    surfaceVariant = BgCard,
    onSurfaceVariant = TextSecondary,
    outline = Primary.copy(alpha = 0.35f)
)

private val LightColorScheme = DarkColorScheme.copy(
    background = Bg,
    surface = Bg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    inverseOnSurface = Color.Black
)

@Composable
fun PAMProjectTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}