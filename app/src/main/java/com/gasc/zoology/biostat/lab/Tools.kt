package com.gasc.zoology.biostat.lab

import com.gasc.zoology.biostat.stats.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max

enum class Kind { MULTI, NUM, CHOICE }
data class Opt(val value: String, val en: String, val ta: String)
data class Input(val key: String, val en: String, val ta: String, val default: String, val kind: Kind = Kind.MULTI,
                 val options: List<Opt> = emptyList(), val hintEn: String = "", val hintTa: String = "",
                 val optional: Boolean = false)

data class Table(val header: List<String>, val rows: List<List<String>>)
sealed interface Plot {
    data class Hist(val edges: List<Double>, val counts: List<Int>) : Plot
    data class Scatter(val xs: List<Double>, val ys: List<Double>, val line: List<Pair<Double, Double>>?,
                       val xLabel: String, val yLabel: String) : Plot
}
data class Out(val metrics: List<Pair<String, String>>, val notes: List<String> = emptyList(),
               val tables: List<Pair<String, Table>> = emptyList(), val plot: Plot? = null)

class ToolDef(val id: String, val titleEn: String, val titleTa: String, val descEn: String, val descTa: String,
              val inputs: List<Input>, val run: (Map<String, String>, Boolean) -> Out) {
    fun defaults() = inputs.associate { it.key to it.default }
}

private fun num(m: Map<String, String>, k: String, label: String): Double =
    m[k]?.trim()?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: throw StatException("badNumber", listOf(label))

private fun optNum(m: Map<String, String>, k: String, label: String): Double? =
    if (m[k].isNullOrBlank()) null else num(m, k, label)

private fun sigWord(p: Double, ta: Boolean, alpha: Double = 0.05) =
    if (p < alpha) (if (ta) "புள்ளியியல் ரீதியாக முக்கியத்துவம் வாய்ந்தது" else "statistically significant")
    else (if (ta) "புள்ளியியல் ரீதியாக முக்கியத்துவம் அற்றது" else "not statistically significant")

private fun decision(p: Double, ta: Boolean) =
    if (ta) "α = 0.05 மட்டத்தில் முடிவு ${sigWord(p, true)} (p = ${pFmt(p)}); பூஜ்யக் கருதுகோளை ${if (p < 0.05) "நிராகரிக்கலாம்" else "நிராகரிக்க இயலாது"}. இது மாதிரிச் சோதனை அனுமானங்களின் கீழான புள்ளியியல் கூற்று மட்டுமே; உயிரியல் முக்கியத்துவத்தைக் குறிக்காது."
    else "At α = 0.05 the result is ${sigWord(p, false)} (p = ${pFmt(p)}); the null hypothesis ${if (p < 0.05) "is rejected" else "cannot be rejected"}. This is an inferential statement under the test assumptions, not a statement of biological importance."

private fun histogram(x: List<Double>): Plot.Hist {
    val s = x.sorted(); val n = s.size
    val k = max(3, minOf(12, ceil(ln(n.toDouble()) / ln(2.0)).toInt() + 1))
    val lo = s.first(); val hi = s.last()
    if (hi == lo) return Plot.Hist(listOf(lo - 0.5, hi + 0.5), listOf(n))
    val w = (hi - lo) / k
    val counts = IntArray(k)
    for (v in s) counts[minOf(k - 1, ((v - lo) / w).toInt())]++
    return Plot.Hist((0..k).map { lo + it * w }, counts.toList())
}

private fun zFor(conf: String): Double = when (conf) { "90" -> Dist.normInv(0.95); "99" -> Dist.normInv(0.995); else -> Dist.normInv(0.975) }

private val confOpts = listOf(Opt("90", "90%", "90%"), Opt("95", "95%", "95%"), Opt("99", "99%", "99%"))

