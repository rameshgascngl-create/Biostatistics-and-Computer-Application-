package com.gasc.zoology.biostat

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

/** Small on-device store (SharedPreferences) for language choice and learning progress. */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    var tamil by mutableStateOf(prefs.getBoolean("tamil", false))
        private set
    val completed: SnapshotStateList<String> =
        (prefs.getStringSet("completed", emptySet()) ?: emptySet()).toList().toMutableStateList()
    val answeredRight: SnapshotStateList<String> =
        (prefs.getStringSet("right", emptySet()) ?: emptySet()).toList().toMutableStateList()
    var quizAttempts by mutableStateOf(prefs.getInt("attempts", 0))
        private set

    fun toggleTamil() { tamil = !tamil; prefs.edit().putBoolean("tamil", tamil).apply() }

    fun toggleLesson(id: String) {
        if (!completed.remove(id)) completed.add(id)
        prefs.edit().putStringSet("completed", completed.toSet()).apply()
    }

    fun recordAnswer(id: String, correct: Boolean) {
        if (correct && id !in answeredRight) answeredRight.add(id)
        quizAttempts += 1
        prefs.edit().putStringSet("right", answeredRight.toSet()).putInt("attempts", quizAttempts).apply()
    }
}
