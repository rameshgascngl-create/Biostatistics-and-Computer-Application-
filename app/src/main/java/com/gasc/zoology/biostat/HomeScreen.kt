package com.gasc.zoology.biostat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(content: CourseContent, store: ProgressStore, onOpenLesson: (String) -> Unit, onOpenLessons: () -> Unit) {
    val ta = store.tamil
    val total = content.lessons.size
    val done = store.completed.count { id -> content.lessons.any { it.id == id } }
    val next = content.lessons.firstOrNull { it.id !in store.completed }
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard(tr("Course progress", "பாட முன்னேற்றம்")) {
                LinearProgressIndicator(progress = { if (total == 0) 0f else done / total.toFloat() }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Para(tr("$done of $total lessons completed", "$total பாடங்களில் $done முடிந்தன"))
                Para(tr("${store.answeredRight.size} of ${content.questions.size} quiz questions answered correctly", "${content.questions.size} வினாக்களில் ${store.answeredRight.size} சரியாக விடையளிக்கப்பட்டன"))
                Spacer(Modifier.height(4.dp))
                if (next != null) {
                    Button(onClick = { onOpenLesson(next.id) }, modifier = Modifier.testTag("continue")) {
                        Text(if (done == 0) tr("Start learning", "கற்கத் தொடங்கு") else tr("Continue learning", "கற்றலைத் தொடர்"))
                    }
                } else Para(tr("All lessons completed — revise with the quiz and flashcards.", "அனைத்துப் பாடங்களும் முடிந்தன — வினாடி வினா, அட்டைகள் மூலம் மீள்பார்வை செய்யுங்கள்."))
            }
        }
        item { Text(tr("Syllabus", "பாடத்திட்டம்"), style = MaterialTheme.typography.titleLarge) }
        items(content.syllabus.indices.toList()) { i ->
            val u = content.syllabus[i]
            val tu = content.extra.syllabus.getOrNull(i)
            val title = if (ta) tu?.title.orEmpty() else u.title
            val topics = if (ta) tu?.topics ?: emptyList() else u.topics
            var open by rememberSaveable(u.unit) { mutableStateOf(false) }
            SectionCard(tr("Unit ${u.unit} — $title", "அலகு ${u.unit} — $title"), modifier = Modifier.clickable { open = !open }) {
                if (open) {
                    Bullets(topics)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpenLessons) { Text(tr("Open lessons", "பாடங்களைத் திற")) }
                } else Para(tr("${topics.size} topics · tap to expand", "${topics.size} தலைப்புகள் · விரிக்க தொடவும்"))
            }
        }
    }
}
