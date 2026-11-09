package com.gasc.zoology.biostat

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Coverage gate: book pages must not silently omit lessons or their academic sections. */
class BookPageContentTest {
    private val dir = File("src/main/assets/content")
    private fun read(name: String) = File(dir, "$name.json").readText(Charsets.UTF_8)
    private val course = ContentRepository.parse(
        read("lessons"), read("tamil_lessons"), read("questions"),
        read("tamil_questions"), read("flashcards"), read("syllabus")
    )

    @Test fun allFortyFourChaptersHaveBilingualBookPages() {
        assertEquals(44, course.lessons.size)
        for (lesson in course.lessons) {
            val english = chapterPages(course, lesson, false)
            val tamil = chapterPages(course, lesson, true)
            assertTrue("${lesson.id}: no English pages", english.isNotEmpty())
            assertTrue("${lesson.id}: no Tamil pages", tamil.isNotEmpty())
            assertTrue("${lesson.id}: English heading blank", english.none { it.title.isBlank() })
            assertTrue("${lesson.id}: Tamil heading blank", tamil.none { it.title.isBlank() })
            assertEquals("${lesson.id}: English count", expectedEnglishSections(lesson), english.size)
            assertEquals("${lesson.id}: Tamil count", expectedTamilSections(lesson), tamil.size)
        }
    }

    private fun expectedEnglishSections(l: Lesson): Int = listOf(
        l.objectives.isNotEmpty(), l.intro.isNotBlank(), l.definition.isNotBlank(),
        l.history.isNotBlank(), l.need.isNotEmpty(), l.classification.isNotEmpty(),
        l.explanation.isNotEmpty(), l.examples.isNotEmpty(), l.applications.isNotEmpty(),
        l.advantages.isNotEmpty(), l.limitations.isNotEmpty(), l.precautions.isNotEmpty(),
        l.misconceptions.isNotEmpty(), l.confused.isNotEmpty(), l.exam.isNotEmpty(),
        l.mnemonic.isNotBlank(), l.summary.isNotEmpty(), l.selfcheck.isNotEmpty(),
        l.references.isNotEmpty()
    ).count { it } + l.formulae.size

    private fun expectedTamilSections(l: Lesson): Int {
        val t = course.tamilLessons[l.id] ?: return 1
        return listOf(
            t.overview.isNotBlank(), t.definition.isNotBlank(),
            t.importance.isNotEmpty(), t.explanation.isNotEmpty(),
            t.bioExamples.isNotEmpty(), t.points.isNotEmpty(), t.exam.isNotEmpty(),
            t.mistakes.isNotEmpty(), t.summary.isNotEmpty(), t.selfcheck.isNotEmpty()
        ).count { it } + l.formulae.count { "${l.id}|${it.name}" in course.extra.formulas }
    }
}
