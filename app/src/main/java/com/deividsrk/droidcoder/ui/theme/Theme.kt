package com.deividsrk.droidcoder.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DraculaPink,
    onPrimary = Color.Black,
    primaryContainer = DraculaCurrentLine,
    onPrimaryContainer = DraculaForeground,
    secondary = DraculaCyan,
    onSecondary = Color.Black,
    secondaryContainer = DraculaSelection,
    onSecondaryContainer = DraculaCyan,
    tertiary = DraculaGreen,
    onTertiary = Color.Black,
    tertiaryContainer = DraculaSelection,
    onTertiaryContainer = DraculaGreen,
    background = DraculaBackground,
    onBackground = DraculaForeground,
    surface = DraculaCurrentLine,
    onSurface = DraculaForeground,
    surfaceVariant = DraculaSelection,
    onSurfaceVariant = DraculaComment,
    outline = DraculaBorder,
    outlineVariant = DraculaSelection,
    error = DraculaRed,
    onError = Color.White
)

// Sharp, geometric, modern square corners for cards, text fields, buttons and dialogs
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp)
)

@Composable
fun DroidCoder2Theme(
    darkTheme: Boolean = true, // Force dark coding-focused theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
