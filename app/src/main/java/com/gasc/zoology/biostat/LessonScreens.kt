package com.gasc.zoology.biostat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LessonListScreen(content: CourseContent, store: ProgressStore, onOpen: (String) -> Unit) {
    var unit by rememberSaveable { mutableStateOf(0) }
    val shown = content.lessons.filter { unit == 0 || it.unit == unit }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((0..6).toList()) { u ->
                    FilterChip(selected = unit == u, onClick = { unit = u },
                        label = { Text(if (u == 0) "All" else "Unit $u") })
                }
            }
        }
        items(shown, key = { it.id }) { l ->
            val ta = if (store.tamil) content.tamilLessons[l.id] else null
            val done = l.id in store.completed
            SectionCard(
                title = (if (done) "✓ " else "") + l.title,
                modifier = Modifier.clickable { onOpen(l.id) }
            ) {
                if (ta != null && ta.title.isNotBlank()) Para(ta.title)
                Para("Unit ${l.unit} · ${l.difficulty} · ${l.study.read} min read")
            }
        }
    }
}

@Composable
fun LessonScreen(content: CourseContent, l: Lesson, store: ProgressStore, onOpen: (String) -> Unit) {
    val ta = if (store.tamil) content.tamilLessons[l.id] else null
    val idx = content.lessons.indexOfFirst { it.id == l.id }
    val prev = content.lessons.getOrNull(idx - 1)
    val next = content.lessons.getOrNull(idx + 1)
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(l.title, style = MaterialTheme.typography.headlineSmall)
            if (ta != null && ta.title.isNotBlank())
                Text(ta.title, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            Para("Unit ${l.unit} · ${l.difficulty} · ${l.study.read} min read · ${l.study.practice} min practice")
        }
        if (ta != null) item { TamilSections(ta) }
        if (l.objectives.isNotEmpty()) item { SectionCard("Learning objectives") { Bullets(l.objectives) } }
        if (l.intro.isNotBlank()) item { SectionCard("Introduction") { Para(l.intro) } }
        if (l.definition.isNotBlank()) item { SectionCard("Definition") { Para(l.definition) } }
        if (l.history.isNotBlank()) item { SectionCard("Background") { Para(l.history) } }
        if (l.need.isNotEmpty()) item { SectionCard("Why it matters") { Bullets(l.need) } }
        if (l.classification.isNotEmpty()) item {
            SectionCard("Classification") {
                l.classification.forEach { Para("${it.name}: ${it.text}") }
            }
        }
        if (l.explanation.isNotEmpty()) item {
            SectionCard("Explanation") { l.explanation.forEach { Para(it) } }
        }
        items(l.formulae) { f ->
            SectionCard("Formula — ${f.name}") {
                Mono(f.formula)
                if (f.meaning.isNotBlank()) Para(f.meaning)
                f.symbols.forEach { (k, v) -> Para("$k — $v") }
                if (f.assumptions.isNotEmpty()) { Para("Assumptions:"); Bullets(f.assumptions) }
                if (f.use.isNotBlank()) Para("Use: ${f.use}")
                if (f.avoid.isNotBlank()) Para("Avoid when: ${f.avoid}")
                if (f.bio.isNotBlank()) Para("Biological example: ${f.bio}")
            }
        }
        if (l.examples.isNotEmpty()) item {
            SectionCard("Worked examples") {
                l.examples.forEach { ExampleBlock(it) }
            }
        }
        if (l.applications.isNotEmpty()) item {
            SectionCard("Applications") {
                l.applications.forEach { (field, notes) -> Para(field); Bullets(notes) }
            }
        }
        if (l.advantages.isNotEmpty()) item { SectionCard("Advantages") { Bullets(l.advantages) } }
        if (l.limitations.isNotEmpty()) item { SectionCard("Limitations") { Bullets(l.limitations) } }
        if (l.precautions.isNotEmpty()) item { SectionCard("Precautions") { Bullets(l.precautions) } }
        if (l.misconceptions.isNotEmpty()) item { SectionCard("Common misconceptions") { Bullets(l.misconceptions) } }
        if (l.confused.isNotEmpty()) item { SectionCard("Often confused") { Bullets(l.confused) } }
        if (l.exam.isNotEmpty()) item { SectionCard("Exam guidance") { Bullets(l.exam) } }
        if (l.mnemonic.isNotBlank()) item { SectionCard("Memory aid") { Para(l.mnemonic) } }
        if (l.summary.isNotEmpty()) item { SectionCard("Summary") { Bullets(l.summary) } }
        if (l.selfcheck.isNotEmpty()) item { SectionCard("Self-check") { Bullets(l.selfcheck) } }
        if (l.references.isNotEmpty()) item { SectionCard("References") { Bullets(l.references) } }
        item {
            val done = l.id in store.completed
            Button(onClick = { store.toggleLesson(l.id) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (done) "Completed — tap to undo" else "Mark lesson as complete")
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (prev != null) OutlinedButton(onClick = { onOpen(prev.id) }) { Text("← Previous") }
                if (next != null) OutlinedButton(onClick = { onOpen(next.id) }) { Text("Next →") }
            }
        }
    }
}

@Composable
private fun ExampleBlock(e: Example) {
    var show by rememberSaveable(e.q) { mutableStateOf(false) }
    Para("${e.level}: ${e.q}")
    if (e.steps.isNotEmpty()) Bullets(e.steps)
    if (show) Para("Answer: ${e.answer}")
    OutlinedButton(onClick = { show = !show }) { Text(if (show) "Hide answer" else "Show answer") }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun TamilSections(t: TamilLesson) {
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (t.overview.isNotBlank()) SectionCard("தமிழ் — மேலோட்டம்", tamil = true) { Para(t.overview) }
        if (t.definition.isNotBlank()) SectionCard("வரையறை", tamil = true) { Para(t.definition) }
        if (t.importance.isNotEmpty()) SectionCard("முக்கியத்துவம்", tamil = true) { Bullets(t.importance) }
        if (t.explanation.isNotEmpty()) SectionCard("விளக்கம்", tamil = true) { t.explanation.forEach { Para(it) } }
        if (t.bioExamples.isNotEmpty()) SectionCard("எடுத்துக்காட்டுகள்", tamil = true) { Bullets(t.bioExamples) }
        if (t.points.isNotEmpty()) SectionCard("முக்கியக் குறிப்புகள்", tamil = true) { Bullets(t.points) }
        if (t.exam.isNotEmpty()) SectionCard("தேர்வு வழிகாட்டி", tamil = true) { Bullets(t.exam) }
        if (t.mistakes.isNotEmpty()) SectionCard("பொதுவான தவறுகள்", tamil = true) { Bullets(t.mistakes) }
        if (t.summary.isNotEmpty()) SectionCard("சுருக்கம்", tamil = true) { Bullets(t.summary) }
        if (t.selfcheck.isNotEmpty()) SectionCard("சுயசோதனை", tamil = true) { Bullets(t.selfcheck) }
    }
}
