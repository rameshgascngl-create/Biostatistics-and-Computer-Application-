package com.gasc.zoology.biostat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun FormulaScreen(content: CourseContent, store: ProgressStore) {
    val ta = store.tamil
    var query by rememberSaveable { mutableStateOf("") }
    val entries = content.lessons.flatMap { l -> l.formulae.map { f -> Triple(l, f, content.extra.formulas["${l.id}|${f.name}"]) } }
    val shown = entries.filter { (_, f, tf) ->
        val name = if (ta) tf?.name.orEmpty() else f.name
        query.isBlank() || name.contains(query.trim(), ignoreCase = true) || f.formula.contains(query.trim(), ignoreCase = true)
    }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(tr("Formula explorer — ${entries.size} formulae", "சூத்திரத் தேடல் — ${entries.size} சூத்திரங்கள்"), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("formula-search"),
            label = { Text(tr("Search by name or symbol", "பெயர் அல்லது குறியீட்டால் தேடு")) })
        shown.forEach { (l, f, tf) ->
            if (ta) { if (tf != null) SectionCard(tf.name) { Mono(f.formula); Para(tf.meaning); Para("பயன்பாடு: ${tf.use}") } }
            else SectionCard(f.name) {
                Mono(f.formula); if (f.meaning.isNotBlank()) Para(f.meaning)
                f.symbols.forEach { (k, v) -> Para("$k — $v") }
                if (f.use.isNotBlank()) Para("Use: ${f.use}")
                if (f.avoid.isNotBlank()) Para("Avoid when: ${f.avoid}")
            }
        }
        if (shown.isEmpty()) Para(tr("No formula matches.", "பொருந்தும் சூத்திரம் இல்லை."))
    }
}

@Composable
fun ToolkitScreen(content: CourseContent, store: ProgressStore) {
    val ta = store.tamil
    val k = content.extra.toolkit
    fun pick(i: ToolkitItem) = if (ta) i.ta else i.en
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(tr("Student toolkit", "மாணவர் கருவித்தொகுப்பு"), style = MaterialTheme.typography.titleLarge)
        Para(tr("Mnemonics, extra practice problems and a formula cheat-sheet for undergraduate Zoology students.", "இளநிலை விலங்கியல் மாணவர்களுக்கான நினைவுத் துணுக்குகள், கூடுதல் பயிற்சிக் கணக்குகள், சூத்திரக் குறிப்புத்தாள்."))
        Text(tr("Key mnemonics", "முக்கிய நினைவுத் துணுக்குகள்"), style = MaterialTheme.typography.titleMedium)
        k.mnemonics.map(::pick).forEach { SectionCard(it.title) { Para(it.phrase); Para(it.text) } }
        Text(tr("Extra biological sample problems", "கூடுதல் உயிரியல் மாதிரிக் கணக்குகள்"), style = MaterialTheme.typography.titleMedium)
        k.problems.map(::pick).forEach { SectionCard(it.title) { Mono(it.data); Para(it.task) } }
        Text(tr("Formula cheat-sheet", "சூத்திரக் குறிப்புத்தாள்"), style = MaterialTheme.typography.titleMedium)
        k.cheat.map(::pick).forEach { SectionCard(it.title) { it.lines.forEach { line -> Mono(line) } } }
    }
}
