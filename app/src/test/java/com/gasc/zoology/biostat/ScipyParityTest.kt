package com.gasc.zoology.biostat

import com.gasc.zoology.biostat.stats.*
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Compares every Kotlin calculator with reference values produced by SciPy (tools/gen_scipy_reference.py). */
class ScipyParityTest {
    private val ref: JsonObject = Json.parseToJsonElement(
        File("src/test/resources/scipy_reference.json").readText()).jsonObject

    private fun JsonElement.d() = jsonPrimitive.double
    private fun JsonElement.dl() = jsonArray.map { it.d() }
    private fun JsonElement.gl() = jsonArray.map { it.dl() }
    private fun JsonObject.d(k: String) = getValue(k).d()
    private fun JsonObject.dl(k: String) = getValue(k).dl()
    private fun o(k: String) = ref.getValue(k).jsonObject

    private fun near(label: String, expected: Double, actual: Double, rel: Double = 1e-9, abs: Double = 1e-12) {
        val tol = max(abs, rel * abs(expected))
        assertTrue("$label: expected $expected got $actual (tol $tol)", abs(expected - actual) <= tol)
    }

    @Test fun distributions() {
        val d = o("dist")
        d.getValue("tcdf").gl().forEach { (t, df, e) -> near("t.cdf($t,$df)", e, Dist.studentTCdf(t, df), 1e-9, 1e-12) }
        d.getValue("chi2sf").gl().forEach { (x, df, e) -> near("chi2.sf($x,$df)", e, Dist.chiSquareP(x, df), 1e-9, 1e-12) }
        d.getValue("fsf").gl().forEach { (x, a, b, e) -> near("f.sf", e, Dist.fSurvival(x, a, b), 1e-9, 1e-12) }
        d.getValue("normcdf").gl().forEach { (z, e) -> near("norm.cdf($z)", e, Dist.normCdf(z), 1e-9, 1e-12) }
        d.getValue("norminv").gl().forEach { (p, e) -> near("norm.ppf($p)", e, Dist.normInv(p), 1e-9, 1e-9) }
        d.getValue("tinv").gl().forEach { (p, df, e) -> near("t.ppf($p,$df)", e, Dist.tQuantile(p, df), 1e-9, 1e-9) }
        d.getValue("nctcdf").gl().forEach { (t, df, dl, e) -> near("nct.cdf", e, Dist.nctCdf(t, df, dl), 1e-5, 1e-6) }
        d.getValue("srange").gl().forEach { (q, k, df, e) ->
            near("studentized_range.cdf($q,$k,$df)", e, Dist.studentizedRangeCdf(q, k.toInt(), df), 1e-4, 1e-4)
        }
        ref.getValue("fisher").gl().forEach { (a, b, c, dd, e) ->
            near("fisher", e, Dist.fisherExact2x2(a.toInt(), b.toInt(), c.toInt(), dd.toInt()), 1e-8, 1e-12)
        }
    }

    @Test fun descriptiveStats() {
        val r = o("descriptive"); val s = descriptive(r.dl("x"))
        assertEquals(r.d("n").toInt(), s.n)
        near("mean", r.d("mean"), s.mean); near("median", r.d("median"), s.median)
        near("q1", r.d("q1"), s.q1); near("q3", r.d("q3"), s.q3)
        near("var", r.d("sampleVar"), s.sampleVariance); near("sd", r.d("sampleSD"), s.sampleSD)
        near("popvar", r.d("popVar"), s.popVariance); near("se", r.d("se"), s.se)
        near("cv", r.d("cv"), s.cv); near("skew", r.d("skew"), s.skew)
        near("ciLow", r.d("ciLow"), s.ciLow, 1e-8); near("ciHigh", r.d("ciHigh"), s.ciHigh, 1e-8)
        assertEquals(listOf(14.0), s.modes)
    }

