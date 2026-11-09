package com.gasc.zoology.biostat.stats

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Error carrying a stable code and arguments so the UI can word it in either language. */
class StatException(val code: String, val args: List<Any> = emptyList()) : Exception(code)

/** Special functions and distributions (Lanczos log-gamma, incomplete beta/gamma, t, chi-square, F, normal). */
object Dist {
    private val LANCZOS = doubleArrayOf(
        676.5203681218851, -1259.1392167224028, 771.32342877765313, -176.6150291621406,
        12.507343278686905, -0.13857109526572012, 9.984369578019571e-6, 1.5056327351493116e-7
    )

    fun logGamma(zIn: Double): Double {
        if (zIn < 0.5) return ln(PI) - ln(sin(PI * zIn)) - logGamma(1 - zIn)
        val z = zIn - 1
        var x = 0.9999999999998099
        for (i in LANCZOS.indices) x += LANCZOS[i] / (z + i + 1)
        val t = z + LANCZOS.size - 0.5
        return 0.5 * ln(2 * PI) + (z + 0.5) * ln(t) - t + ln(x)
    }

    private fun betaCF(a: Double, b: Double, x: Double): Double {
        val maxIt = 200; val eps = 3e-14; val fpmin = 1e-300
        val qab = a + b; val qap = a + 1; val qam = a - 1
        var c = 1.0
        var d = 1 - qab * x / qap
        if (abs(d) < fpmin) d = fpmin
        d = 1 / d
        var h = d
        for (m in 1..maxIt) {
            val m2 = 2 * m
            var aa = m * (b - m) * x / ((qam + m2) * (a + m2))
            d = 1 + aa * d; if (abs(d) < fpmin) d = fpmin
            c = 1 + aa / c; if (abs(c) < fpmin) c = fpmin
            d = 1 / d; h *= d * c
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
            d = 1 + aa * d; if (abs(d) < fpmin) d = fpmin
            c = 1 + aa / c; if (abs(c) < fpmin) c = fpmin
            d = 1 / d
            val del = d * c
            h *= del
            if (abs(del - 1) < eps) break
        }
        return h
    }

    /** Regularised incomplete beta function I_x(a, b). */
    fun regIncBeta(x: Double, a: Double, b: Double): Double {
        if (x <= 0) return 0.0
        if (x >= 1) return 1.0
        val bt = exp(logGamma(a + b) - logGamma(a) - logGamma(b) + a * ln(x) + b * ln(1 - x))
        return if (x < (a + 1) / (a + b + 2)) bt * betaCF(a, b, x) / a
        else 1 - bt * betaCF(b, a, 1 - x) / b
    }

    /** Regularised upper incomplete gamma Q(a, x). */
    fun regGammaQ(a: Double, x: Double): Double {
        if (x < 0 || a <= 0) return Double.NaN
        if (x == 0.0) return 1.0
        val eps = 3e-14; val itMax = 300; val fpmin = 1e-300
        val gln = logGamma(a)
        if (x < a + 1) {
            var ap = a; var sum = 1 / a; var del = sum
            for (n in 1..itMax) {
                ap += 1; del *= x / ap; sum += del
                if (abs(del) < abs(sum) * eps) break
            }
            val p = sum * exp(-x + a * ln(x) - gln)
            return max(0.0, 1 - p)
        }
        var b = x + 1 - a
        var c = 1 / fpmin
        var d = 1 / b
        var h = d
        for (i in 1..itMax) {
            val an = -i * (i - a)
            b += 2
            d = an * d + b; if (abs(d) < fpmin) d = fpmin
            c = b + an / c; if (abs(c) < fpmin) c = fpmin
            d = 1 / d
            val del = d * c
            h *= del
            if (abs(del - 1) < eps) break
        }
        return max(0.0, min(1.0, exp(-x + a * ln(x) - gln) * h))
    }

    fun studentTCdf(t: Double, df: Double): Double {
        if (!(df > 0)) return Double.NaN
        val x = df / (df + t * t)
        val ib = regIncBeta(x, df / 2, 0.5)
        return if (t >= 0) 1 - 0.5 * ib else 0.5 * ib
    }

