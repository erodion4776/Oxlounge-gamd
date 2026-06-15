package com.oxlounge

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define the custom Lounge Neon-meets-Gold Palette
val NeonPurple = Color(0xFFA855F7)
val GoldenGold = Color(0xFFF59E0B)
val LightGold = Color(0xFFFBBF24)
val DarkLoungeBg = Color(0xFF0F0C1B)
val SurfaceLounge = Color(0xFF1E1B33)
val BorderLounge = Color(0xFF2E2A4E)
val TextLight = Color(0xFFF3F4F6)
val TextGray = Color(0xFF9CA3AF)

private val LoungeDarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    secondary = GoldenGold,
    tertiary = LightGold,
    background = DarkLoungeBg,
    surface = SurfaceLounge,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextLight,
    onSurface = TextLight,
    outline = BorderLounge
)

private val LoungeLightColorScheme = lightColorScheme(
    primary = NeonPurple,
    secondary = GoldenGold,
    tertiary = LightGold,
    background = Color(0xFFF3F4F6),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF1F2937),
    outline = Color(0xFFE5E7EB)
)

@Composable
fun LoungeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LoungeDarkColorScheme else LoungeDarkColorScheme // Keep it dark by default to respect lounge aesthetic

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
