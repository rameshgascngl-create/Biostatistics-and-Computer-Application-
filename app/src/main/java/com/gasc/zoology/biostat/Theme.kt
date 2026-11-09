package com.gasc.zoology.biostat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Accessible educational palette: midnight blue, ocean teal and warm activity accents. */
private val Scheme = lightColorScheme(
    primary = Color(0xFF1F56A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEAFE),
    onPrimaryContainer = Color(0xFF143B78),
    secondary = Color(0xFF087F83),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9F3EF),
    onSecondaryContainer = Color(0xFF084C4C),
    tertiary = Color(0xFFA35E24),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE9D4),
    onTertiaryContainer = Color(0xFF6C360C),
    surface = Color.White,
    onSurface = Color(0xFF172C46),
    surfaceVariant = Color(0xFFEAF0F9),
    onSurfaceVariant = Color(0xFF53677F),
    background = Color(0xFFF3F7FE),
    onBackground = Color(0xFF172C46),
    outline = Color(0xFF9FACC0)
)

@Composable
fun BiostatTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, content = content)
