package com.gasc.zoology.biostat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** True when the app is in Tamil mode. English and Tamil are separate modes: text is never shown in both. */
val LocalTamil = compositionLocalOf { false }

@Composable
fun tr(en: String, ta: String): String = if (LocalTamil.current) ta else en
