package com.gasc.zoology.biostat

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

private val UNITS = listOf("All", "I", "II", "III", "IV", "V", "VI")
private val TA_LETTERS = listOf("அ", "ஆ", "இ", "ஈ", "உ", "ஊ")

@Composable
fun QuizScreen(content: CourseContent, store: ProgressStore) {
    val ta = store.tamil
    var unit by rememberSaveable { mutableStateOf("All") }
    var count by rememberSaveable { mutableIntStateOf(10) }
    var sessionIds by rememberSaveable { mutableStateOf<List<String>?>(null) }
    val s = sessionIds?.mapNotNull { id -> content.questions.firstOrNull { it.id == id } }
    if (s == null) {
        val pool = content.questions.filter { unit == "All" || it.unit == unit }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard(tr("Practice quiz", "பயிற்சி வினாடி வினா")) {
                    Para(tr("Choose a unit and the number of questions.", "அலகையும் வினாக்களின் எண்ணிக்கையையும் தேர்ந்தெடுக்கவும்."))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(UNITS) { u ->
                            FilterChip(selected = unit == u, onClick = { unit = u },
                                label = { Text(if (u == "All") tr("All", "அனைத்தும்") else u) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(10, 20, 50)) { n -> FilterChip(selected = count == n, onClick = { count = n }, label = { Text("$n") }) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Para(tr("${pool.size} questions available", "${pool.size} வினாக்கள் உள்ளன"))
                    Button(onClick = { sessionIds = pool.shuffled().take(count).map { it.id } }, enabled = pool.isNotEmpty(), modifier = Modifier.testTag("start-quiz")) {
                        Text(tr("Start quiz", "வினாடி வினாவைத் தொடங்கு"))
                    }
                }
            }
        }
    } else {
        QuizSession(content, store, s, ta, onFinish = { sessionIds = null })
    }
}

@Composable
private fun QuizSession(content: CourseContent, store: ProgressStore, qs: List<Question>, ta: Boolean, onFinish: () -> Unit) {
    var i by rememberSaveable { mutableIntStateOf(0) }
    var picked by rememberSaveable { mutableIntStateOf(-1) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    if (i >= qs.size) {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                SectionCard(tr("Quiz complete", "வினாடி வினா முடிந்தது")) {
                    Para(tr("Score: $score / ${qs.size}", "மதிப்பெண்: $score / ${qs.size}"))
                    Button(onClick = onFinish) { Text(tr("Back to quiz setup", "வினாடி வினா அமைப்புக்குத் திரும்பு")) }
                }
            }
        }
        return
    }
    val q = qs[i]
    val tq = content.tamilQuestions[q.id]
    val text = if (ta) tq?.q.orEmpty() else q.q
    val opts = if (ta) tq?.o ?: emptyList() else q.o
    val expl = if (ta) tq?.e.orEmpty() else q.e
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(tr("Question ${i + 1} of ${qs.size}", "வினா ${i + 1} / ${qs.size}"), style = MaterialTheme.typography.labelLarge)
            SectionCard(tr("Unit ${q.unit}", "அலகு ${q.unit}")) { Para(text) }
        }
        items(opts.indices.toList()) { k ->
            val answered = picked >= 0
            val letter = if (ta) TA_LETTERS[k] else "${'A' + k}"
            val label = buildString {
                append("$letter. ${opts[k]}")
                if (answered && k == q.a) append("  ✓")
                if (answered && k == picked && k != q.a) append("  ✗")
            }
            val chosen = answered && (k == q.a || k == picked)
            (if (chosen) Button(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) { Text(label) }
            else OutlinedButton(onClick = {
                if (!answered) {
                    picked = k
                    val ok = k == q.a
                    if (ok) score += 1
                    store.recordAnswer(q.id, ok)
                }
            }, modifier = Modifier.fillMaxWidth().testTag("option-$k")) { Text(label) })
        }
        if (picked >= 0) item {
            SectionCard(if (picked == q.a) tr("Correct", "சரி") else tr("Not quite", "சரியல்ல")) { Para(expl) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { i += 1; picked = -1 }, modifier = Modifier.testTag("next-question")) {
                Text(if (i + 1 < qs.size) tr("Next question", "அடுத்த வினா") else tr("See score", "மதிப்பெண்ணைக் காண்"))
            }
        }
    }
}
