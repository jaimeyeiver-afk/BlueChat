package com.zeni.ia.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Violet = Color(0xFF7C3AED)
val VioletLight = Color(0xFFB57CFF)
val VioletDeep = Color(0xFF3B0A8C)
val Ink = Color(0xFF000000)
val Panel = Color(0xFF0E0A18)
val Bubble = Color(0xFF1A1230)

private val scheme = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    secondary = VioletLight,
    background = Ink,
    onBackground = Color(0xFFF2EEFF),
    surface = Panel,
    onSurface = Color(0xFFF2EEFF),
    surfaceVariant = Bubble,
    onSurfaceVariant = Color(0xFFCFC3EE),
    error = Color(0xFFFF6B8A)
)

@Composable
fun ZeniTheme(content: @Composable () -> Unit) {
    // Zeni IA siempre usa su diseño oscuro morado.
    MaterialTheme(colorScheme = scheme, content = content)
}
