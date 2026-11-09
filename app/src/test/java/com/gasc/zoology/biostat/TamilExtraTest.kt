package com.gasc.zoology.biostat

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tamil mode must be complete and free of English prose for formulae, flashcards, syllabus and toolkit. */
class TamilExtraTest {
    private val dir = File("src/main/assets/content")
    private fun t(n: String) = File(dir, "$n.json").readText(Charsets.UTF_8)
    private val c = ContentRepository.parse(t("lessons"), t("tamil_lessons"), t("questions"), t("tamil_questions"),
        t("flashcards"), t("syllabus"), t("tamil_extra"))

    private val allowed = setOf("ANOVA", "SPSS", "CPU", "ALU", "RR", "OR", "AR", "SD", "SE", "IF", "SUM", "AVERAGE", "MS", "SS", "df",
        "AA", "Aa", "aa", "Excel", "Welch", "Tukey", "pp", "Z", "NNT", "yx", "xy", "sd", "pq")
    private fun latin(s: String) = Regex("[A-Za-z]{2,}").findAll(s).map { it.value }.filter { it !in allowed }.toList()
    private fun isTamil(s: String) = s.any { it in '஀'..'௿' }

    @Test fun everyFormulaHasTamil() {
        val keys = c.lessons.flatMap { l -> l.formulae.map { "${l.id}|${it.name}" } }
        assertEquals(43, keys.size)
        keys.forEach { k ->
            val f = c.extra.formulas[k] ?: error("missing Tamil formula $k")
            assertTrue(k, isTamil(f.name) && isTamil(f.meaning) && isTamil(f.use))
            assertEquals("$k leaks: ${latin(f.name + f.meaning + f.use)}", emptyList<String>(), latin(f.name + f.meaning + f.use))
        }
    }

    @Test fun everyFlashcardHasTamilDefinition() {
        c.flashcards.forEach { f ->
            val tc = c.extra.flashcards[f.id] ?: error("missing Tamil card ${f.id}")
            assertTrue(f.id, isTamil(tc.definition))
            assertEquals("${f.id} leaks", emptyList<String>(), latin(tc.definition + tc.term))
            if (f.id.startsWith("F0")) assertTrue("${f.id} term", isTamil(tc.term))
            else assertTrue("${f.id} lesson title fallback", isTamil(c.tamilLessons[f.lesson]?.title.orEmpty()))
        }
    }

    @Test fun syllabusMatches() {
        assertEquals(c.syllabus.size, c.extra.syllabus.size)
        c.syllabus.forEachIndexed { i, u ->
            assertEquals("unit ${u.unit} topic count", u.topics.size, c.extra.syllabus[i].topics.size)
            assertEquals(emptyList<String>(), latin(c.extra.syllabus[i].title + c.extra.syllabus[i].topics.joinToString(" ")))
        }
    }

    @Test fun toolkitIsBilingualAndSeparate() {
        val k = c.extra.toolkit
        assertEquals(6, k.mnemonics.size); assertEquals(5, k.problems.size); assertEquals(6, k.cheat.size)
        (k.mnemonics + k.problems + k.cheat).forEach {
            assertTrue(isTamil(it.ta.title)); assertTrue(!isTamil(it.en.title + it.en.text + it.en.task + it.en.lines.joinToString()))
            assertEquals(emptyList<String>(), latin(it.ta.title + it.ta.phrase + it.ta.text + it.ta.task))
        }
    }

    @Test fun tamilQuizIsCompleteAndIndependent() {
        c.questions.forEach { q ->
            val tq = c.tamilQuestions.getValue(q.id)
            assertTrue("${q.id} Tamil question", isTamil(tq.q))
            // A few explanations are pure arithmetic (e.g. "z = (26−20)/3 = 2") and are identical in both languages.
            assertTrue("${q.id} Tamil explanation", isTamil(tq.e) || !Regex("[A-Za-z]{3,}").containsMatchIn(tq.e))
            assertEquals("${q.id} Tamil option count", q.o.size, tq.o.size)
        }
    }
}
