package com.gasc.zoology.biostat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Consistent colour coding by academic purpose; every tint has a dark readable heading. */
internal data class ReadingTone(val accent: Color, val surface: Color)

internal fun readingTone(title: String): ReadingTone {
    val name = title.lowercase()
    return when {
        listOf("learning objectives", "introduction", "background", "மேலோட்டம்", "அறிமுகம்", "கற்றல் நோக்கம்")
            .any { name.contains(it) } -> ReadingTone(Color(0xFF2459A7), Color(0xFFE7EFFD))
        listOf("definition", "வரையறை", "classification", "வகைப்பாடு")
            .any { name.contains(it) } -> ReadingTone(Color(0xFF087A75), Color(0xFFE1F4EF))
        listOf("formula", "சூத்திரம்", "கணக்கீடு")
            .any { name.contains(it) } -> ReadingTone(Color(0xFF7248AB), Color(0xFFF0E9FC))
        listOf("example", "எடுத்துக்காட்டு", "worked")
            .any { name.contains(it) } -> ReadingTone(Color(0xFFA5621E), Color(0xFFFFF0DA))
        listOf("application", "பயன்பாடு", "why it matters", "முக்கியத்துவம்")
            .any { name.contains(it) } -> ReadingTone(Color(0xFF087385), Color(0xFFE1F3F8))
        listOf("misconception", "mistake", "தவறு", "பிழை", "limitations", "precautions", "வரம்பு")
            .any { name.contains(it) } -> ReadingTone(Color(0xFFAC4C5F), Color(0xFFFFEAF0))
        listOf("summary", "சுருக்கம்", "முக்கியக் குறிப்புகள்", "memory aid")
            .any { name.contains(it) } -> ReadingTone(Color(0xFF1C7657), Color(0xFFE1F3E9))
        listOf("self-check", "சுயசோதனை", "exam", "தேர்வு")
            .any { name.contains(it) } -> ReadingTone(Color(0xFF7754A3), Color(0xFFF1EBFC))
        else -> ReadingTone(Color(0xFF385E8A), Color(0xFFEDF3FA))
    }
}

/**
 * Native inline textbook accordion: coloured by lesson type and fully accessible at large font sizes.
 * The existing test tags and saveable expansion state are intentionally retained.
 */
@Composable
fun LessonSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyOpenOverride: Boolean = false,
    body: @Composable ColumnScope.() -> Unit
) {
    val tone = readingTone(title)
    val initiallyOpen = initiallyOpenOverride || title == "Learning objectives" || title == "மேலோட்டம்"
    var expanded by rememberSaveable(title, initiallyOpenOverride) { mutableStateOf(initiallyOpen) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        border = BorderStroke(1.dp, tone.accent.copy(alpha = 0.25f)),
        colors = CardDefaults.cardColors(containerColor = tone.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
                    .testTag("lesson-section-" + title).padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.width(5.dp).height(36.dp)
                    .background(tone.accent, RoundedCornerShape(5.dp)))
                Text(title, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = tone.accent)
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = tr(if (expanded) "Collapse section" else "Expand section",
                        if (expanded) "பகுதியை மூடு" else "பகுதியை விரி"),
                    tint = tone.accent
                )
            }
            if (expanded) {
                HorizontalDivider(color = tone.accent.copy(alpha = .16f))
                Column(Modifier.fillMaxWidth().background(Color.White.copy(alpha = .84f))
                    .padding(horizontal = 17.dp, vertical = 12.dp)
                    .testTag("section-body-" + title)) {
                    body()
                }
            }
        }
    }
}
