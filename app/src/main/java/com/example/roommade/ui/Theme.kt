package com.example.roommade.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 밝고 깔끔한 톤으로 배경/서피스 컬러를 재정의합니다.
private val LightColors = lightColorScheme(
    primary = Color(0xFF5E5CE6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E3FF),
    onPrimaryContainer = Color(0xFF1A1570),
    secondary = Color(0xFF4E5D6A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1E7EF),
    onSecondaryContainer = Color(0xFF111C25),
    background = Color(0xFFF6F5F2),
    onBackground = Color(0xFF1F1F1F),
    surface = Color.White,
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFEAE9EE),
    onSurfaceVariant = Color(0xFF4A4A4A),
    surfaceTint = Color.White,
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    surfaceDim = Color(0xFFF6F5F2),
    outline = Color(0xFFC2C4CC),
    error = Color(0xFFBA1B1B),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun RoomMadeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