val TOOLS: List<ToolDef> = listOf(
    ToolDef("descriptive", "Descriptive statistics", "விளக்க புள்ளியியல்",
        "Mean, median, mode, spread, skewness, 95% confidence interval and a histogram.",
        "சராசரி, நடுக்கோடி, முகடு, பரவல், கோட்டம், 95% நம்பக இடைவெளி மற்றும் நிகழ்வெண் வரைபடம்.",
        listOf(Input("data", "Observations", "கண்டறிதல்கள்", "12, 15, 14, 10, 18, 15, 13, 16, 15, 11", hintEn = "Separate values by spaces, commas or semicolons", hintTa = "மதிப்புகளை இடைவெளி, காற்புள்ளி அல்லது அரைப்புள்ளியால் பிரிக்கவும்"))
    ) { m, ta ->
        val x = Parse.list(m["data"].orEmpty(), 1); val d = descriptive(x)
        val mode = if (d.modes.isEmpty()) (if (ta) "மீண்டும் வரும் மதிப்பு இல்லை" else "no repeated value") else d.modes.joinToString(", ") { fmt(it) }
        val met = listOf(
            "n" to d.n.toString(), (if (ta) "சராசரி (x̄)" else "Mean") to fmt(d.mean), (if (ta) "நடுக்கோடி" else "Median") to fmt(d.median),
            (if (ta) "முகடு" else "Mode") to mode, (if (ta) "மாதிரி திட்ட விலக்கம் (s)" else "Sample SD") to fmt(d.sampleSD),
            (if (ta) "மாதிரி மாறுபாடு (s²)" else "Sample variance") to fmt(d.sampleVariance),
            (if (ta) "சராசரியின் திட்டப் பிழை" else "SE of mean") to fmt(d.se), (if (ta) "மாறுபாட்டுக் குணகம் (CV)" else "CV") to (if (d.cv.isNaN()) "—" else fmt(d.cv, 3) + "%"),
            (if (ta) "குறைந்தபட்சம்" else "Minimum") to fmt(d.min), (if (ta) "முதல் கால்மானம் (Q1)" else "Q1") to fmt(d.q1), (if (ta) "மூன்றாம் கால்மானம் (Q3)" else "Q3") to fmt(d.q3), (if (ta) "அதிகபட்சம்" else "Maximum") to fmt(d.max),
            (if (ta) "இடைக்கால்மான வீச்சு (IQR)" else "IQR") to fmt(d.iqr), (if (ta) "வீச்சு" else "Range") to fmt(d.range), (if (ta) "கோட்டம்" else "Skewness") to fmt(d.skew),
            (if (ta) "95% நம்பக இடைவெளி" else "95% CI of mean") to if (d.n > 1) "${fmt(d.ciLow)} – ${fmt(d.ciHigh)}" else "—")
        val notes = listOf(
            if (ta) "சராசரி எண்கணித மையத்தையும், நடுக்கோடி 50-ஆவது நூற்றுமானத்தையும், திட்ட விலக்கம் சராசரியைச் சுற்றிய வழக்கமான பரவலையும் தருகிறது. சராசரி ± திட்ட விலக்கத்தை முழுமையான விளக்கமாக ஏற்கும் முன் பரவல் வடிவத்தைப் பரிசீலிக்கவும்."
            else "The mean is the arithmetic centre, the median the 50th percentile and the sample SD the typical spread about the mean. Inspect the distribution before relying on mean ± SD alone.",
            if (d.n < 3) (if (ta) "கோட்டத்திற்குக் குறைந்தது 3 கண்டறிதல்கள் தேவை." else "Skewness needs at least 3 observations.") else "")
        Out(met, notes.filter { it.isNotEmpty() }, plot = histogram(x))
    },

    ToolDef("ttest", "Student's t-test", "t-சோதனை",
        "One-sample, pooled, Welch or paired t-test with a 95% confidence interval for the mean difference.",
        "ஒரு மாதிரி, ஒருங்கிணைந்த மாறுபாடு, வெல்ச் அல்லது இணை t-சோதனை; சராசரி வேறுபாட்டுக்கான 95% நம்பக இடைவெளி.",
        listOf(
            Input("type", "Test design", "சோதனை வடிவமைப்பு", "paired", Kind.CHOICE, listOf(
                Opt("one", "One-sample", "ஒரு மாதிரி"), Opt("welch", "Welch (unequal variance)", "வெல்ச் (சமமற்ற மாறுபாடு)"),
                Opt("pooled", "Pooled variance", "ஒருங்கிணைந்த மாறுபாடு"), Opt("paired", "Paired", "இணை"))),
            Input("mu", "Reference mean μ₀ (one-sample only)", "ஒப்பீட்டுச் சராசரி μ₀ (ஒரு மாதிரிக்கு மட்டும்)", "0", Kind.NUM),
            Input("a", "Group A / before", "குழு A / முன்", "42, 45, 40, 48, 44, 46"),
            Input("b", "Group B / after (not used for one-sample)", "குழு B / பின் (ஒரு மாதிரிக்குத் தேவையில்லை)", "45, 47, 43, 50, 48, 49"))
    ) { m, ta ->
        val type = when (m["type"]) { "one" -> TType.ONE; "welch" -> TType.WELCH; "pooled" -> TType.POOLED; else -> TType.PAIRED }
        val mu = if (type == TType.ONE) num(m, "mu", if (ta) "μ₀" else "μ₀") else 0.0
        val a = Parse.numbers(m["a"].orEmpty())
        val b = if (type == TType.ONE) emptyList() else Parse.numbers(m["b"].orEmpty())
        val r = tTest(type, a, b, mu)
        val label = when (type) {
            TType.ONE -> if (ta) "ஒரு மாதிரி t-சோதனை" else "One-sample t-test"
            TType.WELCH -> if (ta) "வெல்ச் t-சோதனை" else "Welch t-test"
            TType.POOLED -> if (ta) "ஒருங்கிணைந்த மாறுபாடு t-சோதனை" else "Pooled-variance t-test"
            TType.PAIRED -> if (ta) "இணை t-சோதனை" else "Paired t-test"
        }
        val effName = when (type) { TType.PAIRED -> "d_z"; TType.ONE -> "d"; else -> "Cohen d" }
        val met = listOf(
            (if (ta) "சோதனை" else "Test") to label, (if (ta) "சராசரி வேறுபாடு" else "Mean difference") to fmt(r.diff),
            (if (ta) "திட்டப் பிழை" else "Standard error") to fmt(r.se), "t" to fmt(r.t), (if (ta) "சுதந்திர அளவு (df)" else "df") to fmt(r.df, 4),
            (if (ta) "இருபக்க p" else "Two-sided p") to pFmt(r.p), (if (ta) "வேறுபாட்டின் 95% நம்பக இடைவெளி" else "95% CI of difference") to "${fmt(r.ciLow)} – ${fmt(r.ciHigh)}",
            (if (ta) "விளைவு அளவு ($effName)" else "Effect size ($effName)") to fmt(r.effect))
        val ass = when (type) {
            TType.PAIRED -> if (ta) "அனுமானங்கள்: இணை வேறுபாடுகள் தோராயமாக இயல்பு பரவலில் இருக்க வேண்டும்; இணைகள் ஒன்றுக்கொன்று சார்பற்றவை. வேறுபாடு = பின் − முன்." else "Assumptions: paired differences approximately normal; pairs independent. Difference = after − before."
            TType.ONE -> if (ta) "அனுமானங்கள்: கண்டறிதல்கள் சார்பற்றவை; மாதிரி தோராயமாக இயல்பு பரவலில் இருக்க வேண்டும். நம்பக இடைவெளி (x̄ − μ₀)-க்கானது." else "Assumptions: independent observations, approximately normal data. The interval is for (x̄ − μ₀)."
            TType.WELCH -> if (ta) "அனுமானங்கள்: சார்பற்ற குழுக்கள்; தோராயமாக இயல்பு பரவல். மாறுபாடுகள் சமமாக இருக்க வேண்டியதில்லை; சமத்துவம் உறுதியில்லாதபோது வெல்ச் சோதனை பரிந்துரைக்கப்படுகிறது." else "Assumptions: independent groups, approximately normal data. Equal variances are not required; Welch is the safer default when equality is doubtful."
            TType.POOLED -> if (ta) "அனுமானங்கள்: சார்பற்ற குழுக்கள்; தோராயமாக இயல்பு பரவல்; இரு குழுக்களின் மாறுபாடுகள் சமம்." else "Assumptions: independent groups, approximately normal data and equal population variances."
        }
        Out(met, listOf(decision(r.p, ta), ass))
    },

    ToolDef("chigof", "Chi-square goodness of fit", "கை-வர்க்கப் பொருத்தச் சோதனை",
        "Compare observed category counts with expected counts (equal expectation if left blank).",
        "காணப்பட்ட எண்ணிக்கைகளை எதிர்பார்க்கப்படும் எண்ணிக்கைகளுடன் ஒப்பிடுதல் (வெறுமையாக விட்டால் சம எதிர்பார்ப்பு).",
        listOf(Input("obs", "Observed counts", "காணப்பட்ட எண்ணிக்கைகள்", "315, 108, 101, 32"),
            Input("exp", "Expected counts (optional)", "எதிர்பார்க்கப்படும் எண்ணிக்கைகள் (விருப்பம்)", "312.75, 104.25, 104.25, 34.75", optional = true))
    ) { m, ta ->
        val obs = Parse.numbers(m["obs"].orEmpty()); val exp = Parse.numbers(m["exp"].orEmpty())
        val r = chiGof(obs, exp)
        val low = r.exp.any { it < 5 }
        val met = listOf("χ²" to fmt(r.chi), (if (ta) "சுதந்திர அளவு (df)" else "df") to r.df.toString(), "p" to pFmt(r.p), "N" to fmt(r.n))
        val tb = Table(listOf(if (ta) "வகை" else "Category", if (ta) "காணப்பட்டது" else "Observed", if (ta) "எதிர்பார்க்கப்பட்டது" else "Expected", if (ta) "(O−E)²/E" else "(O−E)²/E"),
            r.obs.indices.map { listOf((it + 1).toString(), fmt(r.obs[it]), fmt(r.exp[it]), fmt(r.contrib[it])) })
        val notes = mutableListOf(decision(r.p, ta))
        if (low) notes += if (ta) "எச்சரிக்கை: சில எதிர்பார்க்கப்படும் எண்ணிக்கைகள் 5-க்குக் குறைவு; கை-வர்க்க தோராயம் நம்பகமற்றதாக இருக்கலாம் — வகைகளை இணைக்கவும் அல்லது துல்லிய சோதனையைப் பயன்படுத்தவும்." else "Warning: some expected counts are below 5, so the chi-square approximation may be unreliable — merge categories or use an exact test."
        notes += if (ta) "பொருத்த சோதனையில் df = வகைகள் − 1 (மதிப்பிடப்பட்ட அளபுருக்கள் இருந்தால் அவற்றையும் கழிக்கவும்)." else "For goodness of fit df = categories − 1 (also subtract any parameters estimated from the data)."
        Out(met, notes, listOf((if (ta) "அட்டவணை" else "Table") to tb))
    },

    ToolDef("chiind", "Chi-square test of independence", "கை-வர்க்கச் சார்பின்மைச் சோதனை",
        "Contingency table analysis with expected counts, Cramér's V and, for 2×2 tables, Fisher's exact test.",
        "எதிர்பார்க்கப்படும் எண்ணிக்கைகள், கிராமர் V மற்றும் 2×2 அட்டவணைக்கு ஃபிஷரின் துல்லிய சோதனையுடன் இணைப்பு அட்டவணை பகுப்பாய்வு.",
        listOf(Input("m", "Table (one row per line)", "அட்டவணை (ஒரு வரிசைக்கு ஒரு வரி)", "20 15\n10 25", hintEn = "Rows on separate lines, columns separated by spaces", hintTa = "வரிசைகளைத் தனி வரிகளில், நெடுவரிசைகளை இடைவெளியால் பிரிக்கவும்"))
    ) { m, ta ->
        val r = chiIndependence(Parse.matrix(m["m"].orEmpty()))
        val met = mutableListOf("χ²" to fmt(r.chi), (if (ta) "சுதந்திர அளவு (df)" else "df") to r.df.toString(), "p" to pFmt(r.p),
            (if (ta) "கிராமர் V" else "Cramér's V") to fmt(r.cramerV), "N" to fmt(r.n))
        if (!r.fisherSkipped && !r.fisher.isNaN()) met += (if (ta) "ஃபிஷர் துல்லிய p" else "Fisher exact p") to pFmt(r.fisher)
        val tb = Table(listOf("") + r.exp[0].indices.map { (if (ta) "நெடு. " else "Col ") + (it + 1) },
            r.exp.mapIndexed { i, row -> listOf((if (ta) "வரிசை " else "Row ") + (i + 1)) + row.map { fmt(it, 4) } })
        val notes = mutableListOf(decision(r.p, ta))
        if (r.exp.flatten().any { it < 5 }) notes += if (ta) "எச்சரிக்கை: சில எதிர்பார்க்கப்படும் எண்ணிக்கைகள் 5-க்குக் குறைவு; 2×2 அட்டவணையில் ஃபிஷரின் துல்லிய சோதனையை நம்புங்கள்." else "Warning: some expected counts are below 5; for a 2×2 table rely on Fisher's exact test."
        notes += if (ta) "அனுமானங்கள்: கண்டறிதல்கள் சார்பற்றவை; ஒவ்வொரு பொருளும் ஒரே ஒரு கலத்தில் மட்டும் இடம்பெறும். கிராமர் V விளைவின் வலிமையைக் காட்டுகிறது (0 = தொடர்பின்மை, 1 = முழுத் தொடர்பு)." else "Assumptions: independent observations, each subject in exactly one cell. Cramér's V measures effect strength (0 = none, 1 = perfect)."
        Out(met, notes, listOf((if (ta) "எதிர்பார்க்கப்படும் எண்ணிக்கைகள்" else "Expected counts") to tb))
    },

    ToolDef("corr", "Correlation and regression", "ஒட்டுறவும் பின்னடைவும்",
        "Pearson r, Spearman ρ, least-squares line, slope inference and a scatter plot.",
        "பியர்சன் r, ஸ்பியர்மன் ρ, மீச்சிறு வர்க்கக் கோடு, சாய்வு அனுமானம் மற்றும் சிதறல் வரைபடம்.",
        listOf(Input("x", "X values", "X மதிப்புகள்", "1, 2, 3, 4, 5, 6, 7, 8"), Input("y", "Y values", "Y மதிப்புகள்", "2.1, 3.9, 6.2, 7.8, 10.1, 12.2, 13.8, 16.1"))
    ) { m, ta ->
        val x = Parse.numbers(m["x"].orEmpty()); val y = Parse.numbers(m["y"].orEmpty())
        if (x.size != y.size || x.size < 3) throw StatException("pairNeed3")
        val r = pearson(x, y); val rho = spearman(x, y)
        val met = listOf((if (ta) "பியர்சன் r" else "Pearson r") to fmt(r.r), (if (ta) "ஸ்பியர்மன் ρ" else "Spearman ρ") to fmt(rho), "R²" to fmt(r.r2),
            (if (ta) "சாய்வு b" else "Slope b") to fmt(r.b), (if (ta) "வெட்டுத்துண்டு a" else "Intercept a") to fmt(r.a),
            (if (ta) "சாய்வின் 95% நம்பக இடைவெளி" else "95% CI of slope") to "${fmt(r.slopeLow)} – ${fmt(r.slopeHigh)}",
            (if (ta) "பின்னடைவு p" else "Regression p") to pFmt(r.p), (if (ta) "எச்சத் திட்டப் பிழை" else "Residual SE") to fmt(r.rse))
        val eq = (if (ta) "பின்னடைவுச் சமன்பாடு: " else "Regression equation: ") + "Ŷ = ${fmt(r.a)} ${if (r.b >= 0) "+" else "−"} ${fmt(abs(r.b))}X"
        val x0 = x.min(); val x1 = x.max()
        Out(met, listOf(eq,
            if (ta) "r நேரியல் தொடர்பின் திசையையும் வலிமையையும் விவரிக்கிறது; R² = ${fmt(100 * r.r2, 3)}% என்பது இம்மாதிரியில் Y மாறுபாட்டில் இந்த நேர்கோட்டு மாதிரி விளக்கும் பங்கு. ஒட்டுறவு மட்டும் காரணத் தொடர்பை நிரூபிக்காது."
            else "r describes direction and strength of linear association; R² = ${fmt(100 * r.r2, 3)}% is the share of Y variability explained by this line in this sample. Correlation alone does not demonstrate causation.",
            if (ta) "அனுமானங்கள்: நேரியல் உறவு, சார்பற்ற கண்டறிதல்கள், ஏறக்குறைய மாறா எச்ச மாறுபாடு, சிறிய மாதிரிகளுக்குத் தோராயமாக இயல்பான எச்சங்கள். X வீச்சுக்கு வெளியே முன்கணிப்பு புறநிரப்பலாகும்."
            else "Assumptions: linear relationship, independent observations, roughly constant residual variance and approximately normal residuals for small samples. Predicting outside the observed X range is extrapolation."),
            plot = Plot.Scatter(x, y, listOf(x0 to r.a + r.b * x0, x1 to r.a + r.b * x1), "X", "Y"))
    },

    ToolDef("anova", "One-way ANOVA", "ஒரு வழி ANOVA",
        "Compare three or more group means with F, p and eta-squared.",
        "மூன்று அல்லது அதற்கு மேற்பட்ட குழுச் சராசரிகளை F, p மற்றும் ஈட்டா-வர்க்கத்துடன் ஒப்பிடுதல்.",
        listOf(Input("g", "Groups (separate groups with ;)", "குழுக்கள் (குழுக்களை ; கொண்டு பிரிக்கவும்)", "12, 15, 14, 11, 13; 14, 17, 15, 13, 16, 15; 16, 19, 15, 17, 18"))
    ) { m, ta ->
        val g = Parse.groups(m["g"].orEmpty(), 2, 2); val r = anova(g)
        val met = listOf("F" to fmt(r.f), "df" to "${r.df1}, ${r.df2}", "p" to pFmt(r.p), (if (ta) "ஈட்டா-வர்க்கம் η²" else "Eta-squared η²") to fmt(r.eta))
        val ssT = r.ssb + r.ssw
        val tb = Table(listOf(if (ta) "மூலம்" else "Source", "SS", "df", "MS", "F"), listOf(
            listOf(if (ta) "குழுக்களுக்கிடையே" else "Between", fmt(r.ssb), r.df1.toString(), fmt(r.msb), fmt(r.f)),
            listOf(if (ta) "குழுக்களுக்குள்" else "Within", fmt(r.ssw), r.df2.toString(), fmt(r.msw), ""),
            listOf(if (ta) "மொத்தம்" else "Total", fmt(ssT), (r.df1 + r.df2).toString(), "", "")))
        val gt = Table(listOf(if (ta) "குழு" else "Group", "n", if (ta) "சராசரி" else "Mean", "SD"), r.means.indices.map { listOf((it + 1).toString(), r.ns[it].toString(), fmt(r.means[it]), fmt(r.sds[it])) })
        val notes = mutableListOf(decision(r.p, ta))
        val sdv = r.sds.filter { it > 0 }
        if (sdv.size > 1 && sdv.max() / sdv.min() > 2) notes += if (ta) "எச்சரிக்கை: குழுத் திட்ட விலக்கங்களின் விகிதம் 2-ஐ மீறுகிறது; மாறுபாடு சமத்துவ அனுமானம் ஐயத்திற்குரியது (கிரஸ்கல்–வாலிஸ் சோதனையைப் பரிசீலிக்கவும்)." else "Warning: the ratio of largest to smallest group SD exceeds 2, so equal variances are doubtful (consider Kruskal–Wallis)."
        notes += if (ta) "அனுமானங்கள்: சார்பற்ற கண்டறிதல்கள், குழுக்களுக்குள் தோராயமாக இயல்பு பரவல், சம மாறுபாடு. குறிப்பிடத்தக்க F குறைந்தது ஒரு குழுச் சராசரி வேறுபடுவதைக் காட்டுகிறது; எந்தக் குழுக்கள் என்பதற்கு Tukey HSD பயன்படுத்தவும்." else "Assumptions: independent observations, approximately normal within groups, equal variances. A significant F says at least one mean differs; use Tukey HSD to see which."
        Out(met, notes, listOf((if (ta) "ANOVA அட்டவணை" else "ANOVA table") to tb, (if (ta) "குழுச் சுருக்கம்" else "Group summary") to gt))
    },

    ToolDef("tukey", "Tukey HSD post-hoc test", "டூக்கி HSD பிந்தைய சோதனை",
        "All pairwise comparisons after ANOVA using the studentized range distribution.",
        "ANOVA-விற்குப் பின் ஸ்டூடண்டைஸ்டு வீச்சுப் பரவலைப் பயன்படுத்தி அனைத்து இணை ஒப்பீடுகள்.",
        listOf(Input("g", "Groups (separate groups with ;)", "குழுக்கள் (குழுக்களை ; கொண்டு பிரிக்கவும்)", "12, 15, 14, 11, 13; 14, 17, 15, 13, 16, 15; 16, 19, 15, 17, 18"),
            Input("alpha", "Significance level α", "முக்கியத்துவ நிலை α", "0.05", Kind.CHOICE, listOf(Opt("0.10", "0.10", "0.10"), Opt("0.05", "0.05", "0.05"), Opt("0.01", "0.01", "0.01"))))
    ) { m, ta ->
        val g = Parse.groups(m["g"].orEmpty(), 2, 2); val a = m["alpha"]?.toDoubleOrNull() ?: 0.05
        val r = tukeyHsd(g, a)
        val met = listOf("MSW" to fmt(r.msw), (if (ta) "குழுவிற்குள் df" else "df within") to r.dfw.toString(), (if (ta) "q மாறுநிலை" else "q critical") to fmt(r.qcrit), "α" to fmt(r.alpha))
        val tb = Table(listOf(if (ta) "ஒப்பீடு" else "Pair", if (ta) "சராசரி வேறுபாடு" else "Mean diff", "q", "p", "HSD", if (ta) "முடிவு" else "Result"),
            r.pairs.map { listOf("${it.i + 1} vs ${it.j + 1}", fmt(it.diff), fmt(it.q), pFmt(it.p), fmt(it.hsd), if (it.significant) (if (ta) "முக்கியத்துவம் உண்டு" else "significant") else (if (ta) "இல்லை" else "ns")) })
        Out(met, listOf(if (ta) "p மதிப்புகள் குடும்பம்-வாரியான பிழை விகிதத்தைக் கட்டுப்படுத்துகின்றன. அனுமானங்கள் ANOVA-வைப் போன்றவை; குழு அளவுகள் சமமற்றிருந்தால் Tukey–Kramer சரிசெய்தல் பயன்படுத்தப்படுகிறது." else "The p-values control the family-wise error rate. Assumptions are those of ANOVA; unequal group sizes are handled by the Tukey–Kramer adjustment."),
            listOf((if (ta) "இணை ஒப்பீடுகள்" else "Pairwise comparisons") to tb))
    },

    ToolDef("epi", "2×2 risk, odds and effect measures", "2×2 இடர், முரண்பாட்டு விகிதம்",
        "Risk difference, risk ratio, odds ratio with 95% confidence intervals and NNT.",
        "இடர் வேறுபாடு, இடர் விகிதம், முரண்பாட்டு விகிதம் (95% நம்பக இடைவெளியுடன்) மற்றும் NNT.",
        listOf(Input("a", "Exposed with outcome (a)", "வெளிப்பாடு + விளைவு (a)", "30", Kind.NUM), Input("b", "Exposed without outcome (b)", "வெளிப்பாடு, விளைவு இல்லை (b)", "70", Kind.NUM),
            Input("c", "Unexposed with outcome (c)", "வெளிப்பாடு இல்லை, விளைவு உண்டு (c)", "15", Kind.NUM), Input("d", "Unexposed without outcome (d)", "வெளிப்பாடு இல்லை, விளைவு இல்லை (d)", "85", Kind.NUM))
    ) { m, ta ->
        val v = listOf("a" to "a", "b" to "b", "c" to "c", "d" to "d").map { num(m, it.first, it.second) }
        Parse.assertCounts(v)
        val r = epi(v[0], v[1], v[2], v[3])
        val met = listOf((if (ta) "வெளிப்பட்டோரின் இடர்" else "Risk, exposed") to fmt(100 * r.risk1, 4) + "%", (if (ta) "வெளிப்படாதோரின் இடர்" else "Risk, unexposed") to fmt(100 * r.risk0, 4) + "%",
            (if (ta) "இடர் வேறுபாடு" else "Risk difference") to fmt(100 * r.rd, 4) + (if (ta) " சதவீதப் புள்ளி" else " pp"),
            (if (ta) "இடர் விகிதம் (RR)" else "Risk ratio (RR)") to fmt(r.rr), (if (ta) "RR-இன் 95% நம்பக இடைவெளி" else "95% CI of RR") to "${fmt(r.rrLow)} – ${fmt(r.rrHigh)}",
            (if (ta) "முரண்பாட்டு விகிதம் (OR)" else "Odds ratio (OR)") to fmt(r.or), (if (ta) "OR-இன் 95% நம்பக இடைவெளி" else "95% CI of OR") to "${fmt(r.orLow)} – ${fmt(r.orHigh)}",
            "NNT/NNH" to if (r.nnt.isFinite()) ceil(r.nnt).toInt().toString() else "∞")
        val notes = mutableListOf<String>()
        if (r.corrected) notes += if (ta) "ஒரு கலம் பூஜ்யம் என்பதால் 0.5 தொடர்ச்சித் திருத்தம் செய்யப்பட்டது; கவனத்துடன் விளக்கவும்." else "A 0.5 continuity correction was applied because a cell was zero; interpret cautiously."
        val ci1 = r.rrLow <= 1 && r.rrHigh >= 1
        notes += if (ta) "RR இடர்களை ஒப்பிடுகிறது; OR முரண்பாட்டை ஒப்பிடுகிறது; இடர் வேறுபாடு தனி மாற்றத்தைத் தருகிறது. RR-இன் நம்பக இடைவெளி 1-ஐ ${if (ci1) "உள்ளடக்குகிறது — தொடர்பு உறுதியில்லை" else "உள்ளடக்கவில்லை — தொடர்புக்குச் சான்று உள்ளது"}. வடிவமைப்புக்கேற்ப அளவீட்டைத் தேர்ந்தெடுக்கவும் (பின்னோக்கு வழக்கு-கட்டுப்பாட்டு ஆய்வுகளில் OR மட்டுமே பொருந்தும்)."
        else "RR compares risks, OR compares odds and the risk difference gives the absolute change. The RR interval ${if (ci1) "includes 1 — the association is uncertain" else "excludes 1 — there is evidence of association"}. Choose the measure to suit the design (in case-control studies only the OR is valid)."
        Out(met, notes)
    },

    ToolDef("samplesize", "Sample-size planner", "மாதிரி அளவுத் திட்டமிடல்",
        "Sample size to estimate a mean or a proportion with a stated margin of error, drop-out and finite-population adjustment.",
        "கூறிய பிழை எல்லையுடன் சராசரி அல்லது விகிதத்தை மதிப்பிடத் தேவையான மாதிரி அளவு; விடுபடுதல், முடிவுறு மக்கள்தொகைத் திருத்தம்.",
        listOf(Input("kind", "Estimate", "மதிப்பிடுவது", "mean", Kind.CHOICE, listOf(Opt("mean", "Mean", "சராசரி"), Opt("prop", "Proportion", "விகிதம்"))),
            Input("conf", "Confidence level", "நம்பக மட்டம்", "95", Kind.CHOICE, confOpts),
            Input("scale", "SD (mean) or expected proportion p (0–1)", "திட்ட விலக்கம் (சராசரி) அல்லது எதிர்பார்க்கும் விகிதம் p (0–1)", "10", Kind.NUM),
            Input("e", "Margin of error", "பிழை எல்லை", "2", Kind.NUM),
            Input("drop", "Expected drop-out (%)", "எதிர்பார்க்கும் விடுபடுதல் (%)", "10", Kind.NUM),
            Input("pop", "Population size (optional)", "மக்கள்தொகை அளவு (விருப்பம்)", "", Kind.NUM, optional = true))
    ) { m, ta ->
        val isMean = m["kind"] != "prop"
        val pop = optNum(m, "pop", if (ta) "மக்கள்தொகை" else "Population") ?: Double.NaN
        val r = sampleSize(isMean, zFor(m["conf"] ?: "95"), num(m, "scale", if (ta) "திட்ட விலக்கம்/விகிதம்" else "SD / p"), num(m, "e", if (ta) "பிழை எல்லை" else "Margin of error"),
            num(m, "drop", if (ta) "விடுபடுதல்" else "Drop-out") / 100, pop)
        val met = listOf("Z" to fmt(r.z), (if (ta) "முதல்நிலை n₀" else "Initial n₀") to fmt(r.n0, 5), (if (ta) "தேவையான மாதிரி அளவு" else "Required n") to r.base.toString(),
            (if (ta) "விடுபடுதலுக்குப் பின் n" else "n after drop-out") to r.adjusted.toString())
        Out(met, listOf(if (ta) "சூத்திரம்: ${if (isMean) "n₀ = (Z·σ / E)²" else "n₀ = Z²·p(1−p) / E²"}. முடிவுகள் எப்போதும் மேல்நோக்கி முழு எண்ணுக்கு உயர்த்தப்படுகின்றன. σ அல்லது p ஒரு முன்னோடி ஆய்வு அல்லது இலக்கியத்திலிருந்து பெறப்பட வேண்டும்; தெரியாதபோது p = 0.5 மிகப் பழமைவாத தேர்வு."
        else "Formula: ${if (isMean) "n₀ = (Z·σ / E)²" else "n₀ = Z²·p(1−p) / E²"}. Results are always rounded up. σ or p should come from a pilot study or the literature; when unknown, p = 0.5 is the most conservative choice."))
    },

    ToolDef("mw", "Mann–Whitney U test", "மான்–விட்னி U சோதனை",
        "Rank-based comparison of two independent groups (exact p for small tie-free samples).",
        "இரு சார்பற்ற குழுக்களின் தரவரிசை அடிப்படையிலான ஒப்பீடு (சிறிய, இணைமதிப்பற்ற மாதிரிகளுக்குத் துல்லிய p).",
        listOf(Input("a", "Group A", "குழு A", "12, 15, 14, 11, 13"), Input("b", "Group B", "குழு B", "16, 19, 15, 17, 18, 20"))
    ) { m, ta ->
        val r = mannWhitney(Parse.numbers(m["a"].orEmpty()), Parse.numbers(m["b"].orEmpty()))
        val exact = r.method == PMethod.EXACT
        val met = listOf("U" to fmt(r.u), "U₁ / U₂" to "${fmt(r.u1)} / ${fmt(r.u2)}", (if (ta) "z (தோராயம்)" else "z (approx.)") to fmt(r.z), (if (ta) "இருபக்க p" else "Two-sided p") to pFmt(r.p),
            (if (ta) "p முறை" else "p method") to if (exact) (if (ta) "துல்லியம்" else "exact") else (if (ta) "இயல்புத் தோராயம்" else "normal approximation"), "n₁, n₂" to "${r.n1}, ${r.n2}")
        Out(met, listOf(decision(r.p, ta), if (ta) "அனுமானங்கள்: சார்பற்ற கண்டறிதல்கள்; வடிவம் ஒத்திருந்தால் இது நடுக்கோடிகளை ஒப்பிடுகிறது. இயல்புப் பரவல் தேவையில்லை." else "Assumptions: independent observations; if the distribution shapes are similar it compares medians. No normality is required.",
            if (r.ties) (if (ta) "இணைமதிப்புகள் உள்ளதால் இயல்புத் தோராயம் (இணைத் திருத்தத்துடன்) பயன்படுத்தப்பட்டது." else "Ties are present, so the normal approximation with a tie correction was used.") else ""))
            .let { it.copy(notes = it.notes.filter { n -> n.isNotEmpty() }) }
    },

    ToolDef("wilcoxon", "Wilcoxon signed-rank test", "வில்காக்ஸன் குறியிட்ட தரநிலைச் சோதனை",
        "Rank-based test for paired samples (exact p for small tie-free samples).",
        "இணை மாதிரிகளுக்கான தரவரிசை அடிப்படைச் சோதனை (சிறிய, இணைமதிப்பற்ற மாதிரிகளுக்குத் துல்லிய p).",
        listOf(Input("a", "Before", "முன்", "42, 45, 40, 48, 44, 46, 41, 47"), Input("b", "After", "பின்", "46, 49, 42, 53, 47, 50, 40, 52"))
    ) { m, ta ->
        val a = Parse.numbers(m["a"].orEmpty()); val b = Parse.numbers(m["b"].orEmpty())
        if (a.size != b.size) throw StatException("pairedSame")
        val r = wilcoxon(a.indices.map { b[it] - a[it] })
        val met = listOf("W" to fmt(r.w), "W⁺ / W⁻" to "${fmt(r.wPlus)} / ${fmt(r.wMinus)}", (if (ta) "பூஜ்யமற்ற இணைகள்" else "Non-zero pairs") to r.n.toString(),
            (if (ta) "இருபக்க p" else "Two-sided p") to pFmt(r.p),
            (if (ta) "p முறை" else "p method") to if (r.method == PMethod.EXACT) (if (ta) "துல்லியம்" else "exact") else (if (ta) "இயல்புத் தோராயம்" else "normal approximation"))
        Out(met, listOf(decision(r.p, ta), if (ta) "அனுமானங்கள்: இணைகள் சார்பற்றவை; வேறுபாடுகளின் பரவல் சமச்சீராக இருக்க வேண்டும். பூஜ்ய வேறுபாடுகள் நீக்கப்படுகின்றன. வேறுபாடு = பின் − முன்." else "Assumptions: independent pairs and a roughly symmetric distribution of differences. Zero differences are dropped. Difference = after − before."))
    },

    ToolDef("ci", "Confidence interval for a mean", "சராசரிக்கான நம்பக இடைவெளி",
        "t-based confidence interval at 90%, 95% or 99%.",
        "90%, 95% அல்லது 99% மட்டத்தில் t அடிப்படையிலான நம்பக இடைவெளி.",
        listOf(Input("data", "Observations", "கண்டறிதல்கள்", "18.2, 17.5, 19.1, 16.8, 18.9, 17.7, 18.4, 19.0"), Input("conf", "Confidence level", "நம்பக மட்டம்", "95", Kind.CHOICE, confOpts))
    ) { m, ta ->
        val c = (m["conf"]?.toDoubleOrNull() ?: 95.0) / 100
        val r = ciMean(Parse.list(m["data"].orEmpty(), 2), c)
        Out(listOf((if (ta) "சராசரி" else "Mean") to fmt(r.mean), (if (ta) "திட்டப் பிழை" else "SE") to fmt(r.se), (if (ta) "t மாறுநிலை" else "t critical") to fmt(r.tcrit), "df" to r.df.toString(),
            (if (ta) "கீழ் எல்லை" else "Lower") to fmt(r.lower), (if (ta) "மேல் எல்லை" else "Upper") to fmt(r.upper)),
            listOf(if (ta) "சரியான விளக்கம்: இதே முறையைத் திரும்பத் திரும்பப் பயன்படுத்தினால் ${m["conf"]}% இடைவெளிகள் உண்மையான சராசரியைப் பிடிக்கும். குறிப்பிட்ட இடைவெளியில் உண்மையான சராசரி இருப்பதற்கான நிகழ்தகவு ${m["conf"]}% என்பது தவறான விளக்கம்."
            else "Correct interpretation: if the procedure were repeated many times, ${m["conf"]}% of such intervals would contain the true mean. It is not the probability that this particular interval contains it."))
    },

    ToolDef("z", "Z-score and normal tail", "Z-மதிப்பும் இயல்பு வால் நிகழ்தகவும்",
        "Standardise a value and obtain the one- and two-sided tail probabilities of the normal distribution.",
        "ஒரு மதிப்பைத் தரப்படுத்தி இயல்புப் பரவலின் ஒரு-பக்க, இரு-பக்க வால் நிகழ்தகவுகளைப் பெறுதல்.",
        listOf(Input("x", "Value x", "மதிப்பு x", "75", Kind.NUM), Input("mean", "Mean μ", "சராசரி μ", "65", Kind.NUM), Input("sd", "Standard deviation σ", "திட்ட விலக்கம் σ", "8", Kind.NUM))
    ) { m, ta ->
        val r = zScore(num(m, "x", "x"), num(m, "mean", "μ"), num(m, "sd", "σ"))
        val lower = 1 - r.upperTail
        Out(listOf("z" to fmt(r.z), (if (ta) "கீழ்-வால் P(Z ≤ z)" else "Lower tail P(Z ≤ z)") to fmt(lower, 5), (if (ta) "மேல்-வால் P(Z ≥ z)" else "Upper tail P(Z ≥ z)") to fmt(r.upperTail, 5),
            (if (ta) "இரு-வால் p" else "Two-tailed p") to fmt(2 * minOf(lower, r.upperTail), 5)),
            listOf(if (ta) "இயல்புப் பரவலில் |z| > 1.96 என்பது ஏறக்குறைய 5% இரு-வால் பகுதிக்குச் சமம். இந்த மதிப்புகள் தரவு தோராயமாக இயல்பாகப் பரவியுள்ளது என்ற அனுமானத்தில் மட்டுமே பொருந்தும்." else "In a normal distribution |z| > 1.96 corresponds to about 5% in both tails combined. These probabilities assume the variable is approximately normally distributed."))
    },

    ToolDef("power", "Power of a t-test", "t-சோதனையின் ஆற்றல்",
        "Power of a two-sided one-sample or paired t-test, computed from the non-central t distribution.",
        "மைய-அற்ற t பரவலிலிருந்து கணக்கிடப்படும் இருபக்க ஒரு-மாதிரி/இணை t-சோதனையின் ஆற்றல்.",
        listOf(Input("n", "Sample size n", "மாதிரி அளவு n", "20", Kind.NUM), Input("delta", "True difference to detect", "கண்டறிய வேண்டிய உண்மை வேறுபாடு", "3", Kind.NUM),
            Input("sd", "Standard deviation", "திட்ட விலக்கம்", "5", Kind.NUM), Input("alpha", "Significance level α", "முக்கியத்துவ நிலை α", "0.05", Kind.CHOICE, listOf(Opt("0.10", "0.10", "0.10"), Opt("0.05", "0.05", "0.05"), Opt("0.01", "0.01", "0.01"))))
    ) { m, ta ->
        val r = powerMean(num(m, "n", "n"), num(m, "delta", if (ta) "வேறுபாடு" else "Difference"), num(m, "sd", "SD"), m["alpha"]?.toDoubleOrNull())
        Out(listOf((if (ta) "ஆற்றல் (1 − β)" else "Power (1 − β)") to fmt(r.power, 4), (if (ta) "தரப்படுத்திய விளைவு" else "Standardised effect") to fmt(abs(r.delta) / r.sd), (if (ta) "திட்டப் பிழை" else "SE") to fmt(r.se), "df" to r.df.toString()),
            listOf(if (r.power < 0.8) (if (ta) "ஆற்றல் மரபு இலக்கான 0.80-ஐ விடக் குறைவு; உண்மை விளைவு இருந்தாலும் அதைக் கண்டறியாமல் போக வாய்ப்பு அதிகம். மாதிரி அளவை அதிகரிக்கவும்." else "Power is below the conventional 0.80 target, so a true effect of this size is likely to be missed. Increase the sample size.")
            else (if (ta) "ஆற்றல் மரபு இலக்கான 0.80-ஐ எட்டுகிறது." else "Power meets the conventional 0.80 target."),
            if (ta) "சோதனைக்கு முன்பே (a priori) ஆற்றலைக் கணக்கிடுக; முடிவுக்குப் பிந்தைய ‘கண்டறியப்பட்ட ஆற்றல்’ கூடுதல் தகவல் தராது." else "Compute power before the study (a priori); post-hoc ‘observed power’ adds no information beyond the p-value."))
    },

    ToolDef("qq", "Normal Q–Q plot", "இயல்பு Q–Q வரைபடம்",
        "Visual and numerical check of normality: sample quantiles against theoretical normal quantiles.",
        "இயல்புத்தன்மையின் காட்சி, எண் சோதனை: மாதிரி மதிப்புகளை இயல்புக் கோட்பாட்டு மதிப்புகளுடன் ஒப்பிடுதல்.",
        listOf(Input("data", "Observations", "கண்டறிதல்கள்", "12, 15, 14, 10, 18, 15, 13, 16, 15, 11, 17, 14"))
    ) { m, ta ->
        val q = qqData(Parse.list(m["data"].orEmpty(), 3))
        val xs = q.points.map { it.z }; val ys = q.points.map { it.sample }
        Out(listOf("n" to q.n.toString(), (if (ta) "Q–Q ஒட்டுறவு r" else "Q–Q correlation r") to fmt(q.r, 5), (if (ta) "சராசரி" else "Mean") to fmt(q.mean), (if (ta) "திட்ட விலக்கம்" else "SD") to fmt(q.sd)),
            listOf(if (ta) "புள்ளிகள் நேர்கோட்டை ஒட்டியிருந்தால் இயல்புத்தன்மை நியாயமானது; வளைவு கோட்டத்தையும், S-வடிவம் கனமான/இலேசான வால்களையும் காட்டும். சிறிய மாதிரிகளில் வரைபடம் மிகவும் மாறுபடும்; r மட்டும் முறையான இயல்புச் சோதனை அல்ல." else "Points close to the line support normality; curvature suggests skewness and an S-shape suggests heavy or light tails. Plots from small samples are very variable, and r alone is not a formal normality test."),
            plot = Plot.Scatter(xs, ys, listOf(xs.first() to q.mean + q.sd * xs.first(), xs.last() to q.mean + q.sd * xs.last()), if (ta) "கோட்பாட்டு z" else "Theoretical z", if (ta) "மாதிரி மதிப்பு" else "Sample value"))
    },

    ToolDef("hw", "Hardy–Weinberg equilibrium", "ஹார்டி–வெயின்பர்க் சமநிலை",
        "Allele frequencies and a 1-df chi-square test of genotype counts.",
        "அல்லீல் அதிர்வெண்கள் மற்றும் மரபணுவகை எண்ணிக்கைகளுக்கான 1-df கை-வர்க்கச் சோதனை.",
        listOf(Input("aa1", "AA count", "AA எண்ணிக்கை", "298", Kind.NUM), Input("aa2", "Aa count", "Aa எண்ணிக்கை", "489", Kind.NUM), Input("aa3", "aa count", "aa எண்ணிக்கை", "213", Kind.NUM))
    ) { m, ta ->
        val r = hardyWeinberg(listOf(num(m, "aa1", "AA"), num(m, "aa2", "Aa"), num(m, "aa3", "aa")))
        val tb = Table(listOf(if (ta) "மரபணுவகை" else "Genotype", if (ta) "எதிர்பார்க்கப்பட்டது" else "Expected"), listOf("AA", "Aa", "aa").mapIndexed { i, g -> listOf(g, fmt(r.exp[i])) })
        val notes = mutableListOf(if (ta) "தலைமுறைகளிடையே அதிர்வெண் மாறாமல், சமநிலை நிலையில் p² + 2pq + q² = 1. p-மதிப்பு ${if (r.pValue < 0.05) "குறைவு: மரபணுவகை எண்ணிக்கைகள் சமநிலையிலிருந்து குறிப்பிடத்தக்க அளவு விலகுகின்றன (உம்: இனச்சேர்க்கை விருப்பம், தேர்வு, இடம்பெயர்வு, சிறிய மக்கள்தொகை, மரபணுவகை பிழை)" else "அதிகம்: சமநிலையிலிருந்து குறிப்பிடத்தக்க விலகல் இல்லை"}." else "At equilibrium genotype frequencies are p², 2pq and q². ${if (r.pValue < 0.05) "The small p-value indicates a significant departure from equilibrium (e.g. non-random mating, selection, migration, small population or genotyping error)." else "The large p-value shows no significant departure from equilibrium."}")
        if (r.lowExpected) notes += if (ta) "எச்சரிக்கை: சில எதிர்பார்க்கப்படும் எண்ணிக்கைகள் 5-க்குக் குறைவு; துல்லிய சோதனையைப் பரிசீலிக்கவும்." else "Warning: some expected counts are below 5; consider an exact test."
        Out(listOf((if (ta) "அல்லீல் அதிர்வெண் p (A)" else "Allele frequency p (A)") to fmt(r.p), (if (ta) "அல்லீல் அதிர்வெண் q (a)" else "Allele frequency q (a)") to fmt(r.q), "χ²" to fmt(r.chi), "df" to "1", "p" to pFmt(r.pValue), "N" to fmt(r.n)),
            notes, listOf((if (ta) "எதிர்பார்க்கப்படும் மரபணுவகை எண்ணிக்கை" else "Expected genotype counts") to tb))
    },

    ToolDef("kw", "Kruskal–Wallis H test", "கிரஸ்கல்–வாலிஸ் H சோதனை",
        "Rank-based alternative to one-way ANOVA for three or more groups.",
        "மூன்று அல்லது அதற்கு மேற்பட்ட குழுக்களுக்கு ஒரு வழி ANOVA-வின் தரவரிசை அடிப்படை மாற்று.",
        listOf(Input("g", "Groups (separate groups with ;)", "குழுக்கள் (குழுக்களை ; கொண்டு பிரிக்கவும்)", "12, 15, 14, 11, 13; 14, 17, 15, 13, 16, 15; 16, 19, 15, 17, 18"))
    ) { m, ta ->
        val r = kruskalWallis(Parse.groups(m["g"].orEmpty(), 3, 2))
        val tb = Table(listOf(if (ta) "குழு" else "Group", "n", if (ta) "தரவரிசைக் கூட்டுத்தொகை" else "Rank sum"), r.ns.indices.map { listOf((it + 1).toString(), r.ns[it].toString(), fmt(r.rankSums[it])) })
        Out(listOf("H" to fmt(r.h), "df" to r.df.toString(), "p" to pFmt(r.p), "N" to r.n.toString()),
            listOf(decision(r.p, ta), if (ta) "அனுமானங்கள்: சார்பற்ற கண்டறிதல்கள்; குழுக்களின் பரவல் வடிவம் ஒத்திருந்தால் இது நடுக்கோடிகளை ஒப்பிடுகிறது. p-மதிப்பு கை-வர்க்கத் தோராயம் (இணைத் திருத்தத்துடன்); ஒவ்வொரு குழுவிலும் குறைந்தது 5 கண்டறிதல்கள் இருந்தால் நம்பகமானது. குறிப்பிடத்தக்க H-க்குப் பின் இணை ஒப்பீட்டுச் சோதனைகள் (உம்: Dunn) தேவை."
            else "Assumptions: independent observations; with similar shapes it compares medians. The p-value uses the chi-square approximation (with tie correction), reliable when each group has about 5 or more observations. After a significant H, pairwise post-hoc tests (e.g. Dunn) are needed."),
            listOf((if (ta) "தரவரிசைச் சுருக்கம்" else "Rank summary") to tb))
    }
)
