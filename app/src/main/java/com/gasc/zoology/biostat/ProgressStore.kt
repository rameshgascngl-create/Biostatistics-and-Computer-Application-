package com.gasc.zoology.biostat

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

data class ImportSummary(val completed: Int, val bookmarks: Int, val notes: Int, val known: Int, val answered: Int)

/** On-device store (SharedPreferences) for language choice, progress, bookmarks, notes and known flashcards. */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    var tamil by mutableStateOf(prefs.getBoolean("tamil", false))
        private set
    val completed: SnapshotStateList<String> = loadList("completed")
    val answeredRight: SnapshotStateList<String> = loadList("right")
    val bookmarks: SnapshotStateList<String> = loadList("bookmarks")
    val known: SnapshotStateList<String> = loadList("known")
    val notes: SnapshotStateMap<String, String> = androidx.compose.runtime.mutableStateMapOf<String, String>().also { m ->
        runCatching { Json.parseToJsonElement(prefs.getString("notes", "{}") ?: "{}").jsonObject
            .forEach { (k, v) -> (v as? JsonPrimitive)?.content?.let { m[k] = it } } }
    }
    var quizAttempts by mutableStateOf(prefs.getInt("attempts", 0))
        private set

    private fun loadList(key: String) = (prefs.getStringSet(key, emptySet()) ?: emptySet()).toList().sorted().toMutableStateList()
    private fun saveList(key: String, l: List<String>) = prefs.edit().putStringSet(key, l.toSet()).apply()
    private fun saveNotes() = prefs.edit().putString("notes",
        JsonObject(notes.mapValues { JsonPrimitive(it.value) }).toString()).apply()

    fun toggleTamil() { tamil = !tamil; prefs.edit().putBoolean("tamil", tamil).apply() }

    fun toggleLesson(id: String) { if (!completed.remove(id)) completed.add(id); saveList("completed", completed) }
    fun toggleBookmark(id: String) { if (!bookmarks.remove(id)) bookmarks.add(id); saveList("bookmarks", bookmarks) }
    fun toggleKnown(id: String) { if (!known.remove(id)) known.add(id); saveList("known", known) }
    fun setNote(id: String, text: String) {
        if (text.isBlank()) notes.remove(id) else notes[id] = text.take(MAX_NOTE)
        saveNotes()
    }

    fun recordAnswer(id: String, correct: Boolean) {
        if (correct && id !in answeredRight) answeredRight.add(id)
        quizAttempts += 1
        prefs.edit().putStringSet("right", answeredRight.toSet()).putInt("attempts", quizAttempts).apply()
    }

    fun reset() {
        completed.clear(); answeredRight.clear(); bookmarks.clear(); known.clear(); notes.clear(); quizAttempts = 0
        prefs.edit().remove("completed").remove("right").remove("bookmarks").remove("known").remove("notes").remove("attempts").apply()
    }

    /** JSON compatible with the legacy web app's export (completed, bookmarks, notes, known, settings) plus native extras. */
    fun exportJson(): String {
        val o = buildJsonObject {
            put("version", "native-2.0"); put("exportedAt", java.time.Instant.now().toString())
            put("completed", JsonArray(completed.sorted().map(::JsonPrimitive)))
            put("bookmarks", JsonArray(bookmarks.sorted().map(::JsonPrimitive)))
            put("known", JsonArray(known.sorted().map(::JsonPrimitive)))
            put("notes", JsonObject(notes.toSortedMap().mapValues { JsonPrimitive(it.value) }))
            put("answeredRight", JsonArray(answeredRight.sorted().map(::JsonPrimitive)))
            put("quizAttempts", quizAttempts)
            put("settings", buildJsonObject { put("tamil", tamil) })
        }
        return Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), o)
    }

    /** Validates against the course content, replaces the current state only if the file is well-formed. */
    fun importJson(text: String, content: CourseContent): Result<ImportSummary> = runCatching {
        if (text.length > MAX_IMPORT) throw IllegalArgumentException("too large")
        val root = Json.parseToJsonElement(text).jsonObject
        fun ids(key: String, valid: (String) -> Boolean) = ((root[key] as? JsonArray) ?: JsonArray(emptyList()))
            .mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }.filter(valid).distinct()
        val lessonIds = content.lessons.map { it.id }.toSet(); val cardIds = content.flashcards.map { it.id }.toSet()
        val qIds = content.questions.map { it.id }.toSet()
        val newCompleted = ids("completed") { it in lessonIds }; val newBook = ids("bookmarks") { it in lessonIds }
        val newKnown = ids("known") { it in cardIds }; val newRight = ids("answeredRight") { it in qIds }
        val newNotes = ((root["notes"] as? JsonObject) ?: JsonObject(emptyMap())).mapNotNull { (k, v) ->
            val s = (v as? JsonPrimitive)?.takeIf { it.isString }?.content
            if (k in lessonIds && s != null && s.isNotBlank()) k to s.take(MAX_NOTE) else null }.toMap()
        val attempts = ((root["quizAttempts"] as? JsonPrimitive)?.intOrNull ?: 0).coerceIn(0, 1_000_000)
        val tam = ((root["settings"] as? JsonObject)?.get("tamil") as? JsonPrimitive)?.booleanOrNull
        if (root["completed"] == null && root["notes"] == null && root["known"] == null && root["bookmarks"] == null)
            throw IllegalArgumentException("not a progress file")
        completed.clear(); completed.addAll(newCompleted); saveList("completed", completed)
        bookmarks.clear(); bookmarks.addAll(newBook); saveList("bookmarks", bookmarks)
        known.clear(); known.addAll(newKnown); saveList("known", known)
        answeredRight.clear(); answeredRight.addAll(newRight)
        notes.clear(); notes.putAll(newNotes); saveNotes()
        quizAttempts = attempts
        prefs.edit().putStringSet("right", answeredRight.toSet()).putInt("attempts", attempts).apply()
        if (tam != null && tam != tamil) toggleTamil()
        ImportSummary(newCompleted.size, newBook.size, newNotes.size, newKnown.size, newRight.size)
    }

    companion object { const val MAX_NOTE = 20000; const val MAX_IMPORT = 1_000_000 }
}