    fun tTwoP(t: Double, df: Double): Double {
        val c = studentTCdf(t, df)
        val p = 2 * min(c, 1 - c)
        return max(0.0, min(1.0, p))
    }

    fun tQuantile(p: Double, df: Double): Double {
        if (!(p > 0 && p < 1) || !(df > 0)) return Double.NaN
        if (p == 0.5) return 0.0
        if (p < 0.5) return -tQuantile(1 - p, df)
        var lo = 0.0; var hi = 1.0
        while (studentTCdf(hi, df) < p && hi < 1e12) hi *= 2
        repeat(200) {
            val m = (lo + hi) / 2
            if (studentTCdf(m, df) < p) lo = m else hi = m
        }
        return (lo + hi) / 2
    }

    fun tCritical975(df: Double): Double {
        if (!(df > 0)) return Double.NaN
        var lo = 0.0; var hi = 20.0
        repeat(70) {
            val mid = (lo + hi) / 2
            if (studentTCdf(mid, df) < 0.975) lo = mid else hi = mid
        }
        return (lo + hi) / 2
    }

    /** Upper-tail probability of the chi-square distribution. */
    fun chiSquareP(chi: Double, df: Double): Double = regGammaQ(df / 2, chi / 2)

    fun fSurvival(f: Double, d1: Double, d2: Double): Double {
        if (!(f >= 0 && d1 > 0 && d2 > 0)) return Double.NaN
        val x = d1 * f / (d1 * f + d2)
        return max(0.0, min(1.0, 1 - regIncBeta(x, d1 / 2, d2 / 2)))
    }

    fun normCdf(z: Double): Double {
        val q = regGammaQ(0.5, z * z / 2)
        return if (z >= 0) 1 - 0.5 * q else 0.5 * q
    }

    private val A = doubleArrayOf(-39.69683028665376, 220.9460984245205, -275.9285104469687,
        138.357751867269, -30.66479806614716, 2.506628277459239)
    private val B = doubleArrayOf(-54.47609879822406, 161.5858368580409, -155.6989798598866,
        66.80131188771972, -13.28068155288572)
    private val C = doubleArrayOf(-0.007784894002430293, -0.3223964580411365, -2.400758277161838,
        -2.549732539343734, 4.374664141464968, 2.938163982698783)
    private val D = doubleArrayOf(0.007784695709041462, 0.3224671290700398, 2.445134137142996, 3.754408661907416)

    /** Standard normal quantile (Acklam approximation refined by one Halley step). */
    fun normInv(p: Double): Double {
        if (!(p > 0 && p < 1)) throw StatException("probRange")
        val pl = 0.02425
        val x: Double
        if (p < pl) {
            val q = sqrt(-2 * ln(p))
            x = (((((C[0] * q + C[1]) * q + C[2]) * q + C[3]) * q + C[4]) * q + C[5]) /
                ((((D[0] * q + D[1]) * q + D[2]) * q + D[3]) * q + 1)
        } else if (p <= 1 - pl) {
            val q = p - 0.5; val r = q * q
            x = (((((A[0] * r + A[1]) * r + A[2]) * r + A[3]) * r + A[4]) * r + A[5]) * q /
                (((((B[0] * r + B[1]) * r + B[2]) * r + B[3]) * r + B[4]) * r + 1)
        } else {
            val q = sqrt(-2 * ln(1 - p))
            x = -(((((C[0] * q + C[1]) * q + C[2]) * q + C[3]) * q + C[4]) * q + C[5]) /
                ((((D[0] * q + D[1]) * q + D[2]) * q + D[3]) * q + 1)
        }
        val e = normCdf(x) - p
        val u = e * sqrt(2 * PI) * exp(x * x / 2)
        return x - u / (1 + x * u / 2)
    }

    fun logChoose(n: Int, k: Int): Double =
        if (k < 0 || k > n) Double.NEGATIVE_INFINITY
        else logGamma(n + 1.0) - logGamma(k + 1.0) - logGamma(n - k + 1.0)

