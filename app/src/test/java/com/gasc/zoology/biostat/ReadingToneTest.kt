package com.gasc.zoology.biostat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ReadingToneTest {
    @Test fun lessonTypesHaveDistinctBackgrounds() {
        val introductions = readingTone("Introduction").surface
        val definitions = readingTone("Definition").surface
        val formulas = readingTone("Formula — Standard error").surface
        val examples = readingTone("Worked examples").surface
        val pitfalls = readingTone("Common misconceptions").surface
        val summaries = readingTone("Summary").surface
        assertEquals(6, setOf(introductions, definitions, formulas, examples, pitfalls, summaries).size)
    }

    @Test fun tamilAndEnglishHeadingsShareSemanticColours() {
        assertEquals(readingTone("Definition"), readingTone("வரையறை"))
        assertEquals(readingTone("Summary"), readingTone("சுருக்கம்"))
        assertEquals(readingTone("Formula — Probability"), readingTone("சூத்திரம் — நிகழ்தகவு"))
        assertEquals(readingTone("Self-check"), readingTone("சுயசோதனை"))
        assertNotEquals(readingTone("வரையறை"), readingTone("எடுத்துக்காட்டுகள்"))
    }
}
