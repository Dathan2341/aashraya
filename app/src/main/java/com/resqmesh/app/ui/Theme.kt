package com.resqmesh.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    secondary = Color(0xFF7DD3FC),
    tertiary = Color(0xFFC4B5FD),
    background = Color(0xFF020817),
    surface = Color(0xFF0F172A),
    onPrimary = Color(0xFF022C22),
    onSecondary = Color(0xFF082F49),
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

@Composable
fun ResQMeshTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
