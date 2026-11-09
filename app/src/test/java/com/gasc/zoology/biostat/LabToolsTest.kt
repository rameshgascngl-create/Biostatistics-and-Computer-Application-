package com.gasc.zoology.biostat

import com.gasc.zoology.biostat.lab.*
import com.gasc.zoology.biostat.stats.StatException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabToolsTest {
    /** Latin words that may legitimately appear inside Tamil text: statistical symbols, test and author names. */
    private val allowed = setOf("df", "SE", "SD", "CV", "ANOVA", "HSD", "MSW", "NNT", "NNH", "IQR", "RR", "OR", "Cohen", "Tukey", "Kramer",
        "Dunn", "Fisher", "Hardy", "Weinberg", "Pearson", "Spearman", "Mann", "Whitney", "Wilcoxon", "Welch", "Kruskal", "Wallis",
        "AA", "Aa", "aa", "pp", "Col", "Row", "Q", "Z", "Y", "X", "MS", "SS", "dz", "Student", "JSON", "TA", "EN", "Strain", "Before", "After")

    private fun latinWords(s: String) = Regex("[A-Za-z]{2,}").findAll(s).map { it.value }.filter { it !in allowed }.toList()

    private fun allText(o: Out): List<String> = o.metrics.flatMap { listOf(it.first, it.second) } + o.notes +
        o.tables.flatMap { (t, tb) -> listOf(t) + tb.header + tb.rows.flatten() }

    @Test fun seventeenTools() = assertEquals(17, TOOLS.size)

    @Test fun everyToolRunsOnItsExampleInBothLanguages() {
        for (t in TOOLS) for (ta in listOf(false, true)) {
            val o = t.run(t.defaults(), ta)
            assertTrue("${t.id} produced no metrics", o.metrics.isNotEmpty())
            assertTrue("${t.id} has no interpretation", o.notes.isNotEmpty())
        }
    }

    @Test fun tamilModeContainsNoEnglishWords() {
        for (t in TOOLS) {
            val leaks = allText(t.run(t.defaults(), true)).flatMap(::latinWords).distinct()
            assertTrue("${t.id} Tamil output leaks English: $leaks", leaks.isEmpty())
            val ui = listOf(t.titleTa, t.descTa) + t.inputs.flatMap { listOf(it.ta, it.hintTa) + it.options.map { o -> o.ta } }
            val uiLeaks = ui.flatMap(::latinWords).distinct()
            assertTrue("${t.id} Tamil form leaks English: $uiLeaks", uiLeaks.isEmpty())
        }
    }

    @Test fun englishModeContainsNoTamil() {
        for (t in TOOLS) {
            val s = allText(t.run(t.defaults(), false)).joinToString(" ") + t.titleEn + t.descEn
            assertTrue("${t.id} English output contains Tamil script", s.none { it in '஀'..'௿' })
        }
    }

    @Test fun badInputGivesLocalisedErrors() {
        val cases = listOf("descriptive" to mapOf("data" to "1, two"), "ttest" to mapOf("type" to "one", "mu" to "x", "a" to "1,2,3"),
            "chiind" to mapOf("m" to "1 2\n3"), "anova" to mapOf("g" to "1,2,3"), "epi" to mapOf("a" to "1.5", "b" to "2", "c" to "3", "d" to "4"),
            "z" to mapOf("x" to "1", "mean" to "0", "sd" to "0"), "power" to mapOf("n" to "1", "delta" to "1", "sd" to "1", "alpha" to "0.05"))
        for ((id, m) in cases) {
            val t = TOOLS.first { it.id == id }
            val e = runCatching { t.run(t.defaults() + m, false) }.exceptionOrNull()
            assertTrue("$id should throw StatException, got $e", e is StatException)
            val en = errorText(e as StatException, false); val ta = errorText(e, true)
            assertTrue(en.none { it in '஀'..'௿' } && en.isNotBlank())
            assertTrue("$id Tamil error leaks English: $ta", latinWords(ta).isEmpty())
        }
    }

    @Test fun datasetsAnalyseInBothLanguages() {
        assertEquals(13, DATASETS.size)
        for (d in DATASETS) for (ta in listOf(false, true)) {
            val o = d.analyse(ta)
            assertTrue(d.en, o.metrics.isNotEmpty())
            if (ta) { val leaks = allText(o).flatMap(::latinWords).distinct(); assertTrue("${d.en}: $leaks", leaks.isEmpty()) }
        }
    }

    @Test fun decisionTreeIsClosedAndReachesEveryTool() {
        val reachable = mutableSetOf<String>(); val stack = ArrayDeque(listOf("start"))
        while (stack.isNotEmpty()) {
            val id = stack.removeFirst(); if (!reachable.add(id)) continue
            when (val n = TREE[id] ?: error("dangling node $id")) {
                is Question -> n.choices.forEach { stack.add(it.next) }
                is Leaf -> n.tools.forEach { t -> assertTrue("unknown tool $t", TOOLS.any { it.id == t }) }
            }
        }
        val used = reachable.mapNotNull { TREE[it] as? Leaf }.flatMap { it.tools }.toSet()
        assertEquals("tools unreachable from the tree", emptySet<String>(), TOOLS.map { it.id }.toSet() - used)
    }
}
