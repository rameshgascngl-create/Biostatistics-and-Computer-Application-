package com.gasc.zoology.biostat.stats

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** Input parsing shared by every calculator. Errors are StatException codes (localised in the UI). */
object Parse {
    const val MAX_FIELD = 20000

    fun numbers(text: String, max: Int = MAX_FIELD): List<Double> {
        val tokens = text.trim().split(Regex("[\\s,;]+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        if (tokens.size > max) throw StatException("tooMany", listOf(max))
        val nums = tokens.map { it.toDoubleOrNull() }
        val bad = tokens.filterIndexed { i, _ -> nums[i] == null || !nums[i]!!.isFinite() }
        if (bad.isNotEmpty()) throw StatException("nonNumeric", listOf(bad.take(3).joinToString(", ")))
        return nums.map { it!! }
    }

    fun list(text: String, min: Int): List<Double> {
        val a = numbers(text)
        if (a.size < min) throw StatException("needObs", listOf(min))
        return a
    }

    fun groups(text: String, minGroups: Int, minSize: Int): List<List<Double>> {
        val g = text.split(';').map { it.trim() }.filter { it.isNotEmpty() }.map { numbers(it) }
            .filter { it.isNotEmpty() }
        if (g.size < minGroups) throw StatException("needGroups", listOf(minGroups))
        if (g.any { it.size < minSize }) throw StatException("needGroupSize", listOf(minSize))
        return g
    }

    fun matrix(text: String): List<List<Double>> {
        val rows = text.trim().split(Regex("\\n|;")).map { it.trim() }.filter { it.isNotEmpty() }
            .map { row -> row.split(Regex("[\\s,]+")).filter { it.isNotEmpty() }.map { it.toDoubleOrNull() ?: Double.NaN } }
        if (rows.size < 2 || rows.any { r -> r.size < 2 || r.any { !it.isFinite() } }) throw StatException("needMatrix")
        val c = rows[0].size
        if (rows.any { it.size != c }) throw StatException("ragged")
        if (rows.size * c > 2500) throw StatException("tooLarge")
        assertCounts(rows.flatten())
        return rows
    }

    fun isCount(x: Double) = x.isFinite() && x == kotlin.math.floor(x) && x >= 0
    fun assertCounts(a: List<Double>) { if (a.any { !isCount(it) }) throw StatException("counts") }
}

fun mean(a: List<Double>) = a.sum() / a.size
fun variance(a: List<Double>, sample: Boolean = true): Double {
    if (a.size < (if (sample) 2 else 1)) return Double.NaN
    val m = mean(a)
    return a.sumOf { (it - m).pow(2) } / (a.size - (if (sample) 1 else 0))
}
fun sd(a: List<Double>, sample: Boolean = true) = sqrt(variance(a, sample))

fun quantileSorted(a: List<Double>, p: Double): Double {
    if (a.isEmpty()) return Double.NaN
    if (a.size == 1) return a[0]
    val h = (a.size - 1) * p
    val lo = kotlin.math.floor(h).toInt(); val hi = ceil(h).toInt()
    return a[lo] + (a[hi] - a[lo]) * (h - lo)
}

data class Descriptive(
    val n: Int, val sum: Double, val mean: Double, val median: Double, val q1: Double, val q3: Double,
    val iqr: Double, val min: Double, val max: Double, val range: Double,
    val sampleVariance: Double, val sampleSD: Double, val popVariance: Double, val popSD: Double,
    val se: Double, val cv: Double, val skew: Double, val modes: List<Double>, val maxF: Int,
    val ciLow: Double, val ciHigh: Double
)

fun descriptive(a: List<Double>): Descriptive {
    if (a.isEmpty()) throw StatException("needObs", listOf(1))
    val x = a.sorted(); val n = x.size
    val sum = x.sum(); val mean = sum / n
    val sv = variance(x, true); val ssd = sqrt(sv)
    val pv = variance(x, false)
    val se = if (n > 1) ssd / sqrt(n.toDouble()) else Double.NaN
    val cv = if (mean > 0 && n > 1) 100 * ssd / mean else Double.NaN
    val freq = x.groupingBy { it }.eachCount()
    val maxF = freq.values.max()
    val modes = if (maxF == 1) emptyList() else freq.filter { it.value == maxF }.keys.toList()
    var skew = Double.NaN
    if (n >= 3 && ssd > 0) skew = n.toDouble() / ((n - 1.0) * (n - 2.0)) * x.sumOf { ((it - mean) / ssd).pow(3) }
    val tc = if (n > 1) Dist.tCritical975(n - 1.0) else Double.NaN
    return Descriptive(n, sum, mean, quantileSorted(x, .5), quantileSorted(x, .25), quantileSorted(x, .75),
        quantileSorted(x, .75) - quantileSorted(x, .25), x.first(), x.last(), x.last() - x.first(),
        sv, ssd, pv, sqrt(pv), se, cv, skew, modes, maxF, mean - tc * se, mean + tc * se)
}

enum class TType { ONE, POOLED, WELCH, PAIRED }

data class TTestResult(
    val type: TType, val n1: Int, val n2: Int, val m1: Double, val m2: Double, val sd1: Double, val sd2: Double,
    val diff: Double, val se: Double, val t: Double, val df: Double, val p: Double,
    val ciLow: Double, val ciHigh: Double, val effect: Double
)

fun tTest(type: TType, a: List<Double>, b: List<Double>, mu0: Double = 0.0): TTestResult {
    when (type) {
        TType.ONE -> {
            if (a.size < 2) throw StatException("oneNeed2")
            val m = mean(a); val s = sd(a); val se = s / sqrt(a.size.toDouble())
            if (se == 0.0) throw StatException("zeroVar")
            val diff = m - mu0; val t = diff / se; val df = a.size - 1.0
            val tc = Dist.tCritical975(df)
            return TTestResult(type, a.size, 0, m, Double.NaN, s, Double.NaN, diff, se, t, df, Dist.tTwoP(t, df),
                diff - tc * se, diff + tc * se, diff / s)
        }
        TType.PAIRED -> {
            if (a.size != b.size) throw StatException("pairedSame")
            if (a.size < 2) throw StatException("pairedNeed2")
            val d = a.indices.map { b[it] - a[it] }
            val m = mean(d); val s = sd(d); val se = s / sqrt(d.size.toDouble())
            if (se == 0.0) throw StatException("pairedIdentical")
            val t = m / se; val df = d.size - 1.0; val tc = Dist.tCritical975(df)
            return TTestResult(type, d.size, d.size, mean(a), mean(b), sd(a), sd(b), m, se, t, df,
                Dist.tTwoP(t, df), m - tc * se, m + tc * se, m / s)
        }
        else -> {
            if (a.size < 2 || b.size < 2) throw StatException("indNeed2")
            val m1 = mean(a); val m2 = mean(b); val v1 = variance(a); val v2 = variance(b)
            val diff = m1 - m2
            val sp = sqrt(((a.size - 1) * v1 + (b.size - 1) * v2) / (a.size + b.size - 2))
            val se: Double; val df: Double
            if (type == TType.POOLED) {
                df = a.size + b.size - 2.0
                se = sp * sqrt(1.0 / a.size + 1.0 / b.size)
            } else {
                se = sqrt(v1 / a.size + v2 / b.size)
                df = (v1 / a.size + v2 / b.size).pow(2) /
                    ((v1 / a.size).pow(2) / (a.size - 1) + (v2 / b.size).pow(2) / (b.size - 1))
            }
            val t = diff / se
            if (!t.isFinite()) throw StatException("tUndefined")
            val tc = Dist.tCritical975(df)
            return TTestResult(type, a.size, b.size, m1, m2, sqrt(v1), sqrt(v2), diff, se, t, df,
                Dist.tTwoP(t, df), diff - tc * se, diff + tc * se, if (sp > 0) diff / sp else Double.NaN)
        }
    }
}

data class ChiGofResult(val obs: List<Double>, val exp: List<Double>, val contrib: List<Double>,
                        val chi: Double, val df: Int, val p: Double, val n: Double)

fun chiGof(obs: List<Double>, expInput: List<Double> = emptyList()): ChiGofResult {
    if (obs.size < 2) throw StatException("gofNeed2")
    Parse.assertCounts(obs)
    val n = obs.sum()
    if (!(n > 0)) throw StatException("obsTotal")
    val exp: List<Double>
    if (expInput.isEmpty()) exp = obs.map { n / obs.size }
    else {
        if (expInput.size != obs.size) throw StatException("expSame")
        val se = expInput.sum()
        if (expInput.any { it <= 0 }) throw StatException("expPos")
        exp = if (abs(se - 1) < .015) expInput.map { it * n } else expInput
        val sx = exp.sum()
        if (abs(sx - n) > max(.01, n * .005)) throw StatException("expSum")
    }
    val contrib = obs.indices.map { (obs[it] - exp[it]).pow(2) / exp[it] }
    val chi = contrib.sum(); val df = obs.size - 1
    return ChiGofResult(obs, exp, contrib, chi, df, Dist.chiSquareP(chi, df.toDouble()), n)
}

data class ChiIndResult(val m: List<List<Double>>, val exp: List<List<Double>>, val chi: Double, val df: Int,
                        val p: Double, val cramerV: Double, val n: Double, val fisher: Double, val fisherSkipped: Boolean)

fun chiIndependence(m: List<List<Double>>): ChiIndResult {
    Parse.assertCounts(m.flatten())
    val r = m.size; val c = m[0].size
    val rt = m.map { it.sum() }
    val ct = DoubleArray(c); m.forEach { row -> row.forEachIndexed { j, x -> ct[j] += x } }
    val n = rt.sum()
    if (!(n > 0) || rt.any { it == 0.0 } || ct.any { it == 0.0 }) throw StatException("margins")
    val exp = m.indices.map { i -> m[i].indices.map { j -> rt[i] * ct[j] / n } }
    var chi = 0.0
    for (i in m.indices) for (j in m[i].indices) chi += (m[i][j] - exp[i][j]).pow(2) / exp[i][j]
    val df = (r - 1) * (c - 1)
    val v = sqrt(chi / (n * min(r - 1, c - 1)))
    val is2 = r == 2 && c == 2
    val fish = if (is2) Dist.fisherExact2x2(m[0][0].toInt(), m[0][1].toInt(), m[1][0].toInt(), m[1][1].toInt()) else Double.NaN
    return ChiIndResult(m, exp, chi, df, Dist.chiSquareP(chi, df.toDouble()), v, n, fish, is2 && fish.isNaN())
}

data class PearsonResult(val n: Int, val mx: Double, val my: Double, val r: Double, val b: Double, val a: Double,
                         val r2: Double, val sse: Double, val rse: Double, val seb: Double, val t: Double,
                         val df: Double, val p: Double, val slopeLow: Double, val slopeHigh: Double)

fun pearson(x: List<Double>, y: List<Double>): PearsonResult {
    if (x.size != y.size) throw StatException("pairSame")
    if (x.size < 3) throw StatException("pairNeed3")
    val n = x.size; val mx = mean(x); val my = mean(y)
    val sxx = x.sumOf { (it - mx).pow(2) }; val syy = y.sumOf { (it - my).pow(2) }
    val sxy = x.indices.sumOf { (x[it] - mx) * (y[it] - my) }
    if (sxx == 0.0 || syy == 0.0) throw StatException("zeroVarXY")
    val r = max(-1.0, min(1.0, sxy / sqrt(sxx * syy)))
    val b = sxy / sxx; val a = my - b * mx
    val sse = y.indices.sumOf { (y[it] - (a + b * x[it])).pow(2) }
    val df = n - 2.0; val rse = sqrt(sse / df); val seb = rse / sqrt(sxx); val t = b / seb
    val p = Dist.tTwoP(t, df); val tc = Dist.tCritical975(df)
    return PearsonResult(n, mx, my, r, b, a, r * r, sse, rse, seb, t, df, p, b - tc * seb, b + tc * seb)
}

fun rankValues(a: List<Double>): List<Double> {
    val order = a.indices.sortedBy { a[it] }
    val r = DoubleArray(a.size)
    var i = 0
    while (i < order.size) {
        var j = i
        while (j + 1 < order.size && a[order[j + 1]] == a[order[i]]) j++
        val rank = (i + j + 2) / 2.0
        for (k in i..j) r[order[k]] = rank
        i = j + 1
    }
    return r.toList()
}

fun spearman(x: List<Double>, y: List<Double>) = pearson(rankValues(x), rankValues(y)).r

data class AnovaResult(val k: Int, val n: Int, val means: List<Double>, val sds: List<Double>, val ns: List<Int>,
                       val grand: Double, val ssb: Double, val ssw: Double, val df1: Int, val df2: Int,
                       val msb: Double, val msw: Double, val f: Double, val p: Double, val eta: Double)

fun anova(g: List<List<Double>>): AnovaResult {
    val k = g.size; val n = g.sumOf { it.size }
    val means = g.map { mean(it) }
    val grand = g.flatten().sum() / n
    val ssb = g.indices.sumOf { g[it].size * (means[it] - grand).pow(2) }
    val ssw = g.indices.sumOf { i -> g[i].sumOf { (it - means[i]).pow(2) } }
    val df1 = k - 1; val df2 = n - k
    val msb = ssb / df1; val msw = ssw / df2
    val f = if (msw == 0.0) (if (msb > 0) Double.POSITIVE_INFINITY else Double.NaN) else msb / msw
    if (f.isNaN()) throw StatException("anovaUndefined")
    val p = if (f == Double.POSITIVE_INFINITY) 0.0 else Dist.fSurvival(f, df1.toDouble(), df2.toDouble())
    val eta = if (ssb + ssw > 0) ssb / (ssb + ssw) else Double.NaN
    return AnovaResult(k, n, means, g.map { sd(it) }, g.map { it.size }, grand, ssb, ssw, df1, df2, msb, msw, f, p, eta)
}

data class EpiResult(val risk1: Double, val risk0: Double, val rd: Double, val rr: Double, val or: Double,
                     val rrLow: Double, val rrHigh: Double, val orLow: Double, val orHigh: Double,
                     val nnt: Double, val corrected: Boolean)

fun epi(a: Double, b: Double, c: Double, d: Double): EpiResult {
    Parse.assertCounts(listOf(a, b, c, d))
    if (a + b == 0.0 || c + d == 0.0) throw StatException("epiGroup")
    val risk1 = a / (a + b); val risk0 = c / (c + d); val rd = risk1 - risk0
    val corrected = listOf(a, b, c, d).any { it == 0.0 }
    val aa = if (corrected) a + .5 else a; val bb = if (corrected) b + .5 else b
    val cc = if (corrected) c + .5 else c; val dd = if (corrected) d + .5 else d
    val rr = (aa / (aa + bb)) / (cc / (cc + dd)); val or = aa * dd / (bb * cc)
    val seRR = sqrt(1 / aa - 1 / (aa + bb) + 1 / cc - 1 / (cc + dd))
    val seOR = sqrt(1 / aa + 1 / bb + 1 / cc + 1 / dd)
    return EpiResult(risk1, risk0, rd, rr, or, exp(ln(rr) - 1.96 * seRR), exp(ln(rr) + 1.96 * seRR),
        exp(ln(or) - 1.96 * seOR), exp(ln(or) + 1.96 * seOR),
        if (rd != 0.0) 1 / abs(rd) else Double.POSITIVE_INFINITY, corrected)
}

data class SampleSizeResult(val isMean: Boolean, val z: Double, val n0: Double, val base: Int, val adjusted: Int,
                            val finiteN: Boolean, val populationN: Double)

fun sampleSize(isMean: Boolean, z: Double, scale: Double, e: Double, drop: Double = 0.0,
               population: Double = Double.NaN): SampleSizeResult {
    if (!(z > 0)) throw StatException("zPos")
    if (!(drop >= 0 && drop < .8)) throw StatException("dropRange")
    val n0: Double
    if (isMean) {
        if (!(scale > 0)) throw StatException("sdPos")
        if (!(e > 0)) throw StatException("marginPos")
        n0 = (z * scale / e).pow(2)
    } else {
        if (!(scale > 0 && scale < 1)) throw StatException("propRange")
        if (!(e > 0 && e < 1)) throw StatException("marginProp")
        n0 = z * z * scale * (1 - scale) / (e * e)
    }
    val finite = population.isFinite()
    if (finite && (population != kotlin.math.floor(population) || population < 2)) throw StatException("popInt")
    val fpc = if (finite) n0 / (1 + (n0 - 1) / population) else n0
    return SampleSizeResult(isMean, z, n0, ceil(fpc).toInt(), ceil(fpc / (1 - drop)).toInt(), finite, population)
}

data class Ranked(val ranks: List<Double>, val tieSum: Double)

fun rankWithTies(vals: List<Double>): Ranked {
    val idx = vals.indices.sortedBy { vals[it] }
    val ranks = DoubleArray(vals.size)
    var tie = 0.0
    var i = 0
    while (i < idx.size) {
        var j = i
        while (j < idx.size && vals[idx[j]] == vals[idx[i]]) j++
        val avg = (i + 1 + j) / 2.0; val t = j - i
        for (k in i until j) ranks[idx[k]] = avg
        if (t > 1) tie += t.toDouble().pow(3) - t
        i = j
    }
    return Ranked(ranks.toList(), tie)
}

private fun subsetRankSumCounts(n: Int, j: Int): DoubleArray {
    val maxS = n * (n + 1) / 2
    val dp = Array(j + 1) { DoubleArray(maxS + 1) }
    dp[0][0] = 1.0
    for (r in 1..n) for (c in min(r, j) downTo 1) {
        val prev = dp[c - 1]; val cur = dp[c]
        for (s in maxS downTo r) if (prev[s - r] != 0.0) cur[s] += prev[s - r]
    }
    return dp[j]
}

enum class PMethod { EXACT, NORMAL }

data class MannWhitneyResult(val u: Double, val u1: Double, val u2: Double, val z: Double, val r1: Double,
                             val n1: Int, val n2: Int, val p: Double, val method: PMethod, val ties: Boolean)

fun mannWhitney(a: List<Double>, b: List<Double>): MannWhitneyResult {
    if (a.isEmpty() || b.isEmpty()) throw StatException("needBoth")
    val n1 = a.size; val n2 = b.size; val n = n1 + n2
    val (ranks, tieSum) = rankWithTies(a + b)
    val r1 = (0 until n1).sumOf { ranks[it] }
    val u1 = r1 - n1 * (n1 + 1) / 2.0; val u2 = n1.toDouble() * n2 - u1
    val u = min(u1, u2); val mu = n1.toDouble() * n2 / 2
    val sigma = sqrt(n1.toDouble() * n2 / 12 * ((n + 1) - tieSum / (n.toDouble() * (n - 1))))
    val z = if (sigma > 0) (u - mu) / sigma else Double.NaN
    val p: Double; val method: PMethod
    if (tieSum == 0.0 && n <= 40) {
        val cnt = subsetRankSumCounts(n, n1)
        val total = cnt.sum(); val rMin = u + n1 * (n1 + 1) / 2.0
        var cum = 0.0
        var s = 0
        while (s <= rMin) { cum += cnt.getOrElse(s) { 0.0 }; s++ }
        p = min(1.0, 2 * cum / total); method = PMethod.EXACT
    } else {
        val zc = if (sigma > 0) (abs(u - mu) - .5) / sigma else Double.NaN
        p = if (zc.isFinite()) min(1.0, 2 * (1 - Dist.normCdf(max(0.0, zc)))) else Double.NaN
        method = PMethod.NORMAL
    }
    return MannWhitneyResult(u, u1, u2, z, r1, n1, n2, p, method, tieSum > 0)
}

data class WilcoxonResult(val w: Double, val wPlus: Double, val wMinus: Double, val n: Int, val p: Double, val method: PMethod)

fun wilcoxon(d: List<Double>): WilcoxonResult {
    if (d.isEmpty()) throw StatException("needDiffs")
    val nz = d.filter { it != 0.0 }; val n = nz.size
    if (n < 1) throw StatException("allZero")
    val (ranks, tieSum) = rankWithTies(nz.map { abs(it) })
    var wp = 0.0; var wm = 0.0
    nz.forEachIndexed { i, x -> if (x > 0) wp += ranks[i] else wm += ranks[i] }
    val w = min(wp, wm)
    val p: Double; val method: PMethod
    if (tieSum == 0.0 && n <= 30) {
        val maxS = n * (n + 1) / 2
        val dp = DoubleArray(maxS + 1); dp[0] = 1.0
        for (r in 1..n) for (s in maxS downTo r) dp[s] += dp[s - r]
        var cum = 0.0
        var s = 0
        while (s <= w) { cum += dp[s]; s++ }
        p = min(1.0, 2 * cum / 2.0.pow(n)); method = PMethod.EXACT
    } else {
        val m = n * (n + 1) / 4.0
        val sdv = sqrt(n * (n + 1.0) * (2 * n + 1) / 24 - tieSum / 48)
        val zc = if (sdv > 0) (abs(w - m) - .5) / sdv else Double.NaN
        p = if (zc.isFinite()) min(1.0, 2 * (1 - Dist.normCdf(max(0.0, zc)))) else Double.NaN
        method = PMethod.NORMAL
    }
    return WilcoxonResult(w, wp, wm, n, p, method)
}

data class CiResult(val mean: Double, val sd: Double, val se: Double, val tcrit: Double, val df: Int,
                    val lower: Double, val upper: Double, val conf: Double)

fun ciMean(data: List<Double>, conf: Double): CiResult {
    if (data.size < 2) throw StatException("needObs", listOf(2))
    if (!(conf > 0 && conf < 1)) throw StatException("confRange")
    val n = data.size; val m = mean(data); val s = sd(data); val se = s / sqrt(n.toDouble())
    val tc = Dist.tQuantile(1 - (1 - conf) / 2, n - 1.0)
    return CiResult(m, s, se, tc, n - 1, m - tc * se, m + tc * se, conf)
}

data class ZResult(val z: Double, val x: Double, val mean: Double, val sd: Double, val upperTail: Double)

fun zScore(x: Double, mean: Double, sd: Double): ZResult {
    if (!(sd > 0)) throw StatException("sdPos")
    val z = (x - mean) / sd
    return ZResult(z, x, mean, sd, 1 - Dist.normCdf(z))
}

data class PowerResult(val power: Double, val n: Int, val delta: Double, val sd: Double, val se: Double,
                       val df: Int, val alpha: Double)

fun powerMean(n: Double, delta: Double, sd: Double, alphaIn: Double?): PowerResult {
    val alpha = alphaIn ?: .05
    if (!(n == kotlin.math.floor(n) && n >= 2)) throw StatException("nInt")
    if (!(sd > 0)) throw StatException("sdPos")
    if (!delta.isFinite()) throw StatException("effectFinite")
    if (!(alpha > 0 && alpha < 1)) throw StatException("alphaRange")
    val df = n - 1; val se = sd / sqrt(n); val ncp = abs(delta) / se
    val tc = Dist.tQuantile(1 - alpha / 2, df)
    val power = 1 - Dist.nctCdf(tc, df, ncp) + Dist.nctCdf(-tc, df, ncp)
    return PowerResult(min(1.0, max(0.0, power)), n.toInt(), delta, sd, se, df.toInt(), alpha)
}

data class QqPoint(val sample: Double, val z: Double, val theoretical: Double)
data class QqResult(val points: List<QqPoint>, val mean: Double, val sd: Double, val n: Int, val r: Double)

fun qqData(data: List<Double>): QqResult {
    if (data.size < 3) throw StatException("needObs", listOf(3))
    val sorted = data.sorted(); val n = sorted.size
    val m = sorted.sum() / n; val s = sqrt(sorted.sumOf { (it - m).pow(2) } / (n - 1))
    if (!(s > 0)) throw StatException("allIdentical")
    val zs = sorted.indices.map { Dist.normInv((it + 1 - .375) / (n + .25)) }
    val mz = zs.sum() / n
    var sxy = 0.0; var szz = 0.0; var sxx = 0.0
    sorted.forEachIndexed { i, x -> sxy += (x - m) * (zs[i] - mz); szz += (zs[i] - mz).pow(2); sxx += (x - m).pow(2) }
    val pts = sorted.mapIndexed { i, x -> QqPoint(x, zs[i], m + s * zs[i]) }
    return QqResult(pts, m, s, n, sxy / sqrt(sxx * szz))
}

data class HwResult(val chi: Double, val n: Double, val exp: List<Double>, val p: Double, val q: Double,
                    val pValue: Double, val lowExpected: Boolean)

fun hardyWeinberg(obs: List<Double>): HwResult {
    if (obs.size != 3) throw StatException("hwThree")
    Parse.assertCounts(obs)
    val n = obs.sum()
    if (!(n > 0)) throw StatException("obsTotal")
    val p = (2 * obs[0] + obs[1]) / (2 * n); val q = 1 - p
    val exp = listOf(p * p * n, 2 * p * q * n, q * q * n)
    if (exp.any { it <= 0 }) throw StatException("hwAbsent")
    val chi = obs.indices.sumOf { (obs[it] - exp[it]).pow(2) / exp[it] }
    return HwResult(chi, n, exp, p, q, Dist.chiSquareP(chi, 1.0), exp.any { it < 5 })
}

data class KruskalResult(val h: Double, val df: Int, val n: Int, val rankSums: List<Double>, val ns: List<Int>, val p: Double)

fun kruskalWallis(groups: List<List<Double>>): KruskalResult {
    if (groups.size < 2) throw StatException("needGroups", listOf(2))
    val combined = groups.flatten()
    val gIdx = groups.flatMapIndexed { gi, g -> g.map { gi } }
    val n = combined.size
    val (ranks, tieSum) = rankWithTies(combined)
    val rs = DoubleArray(groups.size)
    ranks.forEachIndexed { i, r -> rs[gIdx[i]] += r }
    val ni = groups.map { it.size }
    var h = 12.0 / (n * (n + 1.0)) * groups.indices.sumOf { rs[it] * rs[it] / ni[it] } - 3.0 * (n + 1)
    val c = 1 - tieSum / (n.toDouble().pow(3) - n)
    if (!(c > 0)) throw StatException("allIdentical")
    h /= c
    val df = groups.size - 1
    return KruskalResult(h, df, n, rs.toList(), ni, Dist.chiSquareP(h, df.toDouble()))
}

data class TukeyPair(val i: Int, val j: Int, val diff: Double, val q: Double, val se: Double, val p: Double,
                     val hsd: Double, val significant: Boolean)
data class TukeyResult(val pairs: List<TukeyPair>, val msw: Double, val dfw: Int, val means: List<Double>,
                       val qcrit: Double, val alpha: Double, val k: Int)

fun tukeyHsd(groups: List<List<Double>>, alphaIn: Double? = null): TukeyResult {
    val alpha = alphaIn ?: .05
    val k = groups.size
    if (k < 2) throw StatException("needGroups", listOf(2))
    val means = groups.map { mean(it) }; val ni = groups.map { it.size }
    val n = ni.sum()
    val ssw = groups.indices.sumOf { i -> groups[i].sumOf { (it - means[i]).pow(2) } }
    val dfw = n - k
    if (!(dfw > 0)) throw StatException("needMoreObs")
    val msw = ssw / dfw
    if (!(msw > 0)) throw StatException("zeroWithin")
    val qcrit = Dist.studentizedRangeCrit(k, dfw.toDouble(), alpha)
    val pairs = ArrayList<TukeyPair>()
    for (i in 0 until k) for (j in i + 1 until k) {
        val diff = abs(means[i] - means[j])
        val se = sqrt(msw * (1.0 / ni[i] + 1.0 / ni[j]) / 2)
        val q = diff / se
        val p = max(0.0, 1 - Dist.studentizedRangeCdf(q, k, dfw.toDouble()))
        pairs.add(TukeyPair(i, j, diff, q, se, p, qcrit * se, p < alpha))
    }
    return TukeyResult(pairs, msw, dfw, means, qcrit, alpha, k)
}