    /** Two-sided Fisher exact p-value for a 2x2 table; NaN when the support is too large. */
    fun fisherExact2x2(a: Int, b: Int, c: Int, d: Int, maxSupport: Int = 10000): Double {
        val r1 = a + b; val r2 = c + d; val c1 = a + c; val n = r1 + r2
        val minA = max(0, c1 - r2); val maxA = min(r1, c1)
        if (maxA - minA + 1 > maxSupport) return Double.NaN
        val lpObs = logChoose(r1, a) + logChoose(r2, c1 - a) - logChoose(n, c1)
        val pObs = exp(lpObs)
        var p = 0.0
        for (x in minA..maxA) {
            val pr = exp(logChoose(r1, x) + logChoose(r2, c1 - x) - logChoose(n, c1))
            if (pr <= pObs * (1 + 1e-10)) p += pr
        }
        return min(1.0, p)
    }

    /** Non-central t CDF by Simpson integration of the chi density mixture. */
    fun nctCdf(t: Double, df: Double, delta: Double): Double {
        val logC = ln(2.0) - df / 2 * ln(2.0) - logGamma(df / 2)
        val hi = sqrt(df) + 12
        val n = 4000
        val h = hi / n
        var sum = 0.0
        for (i in 0..n) {
            val u = i * h
            val dens = if (u == 0.0) (if (df == 1.0) exp(logC) else 0.0)
            else exp(logC + (df - 1) * ln(u) - u * u / 2)
            val f = normCdf(t * u / sqrt(df) - delta) * dens
            val w = if (i == 0 || i == n) 1 else if (i % 2 == 1) 4 else 2
            sum += w * f
        }
        return min(1.0, max(0.0, sum * h / 3))
    }

    /** Studentized range CDF P(Q <= q) for k means with df error degrees of freedom. */
    fun studentizedRangeCdf(q: Double, k: Int, df: Double): Double {
        if (!(q > 0)) return 0.0
        val z0 = -8.0; val z1 = 8.0; val nz = 160
        val hz = (z1 - z0) / nz
        val zs = DoubleArray(nz + 1) { z0 + it * hz }
        val phi = DoubleArray(nz + 1) { exp(-zs[it] * zs[it] / 2) / sqrt(2 * PI) }
        val bigPhi = DoubleArray(nz + 1) { normCdf(zs[it]) }
        fun w(wv: Double): Double {
            var s = 0.0
            for (i in 0..nz) {
                var d = bigPhi[i] - normCdf(zs[i] - wv)
                if (d < 0) d = 0.0
                val f = phi[i] * d.pow(k - 1)
                val c = if (i == 0 || i == nz) 1 else if (i % 2 == 1) 4 else 2
                s += c * f
            }
            return min(1.0, k * s * hz / 3)
        }
        val lo = if (df < 10) 0.0 else max(0.0, 1 - 10 / sqrt(2 * df))
        val hi = 1 + 10 / sqrt(2 * df) + (if (df < 5) 6 else 0)
        val ns = 200
        val hs = (hi - lo) / ns
        val logC = ln(2.0) + df / 2 * ln(df / 2) - logGamma(df / 2)
        var tot = 0.0
        for (i in 0..ns) {
            val s = lo + i * hs
            val dens = if (s == 0.0) (if (df == 1.0) exp(logC) else 0.0)
            else exp(logC + (df - 1) * ln(s) - df * s * s / 2)
            if (dens == 0.0 && i > 0 && i < ns) continue
            val c = if (i == 0 || i == ns) 1 else if (i % 2 == 1) 4 else 2
            tot += c * dens * w(q * s)
        }
        return min(1.0, max(0.0, tot * hs / 3))
    }

    fun studentizedRangeCrit(k: Int, df: Double, alpha: Double): Double {
        var lo = 0.0; var hi = 10.0
        while (1 - studentizedRangeCdf(hi, k, df) > alpha && hi < 1e3) hi *= 2
        repeat(40) {
            val m = (lo + hi) / 2
            if (1 - studentizedRangeCdf(m, k, df) > alpha) lo = m else hi = m
        }
        return (lo + hi) / 2
    }
}
