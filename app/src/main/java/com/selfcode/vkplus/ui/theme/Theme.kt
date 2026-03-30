package com.selfcode.vkplus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CyberBlue = Color(0xFF00B4FF)
val CyberAccent = Color(0xFF00EEFF)
val Background = Color(0xFF0A0C10)
val Surface = Color(0xFF111318)
val SurfaceVariant = Color(0xFF1A1D24)
val OnSurface = Color(0xFFE8ECF0)
val OnSurfaceMuted = Color(0xFF8899AA)
val Divider = Color(0xFF1E2530)
val ErrorRed = Color(0xFFFF4466)

private val DarkScheme = darkColorScheme(
    primary = CyberBlue,
    onPrimary = Color(0xFF001F33),
    primaryContainer = Color(0xFF003355),
    onPrimaryContainer = CyberAccent,
    secondary = CyberAccent,
    onSecondary = Color(0xFF001A1F),
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceMuted,
    outline = Divider,
    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun VKPlusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content
    )
}
