package com.gasc.zoology.biostat

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Loads the course content bundled in assets/content/*.json. */
object ContentRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private fun Context.text(name: String) =
        assets.open("content/$name.json").bufferedReader(Charsets.UTF_8).use { it.readText() }

    fun parse(
        lessons: String, tamilLessons: String, questions: String,
        tamilQuestions: String, flashcards: String, syllabus: String
    ) = CourseContent(
        lessons = json.decodeFromString(ListSerializer(Lesson.serializer()), lessons),
        tamilLessons = json.decodeFromString(
            MapSerializer(String.serializer(), TamilLesson.serializer()), tamilLessons),
        questions = json.decodeFromString(ListSerializer(Question.serializer()), questions),
        tamilQuestions = json.decodeFromString(
            MapSerializer(String.serializer(), TamilQuestion.serializer()), tamilQuestions),
        flashcards = json.decodeFromString(ListSerializer(Flashcard.serializer()), flashcards),
        syllabus = json.decodeFromString(ListSerializer(SyllabusUnit.serializer()), syllabus)
    )

    suspend fun load(ctx: Context): CourseContent = withContext(Dispatchers.Default) {
        parse(
            ctx.text("lessons"), ctx.text("tamil_lessons"), ctx.text("questions"),
            ctx.text("tamil_questions"), ctx.text("flashcards"), ctx.text("syllabus")
        )
    }
}
