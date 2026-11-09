package com.gasc.zoology.biostat.lab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gasc.zoology.biostat.LocalTamil
import com.gasc.zoology.biostat.Bullets
import com.gasc.zoology.biostat.SectionCard
import com.gasc.zoology.biostat.stats.StatException
import com.gasc.zoology.biostat.tr

@Composable
fun ToolListScreen(onOpen: (String) -> Unit) {
    val ta = LocalTamil.current
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(tr("Analysis Lab — ${TOOLS.size} tools", "பகுப்பாய்வு ஆய்வகம் — ${TOOLS.size} கருவிகள்"), style = MaterialTheme.typography.titleLarge)
        TOOLS.forEachIndexed { i, tool ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(tool.id) }.testTag("tool-${tool.id}"),
                elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("${i + 1}. ${if (ta) tool.titleTa else tool.titleEn}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(if (ta) tool.descTa else tool.descEn, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolScreen(id: String) {
    val tool = TOOLS.firstOrNull { it.id == id } ?: return
    val ta = LocalTamil.current
    var values by rememberSaveable(id) { mutableStateOf(HashMap(tool.defaults())) }
    var out by remember(id) { mutableStateOf<Out?>(null) }
    var err by remember(id) { mutableStateOf<String?>(null) }
    // Results are recomputed when the language changes so no stale text from the other language is shown.
    fun run() {
        try { out = tool.run(values, ta); err = null }
        catch (e: StatException) { out = null; err = errorText(e, ta) }
        catch (e: Exception) { out = null; err = if (ta) "உள்ளீட்டைச் சரிபார்க்கவும்." else "Please check the input." }
    }
    LaunchedEffect(ta) { if (out != null || err != null) run() }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (ta) tool.titleTa else tool.titleEn, style = MaterialTheme.typography.titleLarge)
        Text(if (ta) tool.descTa else tool.descEn, style = MaterialTheme.typography.bodyMedium)
        tool.inputs.forEach { inp ->
            val label = (if (ta) inp.ta else inp.en)
            when (inp.kind) {
                Kind.CHOICE -> {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        inp.options.forEach { o ->
                            FilterChip(selected = values[inp.key] == o.value, onClick = { values = HashMap(values).also { it[inp.key] = o.value } },
                                label = { Text(if (ta) o.ta else o.en) }, modifier = Modifier.testTag("opt-${inp.key}-${o.value}"))
                        }
                    }
                }
                else -> OutlinedTextField(
                    value = values[inp.key].orEmpty(), onValueChange = { v -> values = HashMap(values).also { it[inp.key] = v } },
                    label = { Text(label) }, modifier = Modifier.fillMaxWidth().testTag("in-${inp.key}"),
                    singleLine = inp.kind == Kind.NUM, minLines = if (inp.kind == Kind.MULTI) 2 else 1, maxLines = 8,
                    supportingText = { val h = if (ta) inp.hintTa else inp.hintEn; if (h.isNotEmpty()) Text(h) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { run() }, modifier = Modifier.testTag("run")) { Text(tr("Calculate", "கணக்கிடு")) }
            OutlinedButton(onClick = { values = HashMap(tool.defaults()); out = null; err = null }) { Text(tr("Reset example", "எடுத்துக்காட்டை மீட்டமை")) }
        }
        err?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("error")) }
        out?.let { ResultView(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultView(o: Out) {
    SectionCard(tr("Result", "முடிவு"), Modifier.testTag("result")) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            o.metrics.forEach { (k, v) ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(10.dp).widthIn(min = 96.dp)) {
                        Text(k, style = MaterialTheme.typography.labelSmall)
                        Text(v, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    o.plot?.let { SectionCard(tr("Plot", "வரைபடம்")) { PlotView(it) } }
    o.tables.forEach { (title, t) ->
        SectionCard(title) {
            Row(Modifier.fillMaxWidth()) { t.header.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) } }
            Spacer(Modifier.height(4.dp))
            t.rows.forEach { row -> Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) { row.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall) } } }
        }
    }
    if (o.notes.isNotEmpty()) SectionCard(tr("Interpretation and assumptions", "விளக்கமும் அனுமானங்களும்")) { Bullets(o.notes) }
}
