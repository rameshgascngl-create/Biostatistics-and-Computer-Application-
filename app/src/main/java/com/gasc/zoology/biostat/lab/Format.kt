package com.gasc.zoology.biostat.lab

import com.gasc.zoology.biostat.stats.StatException
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

/** Number formatting shared by both languages (Western digits are used in Tamil mode too, as in Tamil Nadu textbooks). */
fun fmt(x: Double, digits: Int = 4): String = when {
    x.isNaN() -> "—"
    x.isInfinite() -> if (x > 0) "∞" else "−∞"
    x == 0.0 -> "0"
    abs(x) >= 1e6 || abs(x) < 1e-4 -> String.format(java.util.Locale.ROOT, "%." + (digits - 1) + "e", x)
    else -> BigDecimal(x).round(MathContext(digits)).stripTrailingZeros().toPlainString()
}

fun pFmt(p: Double): String = if (p.isNaN()) "—" else if (p < 1e-4) "< 0.0001" else fmt(p, 4)

/** Localised message for a calculator error code. */
fun errorText(e: StatException, ta: Boolean): String {
    val a = e.args
    fun n(i: Int) = a.getOrNull(i)?.toString() ?: ""
    fun t(en: String, tamil: String) = if (ta) tamil else en
    return when (e.code) {
        "tooMany" -> t("Too many values (limit ${n(0)}).", "மதிப்புகள் மிக அதிகம் (வரம்பு ${n(0)}).")
        "nonNumeric" -> t("Non-numeric entry: ${n(0)}. Use numbers separated by spaces, commas or semicolons.",
            "எண் அல்லாத உள்ளீடு: ${n(0)}. இடைவெளி, காற்புள்ளி அல்லது அரைப்புள்ளியால் பிரித்த எண்களை மட்டும் உள்ளிடவும்.")
        "needObs" -> t("At least ${n(0)} observation(s) required.", "குறைந்தது ${n(0)} கண்டறிதல்(கள்) தேவை.")
        "needMoreObs" -> t("More observations are required.", "மேலும் கண்டறிதல்கள் தேவை.")
        "needGroups" -> t("At least ${n(0)} groups are required; separate groups with a semicolon (;).",
            "குறைந்தது ${n(0)} குழுக்கள் தேவை; குழுக்களை அரைப்புள்ளி (;) கொண்டு பிரிக்கவும்.")
        "needGroupSize" -> t("Every group needs at least ${n(0)} observations.", "ஒவ்வொரு குழுவிலும் குறைந்தது ${n(0)} கண்டறிதல்கள் தேவை.")
        "needMatrix" -> t("Enter a table with at least 2 rows and 2 columns of numbers.", "குறைந்தது 2 வரிசைகள், 2 நெடுவரிசைகள் கொண்ட எண் அட்டவணையை உள்ளிடவும்.")
        "ragged" -> t("All rows must have the same number of columns.", "அனைத்து வரிசைகளிலும் நெடுவரிசைகளின் எண்ணிக்கை சமமாக இருக்க வேண்டும்.")
        "tooLarge" -> t("The table is too large.", "அட்டவணை மிகப் பெரியது.")
        "counts" -> t("Counts must be non-negative whole numbers.", "எண்ணிக்கைகள் எதிர்மறையற்ற முழு எண்களாக இருக்க வேண்டும்.")
        "badNumber" -> t("Enter a valid number for “${n(0)}”.", "“${n(0)}” புலத்தில் சரியான எண்ணை உள்ளிடவும்.")
        "oneNeed2" -> t("A one-sample t-test needs at least 2 observations.", "ஒரு மாதிரி t-சோதனைக்குக் குறைந்தது 2 கண்டறிதல்கள் தேவை.")
        "indNeed2" -> t("Each group needs at least 2 observations.", "ஒவ்வொரு குழுவிலும் குறைந்தது 2 கண்டறிதல்கள் தேவை.")
        "pairedSame" -> t("Paired data must have the same number of values in both lists.", "இணைத் தரவில் இரு பட்டியல்களிலும் மதிப்புகளின் எண்ணிக்கை சமமாக இருக்க வேண்டும்.")
        "pairedNeed2" -> t("At least 2 pairs are required.", "குறைந்தது 2 இணைகள் தேவை.")
        "pairedIdentical" -> t("All paired differences are identical (zero variance); the t statistic is undefined.",
            "இணை வேறுபாடுகள் அனைத்தும் ஒன்றே (மாறுபாடு பூஜ்யம்); t மதிப்பை வரையறுக்க இயலாது.")
        "zeroVar" -> t("The sample variance is zero; the t statistic is undefined.", "மாதிரி மாறுபாடு பூஜ்யம்; t மதிப்பை வரையறுக்க இயலாது.")
        "tUndefined" -> t("The t statistic is undefined for these data.", "இத்தரவுக்கு t மதிப்பை வரையறுக்க இயலாது.")
        "gofNeed2" -> t("Goodness-of-fit needs at least 2 categories.", "பொருத்தப் பொருத்த சோதனைக்குக் குறைந்தது 2 வகைகள் தேவை.")
        "expPos", "expSame", "expSum" -> t("Expected values must be positive, equal in number to the observed counts, and sum to the observed total.",
            "எதிர்பார்க்கப்படும் மதிப்புகள் நேர்மறையாகவும், கவனிக்கப்பட்ட எண்ணிக்கைகளுக்கு இணையான எண்ணிக்கையிலும், அவற்றின் மொத்தத்திற்குச் சமமான கூட்டுத்தொகையுடனும் இருக்க வேண்டும்.")
        "obsTotal", "margins" -> t("Every row and column total must be positive.", "ஒவ்வொரு வரிசை, நெடுவரிசை மொத்தமும் நேர்மறையாக இருக்க வேண்டும்.")
        "zeroVarXY" -> t("X and Y must both vary; correlation is undefined.", "X, Y இரண்டிலும் மாறுபாடு இருக்க வேண்டும்; ஒட்டுறவை வரையறுக்க இயலாது.")
        "pairNeed3" -> t("Correlation needs at least 3 pairs of equal length.", "ஒட்டுறவுக்குச் சம நீளமுள்ள குறைந்தது 3 இணைகள் தேவை.")
        "zeroWithin", "anovaUndefined" -> t("Within-group variance is zero; the F statistic is undefined.", "குழுவிற்குள் மாறுபாடு பூஜ்யம்; F மதிப்பை வரையறுக்க இயலாது.")
        "allIdentical" -> t("All values are identical; a rank-based test is undefined.", "அனைத்து மதிப்புகளும் ஒன்றே; தரவரிசை சோதனையை வரையறுக்க இயலாது.")
        "allZero", "needDiffs" -> t("All differences are zero; there is nothing to test.", "அனைத்து வேறுபாடுகளும் பூஜ்யம்; சோதிக்க எதுவும் இல்லை.")
        "epiGroup" -> t("Each exposure group must contain at least one subject.", "ஒவ்வொரு வெளிப்பாட்டுக் குழுவிலும் குறைந்தது ஒருவர் இருக்க வேண்டும்.")
        "sdPos" -> t("The standard deviation must be positive.", "திட்ட விலக்கம் நேர்மறையாக இருக்க வேண்டும்.")
        "zPos" -> t("The standard deviation must be positive.", "திட்ட விலக்கம் நேர்மறையாக இருக்க வேண்டும்.")
        "confRange" -> t("Confidence must lie between 0 and 1.", "நம்பக மட்டம் 0 முதல் 1 வரை இருக்க வேண்டும்.")
        "alphaRange" -> t("α must lie strictly between 0 and 1.", "α மதிப்பு 0-க்கும் 1-க்கும் இடையில் இருக்க வேண்டும்.")
        "propRange", "probRange" -> t("The proportion must lie strictly between 0 and 1.", "விகிதம் 0-க்கும் 1-க்கும் இடையில் இருக்க வேண்டும்.")
        "dropRange" -> t("The drop-out rate must be from 0% to below 80%.", "விடுபடும் விகிதம் 0% முதல் 80%-க்குக் குறைவாக இருக்க வேண்டும்.")
        "marginPos", "marginProp" -> t("The margin of error must be positive (and below 1 for a proportion).", "பிழை எல்லை நேர்மறையாக இருக்க வேண்டும் (விகிதத்திற்கு 1-ஐ விடக் குறைவாக).")
        "popInt" -> t("The population size must be a positive whole number.", "மக்கள்தொகை அளவு நேர்மறை முழு எண்ணாக இருக்க வேண்டும்.")
        "effectFinite" -> t("The effect size and margin must be positive numbers.", "விளைவு அளவும் பிழை எல்லையும் நேர்மறை எண்களாக இருக்க வேண்டும்.")
        "nInt" -> t("The sample size must be a positive whole number.", "மாதிரி அளவு நேர்மறை முழு எண்ணாக இருக்க வேண்டும்.")
        "hwThree" -> t("Enter exactly three genotype counts: AA, Aa, aa.", "AA, Aa, aa ஆகிய மூன்று மரபணுவகை எண்ணிக்கைகளை மட்டும் உள்ளிடவும்.")
        "hwAbsent" -> t("An allele is absent from the sample; the test is not informative.", "ஓர் அல்லீல் மாதிரியில் இல்லை; சோதனை பயனுள்ளதல்ல.")
        "needBoth" -> t("Both lists are required.", "இரு பட்டியல்களும் தேவை.")
        else -> t("Invalid input (${e.code}).", "தவறான உள்ளீடு (${e.code}).")
    }
}
