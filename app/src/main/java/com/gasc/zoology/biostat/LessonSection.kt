package com.gasc.zoology.biostat

import androidx.compose.foundation.BorderStroke
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

/**
 * Reader accordion corresponding to legacy textbook <details> elements.
 * Headings remain visible at any font scale; opening reveals the complete section inline.
 */
@Composable
fun LessonSection(
    title: String,
    modifier: Modifier = Modifier,
    body: @Composable ColumnScope.() -> Unit
) {
    val initiallyOpen = title == "Learning objectives" || title == "மேலோட்டம்"
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyOpen) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E7F2)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
                    .testTag("lesson-section-" + title).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = tr(if (expanded) "Collapse section" else "Expand section",
                        if (expanded) "பகுதியை மூடு" else "பகுதியை விரி"),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (expanded) {
                HorizontalDivider(color = Color(0xFFE7ECF5))
                Column(Modifier.fillMaxWidth().padding(16.dp).testTag("section-body-" + title)) {
                    body()
                }
            }
        }
    }
}
