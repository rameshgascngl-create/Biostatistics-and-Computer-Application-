package com.gasc.zoology.biostat.lab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gasc.zoology.biostat.LocalTamil
import com.gasc.zoology.biostat.Mono
import com.gasc.zoology.biostat.SectionCard
import com.gasc.zoology.biostat.stats.*
import com.gasc.zoology.biostat.tr

class DataSet(val en: String, val ta: String, val lesson: String, val taskEn: String, val taskTa: String,
              val raw: String, val analyse: (Boolean) -> Out)

private fun tool(id: String, m: Map<String, String>, ta: Boolean) = TOOLS.first { it.id == id }.run(m, ta)

val DATASETS: List<DataSet> = listOf(
    DataSet("Fish length sample", "மீன் நீள மாதிரி", "U1_T08",
        "Calculate mean, median and mode; comment on spread and possible outlier.", "சராசரி, நடுக்கோடி, முகடு ஆகியவற்றைக் கணக்கிட்டு, பரவலையும் சாத்தியமான புறநிலை மதிப்பையும் பற்றிக் கருத்துரைக்கவும்.",
        "12.4, 13.1, 13.5, 14.0, 14.0, 14.7, 15.2, 16.8") { ta -> tool("descriptive", mapOf("data" to "12.4,13.1,13.5,14.0,14.0,14.7,15.2,16.8"), ta) },
    DataSet("Blood-group counts", "இரத்த வகை எண்ணிக்கைகள்", "U1_T03",
        "Identify variable scale and prepare a percentage table and pie diagram.", "மாறியின் அளவீட்டுத் தரத்தை அடையாளம் கண்டு, சதவீத அட்டவணையும் வட்ட வரைபடமும் தயாரிக்கவும்.",
        "A: 42, B: 18, AB: 8, O: 52") { ta ->
        val labels = listOf("A", "B", "AB", "O"); val v = listOf(42.0, 18.0, 8.0, 52.0); val n = v.sum()
        Out(listOf("N" to fmt(n), (if (ta) "மிகப் பொதுவான வகை" else "Most frequent group") to "O"),
            listOf(if (ta) "இது பெயரளவு வகைத் தரவு; சராசரி பொருளற்றது — நிகழ்வெண், சதவீதம், முகடு ஆகியவற்றைப் பயன்படுத்தவும்." else "This is nominal categorical data: a mean is meaningless — use frequencies, percentages and the mode."),
            listOf((if (ta) "சதவீத அட்டவணை" else "Percentage table") to Table(listOf(if (ta) "வகை" else "Group", if (ta) "எண்ணிக்கை" else "Count", "%"),
                labels.indices.map { listOf(labels[it], fmt(v[it]), fmt(100 * v[it] / n, 4)) })))
    },
    DataSet("Quadrat insect counts", "சதுரப் பகுதி பூச்சி எண்ணிக்கைகள்", "U1_T05",
        "Discuss sampling unit, variation and whether more quadrats are needed.", "மாதிரி அலகு, மாறுபாடு, மேலும் சதுரப் பகுதிகள் தேவையா என்பதைப் பற்றி விவாதிக்கவும்.",
        "2, 5, 1, 0, 8, 3, 2, 10, 4, 1") { ta -> tool("descriptive", mapOf("data" to "2,5,1,0,8,3,2,10,4,1"), ta) },
    DataSet("Plant-height classes", "தாவர உயர வகுப்புகள்", "U1_T06",
        "Convert to continuous boundaries and calculate grouped mean.", "தொடர் எல்லைகளாக மாற்றி, தொகுப்புச் சராசரியைக் கணக்கிடவும்.",
        "10–19: 4, 20–29: 9, 30–39: 12, 40–49: 5") { ta ->
        val lo = listOf(10, 20, 30, 40); val f = listOf(4.0, 9.0, 12.0, 5.0)
        val m = lo.map { it + 4.5 }; val n = f.sum(); val gm = f.indices.sumOf { f[it] * m[it] } / n
        Out(listOf("N" to fmt(n), (if (ta) "தொகுப்புச் சராசரி" else "Grouped mean") to fmt(gm)),
            listOf(if (ta) "தொகுப்புச் சராசரி = Σ(f·நடுமதிப்பு) / Σf. வகுப்பு 10–19-இன் தொடர் எல்லைகள் 9.5–19.5." else "Grouped mean = Σ(f·midpoint) / Σf. The class 10–19 has continuous boundaries 9.5–19.5."),
            listOf((if (ta) "வகுப்பு அட்டவணை" else "Class table") to Table(listOf(if (ta) "எல்லைகள்" else "Boundaries", "f", if (ta) "நடுமதிப்பு" else "Midpoint", "f·m"),
                lo.indices.map { listOf("${fmt(lo[it] - 0.5)}–${fmt(lo[it] + 9.5)}", fmt(f[it]), fmt(m[it]), fmt(f[it] * m[it])) })))
    },
    DataSet("Fish-length variability", "மீன் நீளத்தின் மாறுபாடு", "U2_T02",
        "Calculate sample variance and standard deviation; interpret variation in centimetres.", "மாதிரி மாறுபாடு, திட்ட விலக்கத்தைக் கணக்கிட்டு, சென்டிமீட்டரில் மாறுபாட்டை விளக்கவும்.",
        "12.1, 12.8, 13.0, 13.7, 14.4, 15.0") { ta -> tool("descriptive", mapOf("data" to "12.1,12.8,13.0,13.7,14.4,15.0"), ta) },
    DataSet("Aquaculture strain comparison by CV", "மாறுபாட்டுக் குணகம் மூலம் மீன்வளர்ப்பு இனங்களின் ஒப்பீடு", "U2_T03",
        "Calculate CV for both strains and identify the more relatively variable strain.", "இரு இனங்களின் CV-ஐக் கணக்கிட்டு, ஒப்பீட்டளவில் அதிக மாறுபாடுள்ள இனத்தைக் கண்டறியவும்.",
        "Strain A: mean 250, SD 20; Strain B: mean 120, SD 15") { ta ->
        val cvA = 100 * 20.0 / 250; val cvB = 100 * 15.0 / 120
        Out(listOf((if (ta) "இனம் A — CV" else "Strain A CV") to fmt(cvA) + "%", (if (ta) "இனம் B — CV" else "Strain B CV") to fmt(cvB) + "%"),
            listOf(if (ta) "இனம் B-இன் திட்ட விலக்கம் சிறிதாக இருந்தாலும், அதன் சராசரியுடன் ஒப்பிடும்போது ஒப்பீட்டு மாறுபாடு அதிகம் (CV = SD/சராசரி × 100). அலகுகள் அல்லது சராசரிகள் வேறுபடும்போது CV-ஐப் பயன்படுத்தவும்." else "Strain B has the smaller SD but the larger relative variability (CV = SD / mean × 100). Use the CV when means or units differ."))
    },
    DataSet("Blood-group goodness-of-fit", "இரத்த வகைப் பொருத்தச் சோதனை", "U2_T05",
        "Calculate expected counts and chi-square; state degrees of freedom and conclusion.", "எதிர்பார்க்கப்படும் எண்ணிக்கைகள், கை-வர்க்கம் ஆகியவற்றைக் கணக்கிட்டு, சுதந்திர அளவையும் முடிவையும் கூறவும்.",
        "Observed: 38, 22, 10, 50; expected proportions: 0.3, 0.2, 0.1, 0.4") { ta ->
        val obs = listOf(38.0, 22.0, 10.0, 50.0); val n = obs.sum(); val exp = listOf(0.3, 0.2, 0.1, 0.4).map { it * n }
        tool("chigof", mapOf("obs" to obs.joinToString(","), "exp" to exp.joinToString(",")), ta)
    },
    DataSet("Paired fish-weight trial", "இணை மீன் எடை சோதனை", "U2_T07",
        "Calculate pairwise differences, paired t and df; interpret the mean gain.", "இணை வேறுபாடுகள், இணை t, சுதந்திர அளவு ஆகியவற்றைக் கணக்கிட்டு, சராசரி அதிகரிப்பை விளக்கவும்.",
        "Before: 42, 45, 40, 48, 44, 46; after: 45, 47, 43, 50, 48, 49") { ta ->
        tool("ttest", mapOf("type" to "paired", "a" to "42,45,40,48,44,46", "b" to "45,47,43,50,48,49"), ta) },
    DataSet("Fish length and weight pairs", "மீன் நீளம், எடை இணைகள்", "U3_T03",
        "Draw a scatterplot and calculate Pearson r; inspect linearity and outliers.", "சிதறல் வரைபடம் வரைந்து பியர்சன் r-ஐக் கணக்கிட்டு, நேரியல் தன்மையையும் புறநிலை மதிப்புகளையும் ஆராயவும்.",
        "Length: 10, 12, 14, 16, 18, 20; weight: 22, 29, 38, 49, 63, 78") { ta ->
        tool("corr", mapOf("x" to "10,12,14,16,18,20", "y" to "22,29,38,49,63,78"), ta) },
    DataSet("Calibration regression", "அளவீட்டுப் பின்னடைவு", "U3_T05",
        "Fit Y-on-X regression, calculate slope/intercept and predict absorbance at concentration 5 within the calibrated range.", "Y-ஐ X-மீது பின்னடைவு பொருத்தி, சாய்வு, வெட்டுத்துண்டைக் கணக்கிட்டு, அளவீட்டு வீச்சுக்குள் செறிவு 5-இல் உறிஞ்சுதலை முன்கணிக்கவும்.",
        "Concentration: 0, 2, 4, 6, 8; absorbance: 0.02, 0.18, 0.34, 0.51, 0.67") { ta ->
        val x = listOf(0.0, 2.0, 4.0, 6.0, 8.0); val y = listOf(0.02, 0.18, 0.34, 0.51, 0.67)
        val o = tool("corr", mapOf("x" to x.joinToString(","), "y" to y.joinToString(",")), ta); val r = pearson(x, y)
        o.copy(metrics = o.metrics + ((if (ta) "செறிவு 5-இல் முன்கணிப்பு" else "Predicted at concentration 5") to fmt(r.a + r.b * 5)))
    },
    DataSet("Three-diet larval growth", "மூன்று உணவுகளில் இளரி வளர்ச்சி", "U2_T07",
        "Research enrichment: compare three independent mean growth responses using one-way ANOVA and interpret the omnibus F-test.", "ஆய்வு மேம்பாடு: ஒரு வழி ANOVA மூலம் மூன்று சார்பற்ற சராசரி வளர்ச்சிகளை ஒப்பிட்டு, ஒட்டுமொத்த F-சோதனையை விளக்கவும்.",
        "12, 14, 13, 15, 14; 18, 17, 19, 20, 18; 14, 15, 16, 15, 17") { ta -> tool("anova", mapOf("g" to "12,14,13,15,14; 18,17,19,20,18; 14,15,16,15,17"), ta) },
    DataSet("Habitat rank association", "வாழிடத் தரவரிசைத் தொடர்பு", "U3_T04",
        "Research enrichment: compare Pearson and Spearman association and discuss monotonic versus strictly linear relationships.", "ஆய்வு மேம்பாடு: பியர்சன், ஸ்பியர்மன் தொடர்புகளை ஒப்பிட்டு, ஒருபோக்கு மற்றும் கண்டிப்பான நேரியல் உறவுகளை விவாதிக்கவும்.",
        "X: 1–8; Y: 2, 1, 4, 3, 6, 5, 8, 7") { ta -> tool("corr", mapOf("x" to "1,2,3,4,5,6,7,8", "y" to "2,1,4,3,6,5,8,7"), ta) },
    DataSet("Treatment survival 2×2", "சிகிச்சை உயிர்பிழைப்பு 2×2", "U2_T05",
        "Research enrichment: calculate risk ratio, odds ratio and absolute risk difference for a 2×2 table.", "ஆய்வு மேம்பாடு: 2×2 அட்டவணைக்கு இடர் விகிதம், முரண்பாட்டு விகிதம், தனி இடர் வேறுபாட்டைக் கணக்கிடவும்.",
        "a, b, c, d = 30, 70, 15, 85") { ta -> tool("epi", mapOf("a" to "30", "b" to "70", "c" to "15", "d" to "85"), ta) }
)