    private fun checkT(key: String, type: TType, a: List<Double>, b: List<Double>, mu: Double = 0.0) {
        val r = o(key); val t = tTest(type, a, b, mu)
        near("$key t", r.d("t"), t.t, 1e-9); near("$key df", r.d("df"), t.df, 1e-9)
        near("$key p", r.d("p"), t.p, 1e-8); near("$key ciLow", r.d("ciLow"), t.ciLow, 1e-8)
        near("$key ciHigh", r.d("ciHigh"), t.ciHigh, 1e-8)
    }

    @Test fun tTests() {
        checkT("t_one", TType.ONE, o("t_one").dl("a"), emptyList(), o("t_one").d("mu"))
        checkT("t_pooled", TType.POOLED, o("t_pooled").dl("a"), o("t_pooled").dl("b"))
        checkT("t_welch", TType.WELCH, o("t_welch").dl("a"), o("t_welch").dl("b"))
        checkT("t_paired", TType.PAIRED, o("t_paired").dl("a"), o("t_paired").dl("b"))
    }

    @Test fun chiSquare() {
        val g = o("chi_gof"); val r = chiGof(g.dl("obs"), g.dl("exp"))
        near("gof chi", g.d("chi"), r.chi); near("gof p", g.d("p"), r.p, 1e-8)
        val c = o("chi_ind_2x2"); val i = chiIndependence(c.getValue("m").gl())
        near("ind chi", c.d("chi"), i.chi); near("ind p", c.d("p"), i.p, 1e-8)
        near("v", c.d("v"), i.cramerV); near("fisher", c.d("fisher"), i.fisher, 1e-8)
        val c3 = o("chi_ind_3x3"); val i3 = chiIndependence(c3.getValue("m").gl())
        near("3x3 chi", c3.d("chi"), i3.chi); near("3x3 p", c3.d("p"), i3.p, 1e-8)
        assertEquals(4, i3.df); near("3x3 v", c3.d("v"), i3.cramerV)
    }

    @Test fun correlationRegression() {
        val c = o("corr"); val r = pearson(c.dl("x"), c.dl("y"))
        near("r", c.d("r"), r.r); near("slope", c.d("slope"), r.b); near("intercept", c.d("intercept"), r.a)
        near("p", c.d("p"), r.p, 1e-8); near("seb", c.d("seb"), r.seb)
        near("slopeLow", c.d("slopeLow"), r.slopeLow, 1e-8); near("slopeHigh", c.d("slopeHigh"), r.slopeHigh, 1e-8)
        near("rho", c.d("spearman"), spearman(c.dl("x"), c.dl("y")))
        val s = o("spearman_ties"); near("rho ties", s.d("rho"), spearman(s.dl("x"), s.dl("y")))
    }

    @Test fun anovaAndTukey() {
        val a = o("anova"); val r = anova(a.getValue("groups").gl())
        near("F", a.d("F"), r.f); near("p", a.d("p"), r.p, 1e-8); near("eta", a.d("eta"), r.eta)
        assertEquals(2, r.df1); assertEquals(13, r.df2)
        val t = o("tukey"); val tr = tukeyHsd(t.getValue("groups").gl())
        val pm = t.getValue("p").gl()
        tr.pairs.forEach { near("tukey p ${it.i}-${it.j}", pm[it.i][it.j], it.p, 1e-3, 5e-5) }
    }

