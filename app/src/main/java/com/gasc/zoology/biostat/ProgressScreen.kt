package com.gasc.zoology.biostat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ProgressScreen(content: CourseContent, store: ProgressStore) {
    val ctx = LocalContext.current
    val ta = LocalTamil.current
    var message by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) message = runCatching {
            ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(store.exportJson().toByteArray()) }
            if (ta) "முன்னேற்றம் ஏற்றுமதி செய்யப்பட்டது." else "Progress exported."
        }.getOrElse { if (ta) "ஏற்றுமதி தோல்வியடைந்தது." else "Export failed." }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) message = runCatching {
            val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readNBytes(ProgressStore.MAX_IMPORT + 1) }
            if (bytes.size > ProgressStore.MAX_IMPORT) throw IllegalArgumentException("too large")
            val s = store.importJson(String(bytes), content).getOrThrow()
            if (ta) "இறக்குமதி முடிந்தது: ${s.completed} பாடங்கள், ${s.bookmarks} புத்தகக்குறிகள், ${s.notes} குறிப்புகள், ${s.known} அட்டைகள், ${s.answered} சரியான விடைகள்."
            else "Imported: ${s.completed} completed lessons, ${s.bookmarks} bookmarks, ${s.notes} notes, ${s.known} known cards, ${s.answered} correct answers."
        }.getOrElse { if (ta) "செல்லாத அல்லது பொருந்தாத முன்னேற்றக் கோப்பு; எதுவும் மாற்றப்படவில்லை." else "Invalid or incompatible progress file; nothing was changed." }
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("Progress backup", "முன்னேற்றக் காப்பு"), style = MaterialTheme.typography.titleLarge)
        SectionCard(tr("Current progress", "தற்போதைய முன்னேற்றம்")) {
            Bullets(listOf(
                tr("Completed lessons: ${store.completed.size} of ${content.lessons.size}", "முடித்த பாடங்கள்: ${store.completed.size} / ${content.lessons.size}"),
                tr("Bookmarks: ${store.bookmarks.size}", "புத்தகக்குறிகள்: ${store.bookmarks.size}"),
                tr("Notes: ${store.notes.size}", "குறிப்புகள்: ${store.notes.size}"),
                tr("Known flashcards: ${store.known.size} of ${content.flashcards.size}", "அறிந்த அட்டைகள்: ${store.known.size} / ${content.flashcards.size}"),
                tr("Quiz questions answered correctly: ${store.answeredRight.size} of ${content.questions.size}", "சரியாக விடையளித்த வினாக்கள்: ${store.answeredRight.size} / ${content.questions.size}")))
        }
        Button(onClick = { exporter.launch("BCA_Master_Progress.json") }, modifier = Modifier.fillMaxWidth().testTag("export")) { Text(tr("Export progress (JSON)", "முன்னேற்றத்தை ஏற்றுமதி செய் (JSON)")) }
        OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth().testTag("import")) { Text(tr("Import progress (JSON)", "முன்னேற்றத்தை இறக்குமதி செய் (JSON)")) }
        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth().testTag("reset")) { Text(tr("Reset all progress", "முன்னேற்றத்தை முழுவதும் மீட்டமை")) }
        message?.let { Text(it, modifier = Modifier.testTag("progress-message")) }
        Text(tr("Files are exchanged with the web version of this course. Imported data is validated against the course content and capped at 1 MB; invalid files change nothing.",
            "கோப்புகள் இப்பாடத்தின் இணையப் பதிப்புடன் பரிமாறிக்கொள்ளத்தக்கவை. இறக்குமதித் தரவு பாடத்திட்டத்துடன் சரிபார்க்கப்பட்டு 1 MB வரை மட்டுமே ஏற்கப்படும்; செல்லாத கோப்புகள் எதையும் மாற்றாது."), style = MaterialTheme.typography.bodySmall)
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false },
        title = { Text(tr("Reset progress?", "முன்னேற்றத்தை மீட்டமைக்கவா?")) },
        text = { Text(tr("Lesson progress, bookmarks, notes, known cards and quiz results will be erased.", "பாட முன்னேற்றம், புத்தகக்குறிகள், குறிப்புகள், அறிந்த அட்டைகள், வினாடி வினா முடிவுகள் அழிக்கப்படும்.")) },
        confirmButton = { TextButton(onClick = { store.reset(); confirmReset = false; message = if (ta) "மீட்டமைக்கப்பட்டது." else "Progress reset." }) { Text(tr("Reset", "மீட்டமை")) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(tr("Cancel", "ரத்துசெய்")) } })
}
