package com.gasc.zoology.biostat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    content: CourseContent, store: ProgressStore,
    onOpenLesson: (String) -> Unit, onOpenLessons: () -> Unit
) {
    val total = content.lessons.size
    val done = store.completed.count { id -> content.lessons.any { it.id == id } }
    val next = content.lessons.firstOrNull { it.id !in store.completed }
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard("Course progress") {
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else done / total.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Para("$done of $total lessons completed")
                Para("${store.answeredRight.size} of ${content.questions.size} quiz questions answered correctly")
                Spacer(Modifier.height(4.dp))
                if (next != null) {
                    Button(onClick = { onOpenLesson(next.id) }) {
                        Text(if (done == 0) "Start learning" else "Continue learning")
                    }
                } else {
                    Para("All lessons completed — revise with the quiz and flashcards.")
                }
            }
        }
        item {
            Text("Syllabus", style = MaterialTheme.typography.titleLarge)
        }
        items(content.syllabus) { u ->
            var open by rememberSaveable(u.unit) { mutableStateOf(false) }
            SectionCard("Unit ${u.unit} — ${u.title}",
                modifier = Modifier.clickable { open = !open }) {
                if (open) {
                    Bullets(u.topics)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpenLessons) { Text("Open lessons") }
                } else {
                    Para("${u.topics.size} topics · tap to expand")
                }
            }
        }
    }
}
