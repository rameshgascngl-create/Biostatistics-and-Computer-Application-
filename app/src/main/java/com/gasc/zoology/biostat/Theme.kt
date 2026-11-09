package com.gasc.zoology.biostat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = lightColorScheme(
    primary = Color(0xFF0D5C63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5EEF0),
    onPrimaryContainer = Color(0xFF062E32),
    secondary = Color(0xFF00A896),
    tertiaryContainer = Color(0xFFFFF1D6),
    surface = Color(0xFFFFFFFF),
    background = Color(0xFFF6FAFA)
)

@Composable
fun BiostatTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, content = content)
