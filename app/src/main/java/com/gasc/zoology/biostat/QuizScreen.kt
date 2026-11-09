package com.gasc.zoology.biostat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val UNITS = listOf("All", "I", "II", "III", "IV", "V", "VI")

@Composable
fun QuizScreen(content: CourseContent, store: ProgressStore) {
    var unit by rememberSaveable { mutableStateOf("All") }
    var count by rememberSaveable { mutableIntStateOf(10) }
    var session by remember { mutableStateOf<List<Question>?>(null) }
    val s = session
    if (s == null) {
        val pool = content.questions.filter { unit == "All" || it.unit == unit }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard("Practice quiz") {
                    Para("Choose a unit and the number of questions.")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(UNITS) { u ->
                            FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(10, 20, 50)) { n ->
                            FilterChip(selected = count == n, onClick = { count = n }, label = { Text("$n") })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Para("${pool.size} questions available")
                    Button(onClick = { session = pool.shuffled().take(count) }, enabled = pool.isNotEmpty()) {
                        Text("Start quiz")
                    }
                }
            }
        }
    } else {
        QuizSession(content, store, s, onFinish = { session = null })
    }
}

@Composable
private fun QuizSession(content: CourseContent, store: ProgressStore, qs: List<Question>, onFinish: () -> Unit) {
    var i by rememberSaveable { mutableIntStateOf(0) }
    var picked by rememberSaveable { mutableIntStateOf(-1) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    if (i >= qs.size) {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                SectionCard("Quiz complete") {
                    Para("Score: $score / ${qs.size}")
                    Button(onClick = onFinish) { Text("Back to quiz setup") }
                }
            }
        }
        return
    }
    val q = qs[i]
    val ta = if (store.tamil) content.tamilQuestions[q.id] else null
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Question ${i + 1} of ${qs.size}", style = MaterialTheme.typography.labelLarge)
            SectionCard("Unit ${q.unit}") {
                Para(q.q)
                if (ta != null && ta.q.isNotBlank()) Para(ta.q)
            }
        }
        items(q.o.indices.toList()) { k ->
            val answered = picked >= 0
            val label = buildString {
                append("${'A' + k}. ${q.o[k]}")
                val t = ta?.o?.getOrNull(k)
                if (t != null && t.isNotBlank() && t != q.o[k]) append("\n$t")
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
            }, modifier = Modifier.fillMaxWidth()) { Text(label) })
        }
        if (picked >= 0) item {
            SectionCard(if (picked == q.a) "Correct" else "Not quite") {
                Para(q.e)
                if (ta != null && ta.e.isNotBlank()) Para(ta.e)
            }
            Spacer(Modifier.height(8.dp))
            Column {
                Button(onClick = { i += 1; picked = -1 }) {
                    Text(if (i + 1 < qs.size) "Next question" else "See score")
                }
            }
        }
    }
}
