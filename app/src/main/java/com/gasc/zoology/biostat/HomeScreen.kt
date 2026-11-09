package com.gasc.zoology.biostat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val UnitTones = listOf(
    Color(0xFF1769BB), Color(0xFF008A88), Color(0xFF8B59C8),
    Color(0xFFC46E24), Color(0xFF4874B4), Color(0xFF008C6E)
)

@Composable
fun HomeScreen(
    content: CourseContent,
    store: ProgressStore,
    onOpenLesson: (String) -> Unit,
    onOpenLessons: () -> Unit,
    onOpenQuiz: () -> Unit = {},
    onOpenLab: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {}
) {
    val ta = store.tamil
    val total = content.lessons.size
    val done = content.lessons.count { it.id in store.completed }
    val next = content.lessons.firstOrNull { it.id !in store.completed }
    val currentUnit = next?.unit ?: 1
    val progress = if (total == 0) 0f else done.toFloat() / total

    LazyColumn(
        modifier = Modifier.fillMaxWidth().testTag("home-scroll"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF122C5A), Color(0xFF2557A5), Color(0xFF087B86))))
                    .padding(20.dp)
            ) {
                Text(
                    if (ta) "கற்றலும் பயிற்சியும்" else "LEARN • PRACTISE • DISCOVER",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFC7F5EE), fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (ta) "உயிரிப் புள்ளியியல் மற்றும் கணினிப் பயன்பாடுகள்"
                    else "Biostatistics & Computer Applications",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (ta) "ஆறு அலகுகள் • செயல்முறைப் பயிற்சிகள் • கற்றல் முன்னேற்றம்"
                    else "Six units, hands-on statistics and a guided learning path.",
                    style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE2F0FF)
                )
                Spacer(Modifier.height(18.dp))
                if (next != null) {
                    Button(
                        onClick = { onOpenLesson(next.id) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("continue"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF153E79)),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Text(if (done == 0) tr("Start learning", "கற்கத் தொடங்கு")
                             else tr("Continue learning", "கற்றலைத் தொடர்"), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = null)
                    }
                } else {
                    Text(tr("All lessons completed. Well done!", "அனைத்துப் பாடங்களும் முடிக்கப்பட்டன!"),
                        color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    HeroStat(total.toString(), tr("Lessons", "பாடங்கள்"), Modifier.weight(1f))
                    HeroStat(content.questions.size.toString(), tr("MCQs", "வினாக்கள்"), Modifier.weight(1f))
                    HeroStat(content.flashcards.size.toString(), tr("Cards", "அட்டைகள்"), Modifier.weight(1f))
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(tr("Course progress", "பாட முன்னேற்றம்"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress }, modifier = Modifier.fillMaxWidth().height(9.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        color = Color(0xFF12A58F), trackColor = Color(0xFFE5EAF5)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(tr(done.toString() + " of " + total + " lessons completed",
                        total.toString() + " பாடங்களில் " + done + " முடிந்தன"),
                        style = MaterialTheme.typography.bodyMedium)
                    Text(tr(store.answeredRight.size.toString() + " of " + content.questions.size + " quiz questions answered correctly",
                        content.questions.size.toString() + " வினாக்களில் " + store.answeredRight.size + " சரியாக விடையளிக்கப்பட்டன"),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(tr("Syllabus", "பாடத்திட்டம்"),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(tr("Tap a unit to explore lessons here", "பாடங்களைக் காண அலகைத் தொடவும்"),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(tr("6 units", "6 அலகுகள்"),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        items(content.syllabus.indices.toList(), key = { "unit-" + it }) { i ->
            val unit = content.syllabus[i]
            val unitLessons = content.lessons.filter { it.unit == i + 1 }
            val unitDone = unitLessons.count { it.id in store.completed }
            val localised = content.extra.syllabus.getOrNull(i)
            val title = if (ta) localised?.title.orEmpty().ifBlank { unit.title } else unit.title
            val topics = if (ta) localised?.topics.orEmpty().ifEmpty { unit.topics } else unit.topics
            val accent = UnitTones[i % UnitTones.size]
            var expanded by rememberSaveable(unit.unit) { mutableStateOf(i + 1 == currentUnit) }
            Card(
                modifier = Modifier.fillMaxWidth().testTag("home-unit-" + (i + 1)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE0E8F4)),
                shape = RoundedCornerShape(19.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .testTag("home-unit-toggle-" + (i + 1))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.width(5.dp).height(50.dp).clip(RoundedCornerShape(4.dp)).background(accent))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("UNIT " + unit.unit, "அலகு " + unit.unit),
                                style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
                            Text(title, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(5.dp))
                            Text(tr(unitDone.toString() + "/" + unitLessons.size + " lessons completed",
                                unitLessons.size.toString() + " பாடங்களில் " + unitDone + " முடிந்தன"),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = tr(if (expanded) "Collapse unit" else "Expand unit",
                                if (expanded) "அலகை மூடு" else "அலகை விரி"), tint = accent)
                    }
                    LinearProgressIndicator(
                        progress = { if (unitLessons.isEmpty()) 0f else unitDone.toFloat() / unitLessons.size },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = accent, trackColor = Color(0xFFE8EEF8)
                    )
                    if (expanded) {
                        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp, top = 10.dp)) {
                            Text(tr("CHAPTERS", "பாடங்கள்"), style = MaterialTheme.typography.labelMedium,
                                color = accent, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                            unitLessons.forEach { lesson ->
                                val lessonTitle = if (ta) content.tamilLessons[lesson.id]?.title.orEmpty()
                                    .ifBlank { lesson.title } else lesson.title
                                val finished = lesson.id in store.completed
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF2F6FC))
                                        .clickable { onOpenLesson(lesson.id) }
                                        .testTag("home-lesson-" + lesson.id)
                                        .padding(start = 11.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (finished) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF158B66),
                                        modifier = Modifier.size(19.dp))
                                    else Box(Modifier.size(19.dp).clip(RoundedCornerShape(6.dp)).background(accent.copy(alpha = .17f)),
                                        contentAlignment = Alignment.Center) {
                                        Text((lesson.sequence).toString(), style = MaterialTheme.typography.labelSmall,
                                            color = accent, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(9.dp))
                                    Text(lessonTitle, modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    IconButton(
                                        onClick = { store.toggleBookmark(lesson.id) },
                                        modifier = Modifier.size(40.dp).testTag("home-bookmark-" + lesson.id)
                                    ) {
                                        Icon(if (lesson.id in store.bookmarks) Icons.Filled.Bookmark
                                             else Icons.Filled.BookmarkBorder,
                                            contentDescription = tr("Toggle bookmark", "புத்தகக்குறியை மாற்று"),
                                            tint = accent, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            if (unitLessons.isEmpty()) {
                                topics.forEach { Text("• " + it, style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(4.dp)) }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StudyAction(
                    tr("Browse chapters", "பாடங்களைப் பார்"), tr("Read & revise", "படித்து மீள்பார்வை"),
                    Color(0xFFE5EEFF), Color(0xFF204E9A), onOpenLessons, Modifier.weight(1f)
                )
                StudyAction(
                    tr("Practice MCQs", "வினா பயிற்சி"), tr("Test your learning", "பயிற்சி செய்து பார்"),
                    Color(0xFFE2F8EE), Color(0xFF146F52), onOpenQuiz, Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StudyAction(
                    tr("Analysis Lab", "பகுப்பாய்வகம்"), tr("17 statistical tools", "17 புள்ளியியல் கருவிகள்"),
                    Color(0xFFFFEEDD), Color(0xFF9A5423), onOpenLab, Modifier.weight(1f)
                )
                StudyAction(
                    tr("Your pathway", "கற்றல் பாதை"), tr("All six units", "ஆறு அலகுகள்"),
                    Color(0xFFF0E8FF), Color(0xFF7547AA), onOpenLessons, Modifier.weight(1f)
                )
            }
        }
        item {
            TextButton(onClick = onOpenPrivacy, modifier = Modifier.testTag("home-privacy")) {
                Text(tr("Privacy policy", "தனியுரிமைக் கொள்கை"))
            }
        }
    }
}

@Composable
private fun HeroStat(number: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = .13f)).padding(vertical = 9.dp, horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(number, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFFE1F2FF), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StudyAction(
    title: String, subtitle: String, background: Color, ink: Color,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Card(modifier = modifier.heightIn(min = 104.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = background)) {
        Column(Modifier.fillMaxSize().padding(13.dp), verticalArrangement = Arrangement.Center) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ink)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = .86f))
        }
    }
}
