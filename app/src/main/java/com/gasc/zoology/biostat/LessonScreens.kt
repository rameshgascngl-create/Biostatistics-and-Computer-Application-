package com.gasc.zoology.biostat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun LessonListScreen(content: CourseContent, store: ProgressStore, onOpen: (String) -> Unit) {
    val ta = store.tamil
    var unit by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var onlyMarked by rememberSaveable { mutableStateOf(false) }
    val shown = content.lessons.filter { l ->
        (unit == 0 || l.unit == unit) && (!onlyMarked || l.id in store.bookmarks) &&
            (query.isBlank() || (if (ta) content.tamilLessons[l.id]?.title.orEmpty() else l.title).contains(query.trim(), ignoreCase = true))
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("lesson-search"),
                label = { Text(tr("Search lessons", "பாடங்களைத் தேடு")) })
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((0..6).toList()) { u ->
                    FilterChip(selected = unit == u, onClick = { unit = u },
                        label = { Text(if (u == 0) tr("All", "அனைத்தும்") else tr("Unit $u", "அலகு $u")) })
                }
                item { FilterChip(selected = onlyMarked, onClick = { onlyMarked = !onlyMarked }, label = { Text(tr("★ Bookmarks", "★ புத்தகக்குறிகள்")) }) }
            }
        }
        if (shown.isEmpty()) item { Para(tr("No lessons match.", "பொருந்தும் பாடங்கள் இல்லை.")) }
        items(shown, key = { it.id }) { l ->
            val done = l.id in store.completed
            val title = if (ta) content.tamilLessons[l.id]?.title.orEmpty().ifBlank { l.id } else l.title
            SectionCard(
                title = (if (done) "✓ " else "") + (if (l.id in store.bookmarks) "★ " else "") + title,
                modifier = Modifier.clickable { onOpen(l.id) }.testTag("lesson-${l.id}")
            ) {
                Para(if (ta) "அலகு ${l.unit} · ${l.study.read} நிமிட வாசிப்பு" else "Unit ${l.unit} · ${l.difficulty} · ${l.study.read} min read")
            }
        }
    }
}

@Composable
fun LessonScreen(content: CourseContent, l: Lesson, store: ProgressStore, onOpen: (String) -> Unit) {
    val ta = store.tamil
    val t = content.tamilLessons[l.id]
    val idx = content.lessons.indexOfFirst { it.id == l.id }
    val prev = content.lessons.getOrNull(idx - 1)
    val next = content.lessons.getOrNull(idx + 1)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(if (ta) t?.title.orEmpty() else l.title, style = MaterialTheme.typography.headlineSmall)
            Para(if (ta) "அலகு ${l.unit} · ${l.study.read} நிமிட வாசிப்பு · ${l.study.practice} நிமிடப் பயிற்சி"
                 else "Unit ${l.unit} · ${l.difficulty} · ${l.study.read} min read · ${l.study.practice} min practice")
            val marked = l.id in store.bookmarks
            OutlinedButton(onClick = { store.toggleBookmark(l.id) }, modifier = Modifier.testTag("bookmark")) {
                Text(if (marked) tr("★ Bookmarked", "★ புத்தகக்குறியிடப்பட்டது") else tr("☆ Bookmark", "☆ புத்தகக்குறியிடு"))
            }
        }
        if (ta) { if (t != null) tamilBody(this, t, content, l) } else englishBody(this, l)
        item {
            var note by remember(l.id) { mutableStateOf(store.notes[l.id].orEmpty()) }
            SectionCard(tr("My notes", "என் குறிப்புகள்")) {
                OutlinedTextField(value = note, onValueChange = { note = it.take(ProgressStore.MAX_NOTE); store.setNote(l.id, note) },
                    modifier = Modifier.fillMaxWidth().testTag("note"), minLines = 3,
                    label = { Text(tr("Write a note for this lesson", "இப்பாடத்திற்கான குறிப்பை எழுதுக")) })
            }
        }
        item {
            val done = l.id in store.completed
            Button(onClick = { store.toggleLesson(l.id) }, modifier = Modifier.fillMaxWidth().testTag("complete")) {
                Text(if (done) tr("Completed — tap to undo", "முடிந்தது — மாற்ற தொடவும்") else tr("Mark lesson as complete", "பாடத்தை முடிந்ததாகக் குறி"))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (prev != null) OutlinedButton(onClick = { onOpen(prev.id) }) { Text(tr("← Previous", "← முந்தையது")) }
                if (next != null) OutlinedButton(onClick = { onOpen(next.id) }) { Text(tr("Next →", "அடுத்தது →")) }
            }
        }
    }
}

private fun englishBody(s: androidx.compose.foundation.lazy.LazyListScope, l: Lesson) = with(s) {
    if (l.objectives.isNotEmpty()) item { SectionCard("Learning objectives") { Bullets(l.objectives) } }
    if (l.intro.isNotBlank()) item { SectionCard("Introduction") { Para(l.intro) } }
    if (l.definition.isNotBlank()) item { SectionCard("Definition") { Para(l.definition) } }
    if (l.history.isNotBlank()) item { SectionCard("Background") { Para(l.history) } }
    if (l.need.isNotEmpty()) item { SectionCard("Why it matters") { Bullets(l.need) } }
    if (l.classification.isNotEmpty()) item { SectionCard("Classification") { l.classification.forEach { Para("${it.name}: ${it.text}") } } }
    if (l.explanation.isNotEmpty()) item { SectionCard("Explanation") { l.explanation.forEach { Para(it) } } }
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
    if (l.examples.isNotEmpty()) item { SectionCard("Worked examples") { l.examples.forEach { ExampleBlock(it) } } }
    if (l.applications.isNotEmpty()) item { SectionCard("Applications") { l.applications.forEach { (field, notes) -> Para(field); Bullets(notes) } } }
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
}

private fun tamilBody(s: androidx.compose.foundation.lazy.LazyListScope, t: TamilLesson, content: CourseContent, l: Lesson) = with(s) {
    if (t.overview.isNotBlank()) item { SectionCard("மேலோட்டம்") { Para(t.overview) } }
    if (t.definition.isNotBlank()) item { SectionCard("வரையறை") { Para(t.definition) } }
    if (t.importance.isNotEmpty()) item { SectionCard("முக்கியத்துவம்") { Bullets(t.importance) } }
    if (t.explanation.isNotEmpty()) item { SectionCard("விளக்கம்") { t.explanation.forEach { Para(it) } } }
    items(l.formulae) { f ->
        val tf = content.extra.formulas["${l.id}|${f.name}"]
        if (tf != null) SectionCard("சூத்திரம் — ${tf.name}") {
            Mono(f.formula); Para(tf.meaning); Para("பயன்பாடு: ${tf.use}")
        }
    }
    if (t.bioExamples.isNotEmpty()) item { SectionCard("எடுத்துக்காட்டுகள்") { Bullets(t.bioExamples) } }
    if (t.points.isNotEmpty()) item { SectionCard("முக்கியக் குறிப்புகள்") { Bullets(t.points) } }
    if (t.exam.isNotEmpty()) item { SectionCard("தேர்வு வழிகாட்டி") { Bullets(t.exam) } }
    if (t.mistakes.isNotEmpty()) item { SectionCard("பொதுவான தவறுகள்") { Bullets(t.mistakes) } }
    if (t.summary.isNotEmpty()) item { SectionCard("சுருக்கம்") { Bullets(t.summary) } }
    if (t.selfcheck.isNotEmpty()) item { SectionCard("சுயசோதனை") { Bullets(t.selfcheck) } }
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