    @Test fun nonParametric() {
        val m = o("mw_exact"); val mr = mannWhitney(m.dl("a"), m.dl("b"))
        assertEquals(PMethod.EXACT, mr.method)
        near("U", m.d("u"), mr.u); near("mw exact p", m.d("p"), mr.p, 1e-9)
        val mt = o("mw_ties"); val mtr = mannWhitney(mt.dl("a"), mt.dl("b"))
        assertEquals(PMethod.NORMAL, mtr.method)
        near("U ties", mt.d("u"), mtr.u); near("mw ties p", mt.d("p"), mtr.p, 1e-6)
        val w = o("wilcoxon_exact"); val wr = wilcoxon(w.dl("d"))
        assertEquals(PMethod.EXACT, wr.method)
        near("W", w.d("w"), wr.w); near("w exact p", w.d("p"), wr.p, 1e-9)
        val wt = o("wilcoxon_ties"); val wtr = wilcoxon(wt.dl("d"))
        near("W ties", wt.d("w"), wtr.w); near("w ties p", wt.d("p"), wtr.p, 1e-6)
        val k = o("kruskal"); val kr = kruskalWallis(k.getValue("groups").gl())
        near("H", k.d("H"), kr.h); near("kw p", k.d("p"), kr.p, 1e-8)
        val kt = o("kruskal_ties"); val ktr = kruskalWallis(kt.getValue("groups").gl())
        near("H ties", kt.d("H"), ktr.h); near("kw ties p", kt.d("p"), ktr.p, 1e-8)
    }

    @Test fun ciZPowerQqHwSampleEpi() {
        val c = o("ci"); val r = ciMean(c.dl("x"), 0.95)
        near("ci mean", c.d("mean"), r.mean); near("se", c.d("se"), r.se); near("tcrit", c.d("tcrit"), r.tcrit)
        near("lower", c.d("lower"), r.lower, 1e-8); near("upper", c.d("upper"), r.upper, 1e-8)
        near("99 lower", c.d("conf99low"), ciMean(c.dl("x"), 0.99).lower, 1e-8)
        val z = o("z"); val zr = zScore(z.d("x"), z.d("mean"), z.d("sd"))
        near("z", z.d("z"), zr.z); near("z upper", z.d("upper"), zr.upperTail, 1e-8)
        ref.getValue("power").gl().forEach { (n, dl, sd, al, e) ->
            near("power n=$n", e, powerMean(n, dl, sd, al).power, 1e-5, 1e-6)
        }
        val q = o("qq"); val qr = qqData(q.dl("x"))
        near("qq r", q.d("r"), qr.r, 1e-8)
        q.dl("z").forEachIndexed { i, e -> near("qq z$i", e, qr.points[i].z, 1e-8, 1e-9) }
        val h = o("hw"); val hr = hardyWeinberg(h.dl("obs"))
        near("hw p", h.d("p"), hr.p); near("hw chi", h.d("chi"), hr.chi); near("hw pvalue", h.d("pvalue"), hr.pValue, 1e-8)
        val s = o("samplesize"); val sm = s.getValue("mean").dl(); val sp = s.getValue("prop").dl()
        assertEquals(sm[3].toInt(), sampleSize(true, sm[0], sm[1], sm[2]).base)
        assertEquals(sp[3].toInt(), sampleSize(false, sp[0], sp[1], sp[2]).base)
        val e = o("epi"); val c4 = e.dl("cells"); val er = epi(c4[0], c4[1], c4[2], c4[3])
        near("rr", e.d("rr"), er.rr); near("or", e.d("or"), er.or); near("rd", e.d("rd"), er.rd)
        near("rrLow", e.d("rrLow"), er.rrLow, 1e-3); near("rrHigh", e.d("rrHigh"), er.rrHigh, 1e-3)
        near("orLow", e.d("orLow"), er.orLow, 1e-3); near("orHigh", e.d("orHigh"), er.orHigh, 1e-3)
    }

    @Test fun inputValidation() {
        assertTrue(runCatching { Parse.numbers("1, two, 3") }.exceptionOrNull() is StatException)
        assertTrue(runCatching { tTest(TType.ONE, listOf(1.0), emptyList()) }.exceptionOrNull() is StatException)
        assertTrue(runCatching { chiIndependence(Parse.matrix("1 2\n3")) }.exceptionOrNull() is StatException)
        assertTrue(runCatching { zScore(1.0, 0.0, 0.0) }.exceptionOrNull() is StatException)
    }
}
