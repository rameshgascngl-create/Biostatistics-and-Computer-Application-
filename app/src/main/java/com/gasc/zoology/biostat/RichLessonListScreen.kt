package com.gasc.zoology.biostat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Native textbook navigator. Unit rows expand inline to actual lessons (not just syllabus keywords).
 * Every visible lesson is an independent LazyColumn item, preserving test and accessibility navigation.
 * The initial expanded state matches the legacy textbook; students may collapse any unit.
 */
@Composable
fun RichLessonListScreen(
    content: CourseContent, store: ProgressStore, onOpen: (String) -> Unit
) {
    val ta = store.tamil
    var query by rememberSaveable { mutableStateOf("") }
    var selectedUnit by rememberSaveable { mutableIntStateOf(0) }
    var onlyBookmarks by rememberSaveable { mutableStateOf(false) }
    var collapsed by rememberSaveable { mutableStateOf("") }

    val filtered = content.lessons.filter { lesson ->
        val tamilTitle = content.tamilLessons[lesson.id]?.title.orEmpty()
        val isMatch = query.isBlank() || listOf(lesson.title, tamilTitle, lesson.intro)
            .any { it.contains(query.trim(), ignoreCase = true) }
        (selectedUnit == 0 || lesson.unit == selectedUnit) &&
            (!onlyBookmarks || lesson.id in store.bookmarks) && isMatch
    }
    val collapsedUnits = collapsed.split(",").filter { it.isNotBlank() }.toSet()

    LazyColumn(
        modifier = Modifier.fillMaxWidth().testTag("textbook-scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item(key = "textbook-header") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B497B)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(Color(0xFF12365F), Color(0xFF265E9D), Color(0xFF078482)))
                ).padding(20.dp)) {
                    Text(tr("BIOSTATISTICS • VOLUME I", "உயிரிப் புள்ளியியல் • தொகுதி 1"),
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                        color = Color(0xFFC9F5ED))
                    Spacer(Modifier.height(7.dp))
                    Text(tr("Table of contents", "பொருளடக்கம்"),
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(tr("${content.lessons.size} chapters • 6 units • bilingual textbook",
                        "${content.lessons.size} பாடங்கள் • 6 அலகுகள் • இருமொழிப் பாடநூல்"),
                        style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE3F0FE))
                    Spacer(Modifier.height(7.dp))
                    Text(tr("Open any unit, choose a chapter and turn the pages.",
                        "அலகைத் திறந்து, பாடத்தைத் தேர்ந்தெடுத்து, பக்கங்களைப் புரட்டிப் படிக்கலாம்."),
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFFE0E9F6))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        singleLine = true,
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.fillMaxWidth().testTag("lesson-search"),
                        label = { Text(tr("Search chapters", "பாடங்களைத் தேடு")) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color(0xFF1A3152),
                            unfocusedTextColor = Color(0xFF1A3152),
                            focusedLabelColor = Color(0xFF315C89),
                            unfocusedLabelColor = Color(0xFF315C89),
                            focusedBorderColor = Color(0xFFCCE5F6),
                            unfocusedBorderColor = Color(0xFFB9D8E4)
                        )
                    )
                }
            }
        }
        item(key = "filters") {
            LazyRow(modifier = Modifier.fillMaxWidth().testTag("textbook-filter-scroll"),
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items((0..6).toList()) { u ->
                    FilterChip(
                        selected = selectedUnit == u,
                        onClick = {
                            selectedUnit = u
                            if (u != 0) collapsed = collapsedUnits.minus(u.toString()).joinToString(",")
                        },
                        modifier = Modifier.testTag("textbook-filter-" + u),
                        label = { Text(if (u == 0) tr("All units", "அனைத்து அலகுகள்")
                            else tr("Unit " + u, "அலகு " + u)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD3E5FF),
                            selectedLabelColor = Color(0xFF174786)
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = onlyBookmarks,
                        onClick = { onlyBookmarks = !onlyBookmarks },
                        label = { Text(tr("★ Bookmarks", "★ புத்தகக்குறிகள்")) }
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            item(key = "empty") {
                SectionCard(tr("No matching lessons", "பொருந்தும் பாடங்கள் இல்லை")) {
                    Para(tr("Try another keyword or clear the filters.",
                        "வேறு சொல்லைப் பயன்படுத்தவும் அல்லது வடிகட்டியை நீக்கவும்."))
                }
            }
        }

        for (u in 1..6) {
            val lessons = filtered.filter { it.unit == u }
            if (lessons.isEmpty()) continue
            val unit = content.syllabus.getOrNull(u - 1)
            val tamilUnit = content.extra.syllabus.getOrNull(u - 1)
            val title = if (ta) tamilUnit?.title.orEmpty().ifBlank { unit?.title.orEmpty() }
                else unit?.title.orEmpty()
            val all = content.lessons.filter { it.unit == u }
            val completed = all.count { it.id in store.completed }
            val expanded = query.isNotBlank() || u.toString() !in collapsedUnits

            item(key = "unit-heading-" + u) {
                val accent = when (u) {
                    1 -> Color(0xFF1769BB); 2 -> Color(0xFF008A88)
                    3 -> Color(0xFF8757C0); 4 -> Color(0xFFB66C24)
                    5 -> Color(0xFF4670AE); else -> Color(0xFF088A6E)
                }
                Card(
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(containerColor = when (u) {
                        1 -> Color(0xFFEAF2FF); 2 -> Color(0xFFE5F6F1)
                        3 -> Color(0xFFF3ECFB); 4 -> Color(0xFFFFF0DF)
                        5 -> Color(0xFFEAF3FC); else -> Color(0xFFE7F5EB)
                    }),
                    border = BorderStroke(1.dp, accent.copy(alpha = .25f)),
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            collapsed = if (u.toString() in collapsedUnits)
                                collapsedUnits.minus(u.toString()).joinToString(",")
                            else collapsedUnits.plus(u.toString()).joinToString(",")
                        }
                        .testTag("textbook-unit-" + u)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(width = 5.dp, height = 55.dp)
                            .clip(RoundedCornerShape(4.dp)).background(accent))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("UNIT " + u, "அலகு " + u),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold, color = accent)
                            Text(title, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(tr(completed.toString() + "/" + all.size + " completed",
                                all.size.toString() + " பாடங்களில் " + completed + " முடிந்தன"),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = tr(if (expanded) "Collapse" else "Expand",
                                if (expanded) "மூடு" else "விரி"), tint = accent)
                    }
                }
            }
            if (expanded) {
                items(lessons, key = { "lesson-" + it.id }) { lesson ->
                    RichLessonRow(
                        lesson = lesson, content = content, store = store,
                        onOpen = { onOpen(lesson.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RichLessonRow(
    lesson: Lesson, content: CourseContent, store: ProgressStore, onOpen: () -> Unit
) {
    val ta = store.tamil
    val done = lesson.id in store.completed
    val marked = lesson.id in store.bookmarks
    val title = if (ta) content.tamilLessons[lesson.id]?.title.orEmpty().ifBlank { lesson.title }
        else lesson.title

    Card(
        modifier = Modifier.fillMaxWidth().padding(start = 9.dp)
            .clickable(onClick = onOpen).testTag("lesson-" + lesson.id),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (done) Color(0xFFBCE8D7) else Color(0xFFE1E8F4)),
        colors = CardDefaults.cardColors(containerColor = if (done) Color(0xFFEAF8F0) else Color(0xFFFFFCF6))
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, end = 9.dp, bottom = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (done) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = tr("Completed", "முடிந்தது"),
                        tint = Color(0xFF168764), modifier = Modifier.size(18.dp))
                } else {
                    Box(Modifier.size(21.dp).clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFFDCEBFF)), contentAlignment = Alignment.Center) {
                        Text(lesson.sequence.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF174B9A), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    val chapterNumber = content.lessons.indexOfFirst { it.id == lesson.id } + 1
                    Text(tr("CHAPTER $chapterNumber", "பாடம் $chapterNumber"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold, color = Color(0xFF426D97))
                    Text(title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(
                    onClick = { store.toggleBookmark(lesson.id) },
                    modifier = Modifier.size(42.dp).testTag("list-bookmark-" + lesson.id)
                ) {
                    Icon(if (marked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = tr("Toggle bookmark", "புத்தகக்குறியை மாற்று"),
                        tint = Color(0xFF2357A1), modifier = Modifier.size(21.dp))
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 29.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr(lesson.study.read.toString() + " min read",
                    lesson.study.read.toString() + " நிமிட வாசிப்பு"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { store.toggleLesson(lesson.id) },
                    modifier = Modifier.testTag("list-complete-" + lesson.id)) {
                    Text(if (done) tr("✓ Completed", "✓ முடிந்தது")
                        else tr("Mark complete", "முடிந்ததாகக் குறி"),
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
