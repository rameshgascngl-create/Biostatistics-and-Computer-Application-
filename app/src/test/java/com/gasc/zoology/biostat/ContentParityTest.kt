package com.gasc.zoology.biostat

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies that the bundled JSON content decodes into the native models and is bilingual-complete. */
class ContentParityTest {
    private val dir = File("src/main/assets/content")
    private fun t(n: String) = File(dir, "$n.json").readText(Charsets.UTF_8)

    private val c = ContentRepository.parse(
        t("lessons"), t("tamil_lessons"), t("questions"), t("tamil_questions"), t("flashcards"), t("syllabus")
    )

    @Test fun counts() {
        assertEquals(44, c.lessons.size)
        assertEquals(176, c.questions.size)
        assertEquals(6, c.syllabus.size)
        assertTrue(c.flashcards.isNotEmpty())
    }

    @Test fun everyLessonHasTamil() {
        assertEquals(c.lessons.map { it.id }.toSet(), c.tamilLessons.keys)
        c.lessons.forEach { l ->
            val ta = c.tamilLessons.getValue(l.id)
            assertTrue("${l.id} title", ta.title.isNotBlank())
            assertTrue("${l.id} overview", ta.overview.isNotBlank())
            assertTrue("${l.id} explanation", ta.explanation.isNotEmpty())
            assertTrue("${l.id} selfcheck", ta.selfcheck.isNotEmpty())
        }
    }

    @Test fun everyQuestionHasTamilAndValidAnswer() {
        assertEquals(c.questions.map { it.id }.toSet(), c.tamilQuestions.keys)
        c.questions.forEach { q ->
            assertEquals("${q.id} options", 4, q.o.size)
            assertTrue("${q.id} answer index", q.a in 0..3)
            val ta = c.tamilQuestions.getValue(q.id)
            assertTrue("${q.id} tamil stem", ta.q.isNotBlank())
            assertEquals("${q.id} tamil options", 4, ta.o.size)
            assertTrue("${q.id} tamil explanation", ta.e.isNotBlank())
        }
    }

    @Test fun flashcardsPointToLessons() {
        val ids = c.lessons.map { it.id }.toSet()
        c.flashcards.forEach { assertTrue(it.id, it.lesson in ids) }
    }
}
