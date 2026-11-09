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

    /**
     * Validate a complete native or v3.2 legacy progress export before any mutation.
     * Legacy files contain completed/bookmarks/known/notes but no answeredRight or quizAttempts.
     * Reject wrong types and unknown IDs instead of silently converting a damaged backup to empty progress.
     */
    fun importJson(text: String, content: CourseContent): Result<ImportSummary> = runCatching {
        if (text.length > MAX_IMPORT) throw IllegalArgumentException("too large")
        val root = Json.parseToJsonElement(text).jsonObject
        val lessonIds = content.lessons.map { it.id }.toSet()
        val cardIds = content.flashcards.map { it.id }.toSet()
        val questionIds = content.questions.map { it.id }.toSet()

        fun ids(key: String, valid: Set<String>, required: Boolean = true): List<String> {
            val raw = root[key]
            if (raw == null && !required) return emptyList()
            val items = raw as? JsonArray ?: throw IllegalArgumentException("Invalid or missing $key")
            return items.map { element ->
                val value = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
                    ?: throw IllegalArgumentException("Invalid ID in $key")
                if (value !in valid) throw IllegalArgumentException("Unknown ID in $key: $value")
                value
            }.distinct()
        }

        // These four keys occur in both the old web export and the native export.
        // Requiring all four is intentional: a partial/truncated file must not erase the omitted collections.
        val newCompleted = ids("completed", lessonIds)
        val newBookmarks = ids("bookmarks", lessonIds)
        val newKnown = ids("known", cardIds)
        val notesObj = root["notes"] as? JsonObject
            ?: throw IllegalArgumentException("Invalid or missing notes")
        val newNotes = notesObj.map { (key, element) ->
            if (key !in lessonIds) throw IllegalArgumentException("Unknown lesson in notes: $key")
            val value = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?: throw IllegalArgumentException("Invalid note for $key")
            if (value.length > MAX_NOTE) throw IllegalArgumentException("Note exceeds size limit")
            key to value
        }.filter { it.second.isNotBlank() }.toMap()

        // Additional native-only keys are optional for v3.2 legacy interoperability.
        val newCorrect = ids("answeredRight", questionIds, required = false)
        val rawAttempts = root["quizAttempts"]
        val attempts = if (rawAttempts == null) 0 else
            (rawAttempts as? JsonPrimitive)?.intOrNull
                ?: throw IllegalArgumentException("Invalid quizAttempts")
        if (attempts !in 0..1_000_000) throw IllegalArgumentException("quizAttempts out of range")
        val settings = root["settings"]
        if (settings != null && settings !is JsonObject) throw IllegalArgumentException("Invalid settings")
        val lang = (settings as? JsonObject)?.get("tamil")
        val newTamil = if (lang == null) null else
            (lang as? JsonPrimitive)?.booleanOrNull
                ?: throw IllegalArgumentException("Invalid settings.tamil")

        // Persist one fully validated snapshot before changing the in-memory Compose state.
        val notesJson = JsonObject(newNotes.mapValues { JsonPrimitive(it.value) }).toString()
        val editor = prefs.edit()
            .putStringSet("completed", newCompleted.toSet())
            .putStringSet("bookmarks", newBookmarks.toSet())
            .putStringSet("known", newKnown.toSet())
            .putStringSet("right", newCorrect.toSet())
            .putString("notes", notesJson)
            .putInt("attempts", attempts)
        if (newTamil != null) editor.putBoolean("tamil", newTamil)
        if (!editor.commit()) throw IllegalStateException("Unable to save imported progress")

        completed.clear(); completed.addAll(newCompleted)
        bookmarks.clear(); bookmarks.addAll(newBookmarks)
        known.clear(); known.addAll(newKnown)
        answeredRight.clear(); answeredRight.addAll(newCorrect)
        notes.clear(); notes.putAll(newNotes)
        quizAttempts = attempts
        if (newTamil != null) tamil = newTamil
        ImportSummary(newCompleted.size, newBookmarks.size, newNotes.size, newKnown.size, newCorrect.size)
    }

    companion object { const val MAX_NOTE = 20000; const val MAX_IMPORT = 1_000_000 }
}