@Composable
fun DataLabScreen(onOpenLesson: (String) -> Unit) {
    val ta = LocalTamil.current
    val results = remember { mutableStateMapOf<Int, Pair<Boolean, Out?>>() }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("Biology data laboratory — ${DATASETS.size} datasets", "உயிரியல் தரவு ஆய்வகம் — ${DATASETS.size} தரவுத்தொகுப்புகள்"), style = MaterialTheme.typography.titleLarge)
        DATASETS.forEachIndexed { i, d ->
            SectionCard("${i + 1}. " + (if (ta) d.ta else d.en), Modifier.testTag("dataset-$i")) {
                Mono(d.raw.let { raw -> if (ta) raw.replace("Observed", "காணப்பட்டது").replace("expected proportions", "எதிர்பார்க்கும் விகிதங்கள்").replace("Before", "முன்").replace("after", "பின்").replace("Length", "நீளம்").replace("weight", "எடை").replace("Concentration", "செறிவு").replace("absorbance", "உறிஞ்சுதல்").replace("Strain", "இனம்").replace("mean", "சராசரி") else raw })
                Text((tr("Task: ", "பணி: ")) + (if (ta) d.taskTa else d.taskEn), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Button(onClick = { results[i] = ta to d.analyse(ta) }, modifier = Modifier.testTag("analyse-$i")) { Text(tr("Guided analysis", "வழிகாட்டிய பகுப்பாய்வு")) }
                    OutlinedButton(onClick = { onOpenLesson(d.lesson) }) { Text(tr("Related lesson", "தொடர்புடைய பாடம்")) }
                }
                results[i]?.let { (lang, o) -> if (o != null) ResultView(if (lang == ta) o else d.analyse(ta)) }
            }
        }
    }
}
