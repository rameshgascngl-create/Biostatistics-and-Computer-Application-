package com.gasc.zoology.biostat

import com.gasc.zoology.biostat.lab.Leaf
import com.gasc.zoology.biostat.lab.TREE
import com.gasc.zoology.biostat.lab.TreeQuestion
import com.gasc.zoology.biostat.stats.StatException
import com.gasc.zoology.biostat.stats.welchAnova
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Independent SciPy 1.17 references: scipy.stats.f_oneway(*groups, equal_var=false). */
class WelchAnovaTest {
    @Test fun unequalVarianceExamplesMatchSciPy() {
        val a = welchAnova(listOf(
            listOf(12.0, 15.0, 14.0, 11.0, 13.0),
            listOf(14.0, 17.0, 15.0, 13.0, 16.0, 15.0),
            listOf(16.0, 19.0, 15.0, 17.0, 18.0)
        ))
        assertEquals(7.40670760510156, a.f, 1e-9)
        assertEquals(8.322717622080681, a.df2, 1e-9)
        assertEquals(0.014198750210615911, a.p, 1e-8)
        assertEquals(2, a.df1)

        val b = welchAnova(listOf(
            listOf(20.0, 18.0, 21.0, 19.0, 22.0),
            listOf(17.0, 24.0, 27.0, 31.0),
            listOf(21.0, 22.0, 23.0, 22.0, 24.0, 25.0)
        ))
        assertEquals(4.682467071450063, b.f, 1e-9)
        assertEquals(6.0258273430733205, b.df2, 1e-9)
        assertEquals(0.059293996012599534, b.p, 1e-8)
    }

    @Test fun equalVarianceSymmetricExample() {
        val r = welchAnova(listOf(
            listOf(1.0, 2.0, 3.0, 4.0),
            listOf(1.0, 2.0, 3.0, 4.0),
            listOf(2.0, 3.0, 4.0, 5.0)
        ))
        assertEquals(0.72, r.f, 1e-10)
        assertEquals(6.0, r.df2, 1e-10)
        assertEquals(0.5244872612533987, r.p, 1e-8)
    }

    @Test fun rejectsUndefinedWithinGroupVariance() {
        val e = runCatching {
            welchAnova(listOf(listOf(2.0, 2.0, 2.0), listOf(1.0, 2.0, 3.0)))
        }.exceptionOrNull()
        assertTrue(e is StatException)
        assertEquals("zeroWithin", (e as StatException).code)
    }

    @Test fun decisionTreeDistinguishesVarianceAndRankMethods() {
        val multi = TREE["multi_norm"] as TreeQuestion
        assertEquals(listOf("L_anova", "L_welch_anova", "L_kw", "L_robust"),
            multi.choices.map { it.next })
        val welch = TREE["L_welch_anova"] as Leaf
        assertEquals(listOf("welchanova"), welch.tools)
        val kw = TREE["L_kw"] as Leaf
        assertTrue(kw.checkEn.contains("Unequal spread alone"))
        assertTrue((TREE["L_robust"] as Leaf).tools.isEmpty())
        assertTrue((TREE["L_sign1"] as Leaf).tools.isEmpty())
        assertTrue((TREE["L_signpair"] as Leaf).tools.isEmpty())
    }
}
