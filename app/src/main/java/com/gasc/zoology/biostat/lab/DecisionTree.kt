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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gasc.zoology.biostat.Bullets
import com.gasc.zoology.biostat.LocalTamil
import com.gasc.zoology.biostat.SectionCard
import com.gasc.zoology.biostat.tr

sealed interface TreeNode
data class TreeQuestion(val en: String, val ta: String, val choices: List<Choice>) : TreeNode
data class Choice(val en: String, val ta: String, val next: String)
data class Leaf(val titleEn: String, val titleTa: String, val whyEn: String, val whyTa: String,
                val checkEn: String, val checkTa: String, val tools: List<String>) : TreeNode

val TREE: Map<String, TreeNode> = mapOf(
    "start" to TreeQuestion("What do you want to do?", "நீங்கள் என்ன செய்ய விரும்புகிறீர்கள்?", listOf(
        Choice("Describe or check one set of measurements", "ஒரு தொகுதி அளவீடுகளை விவரிக்க அல்லது சரிபார்க்க", "describe"),
        Choice("Compare groups or conditions", "குழுக்களை அல்லது நிலைமைகளை ஒப்பிட", "compare"),
        Choice("Study the relationship between two variables", "இரு மாறிகளுக்கு இடையிலான தொடர்பை ஆய்வு செய்ய", "assoc"),
        Choice("Analyse counts in categories", "வகைகளிலுள்ள எண்ணிக்கைகளைப் பகுப்பாய்வு செய்ய", "cat"),
        Choice("Plan a study (sample size or power)", "ஆய்வைத் திட்டமிட (மாதிரி அளவு / ஆற்றல்)", "plan"))),
    "describe" to TreeQuestion("What do you need?", "உங்களுக்கு என்ன தேவை?", listOf(
        Choice("Summary measures and a histogram", "சுருக்க அளவைகளும் நிகழ்வெண் வரைபடமும்", "L_desc"),
        Choice("A confidence interval for the mean", "சராசரிக்கான நம்பக இடைவெளி", "L_ci"),
        Choice("Is the data approximately normal?", "தரவு தோராயமாக இயல்புப் பரவலில் உள்ளதா?", "L_qq"),
        Choice("Position of one value in a normal distribution", "இயல்புப் பரவலில் ஒரு மதிப்பின் நிலை", "L_z"))),
    "compare" to TreeQuestion("How many groups or samples are compared?", "எத்தனை குழுக்கள்/மாதிரிகள் ஒப்பிடப்படுகின்றன?", listOf(
        Choice("One sample against a reference value", "ஒரு மாதிரி — ஒரு குறிப்பு மதிப்புடன்", "one_norm"),
        Choice("Two groups of different subjects (independent)", "வெவ்வேறு உயிரிகளின் இரு குழுக்கள் (சார்பற்றவை)", "two_norm"),
        Choice("Two measurements on the same subjects (paired)", "ஒரே உயிரிகளில் இரு அளவீடுகள் (இணை)", "pair_norm"),
        Choice("Three or more independent groups", "மூன்று அல்லது அதற்கு மேற்பட்ட சார்பற்ற குழுக்கள்", "multi_norm"))),
    "one_norm" to TreeQuestion("Is the data approximately normal (or n large)?", "தரவு தோராயமாக இயல்பானதா (அல்லது n பெரிதா)?", listOf(
        Choice("Approximately normal (or adequately large sample)", "தோராயமாக இயல்பு (அல்லது போதுமான பெரிய மாதிரி)", "L_t1"),
        Choice("Non-normal but deviations from reference are roughly symmetric", "இயல்பல்ல; குறிப்பு மதிப்பிலிருந்து வேறுபாடுகள் தோராயமாகச் சமச்சீர்", "L_wil1"),
        Choice("Strongly asymmetric differences", "வேறுபாடுகளில் மிகுந்த சமச்சீரின்மை", "L_sign1"))),
    "two_norm" to TreeQuestion("Is each group approximately normal (or n large)?", "ஒவ்வொரு குழுவும் தோராயமாக இயல்பானதா (அல்லது n பெரிதா)?", listOf(
        Choice("Numerical data, means are the goal (normal or adequate sample)", "எண் தரவு; சராசரிகளை ஒப்பிடுதல் நோக்கம் (இயல்பு அல்லது போதிய மாதிரி)", "L_welch"),
        Choice("Ordinal data or similar-shaped non-normal distributions", "வரிசைத் தரவு அல்லது ஒத்த வடிவமுடைய இயல்பற்ற பரவல்கள்", "L_mw"),
        Choice("Very different shapes or strongly skewed unequal spreads", "மிகவும் வேறுபட்ட வடிவங்கள் அல்லது சமமற்ற பரவல்கள்", "L_robust"))),
    "pair_norm" to TreeQuestion("Are the paired differences approximately normal?", "இணை வேறுபாடுகள் தோராயமாக இயல்பானவையா?", listOf(
        Choice("Approximately normal paired differences", "இணை வேறுபாடுகள் தோராயமாக இயல்பு", "L_paired"),
        Choice("Non-normal but roughly symmetric paired differences", "இயல்பற்றவை; ஆனால் இணை வேறுபாடுகள் தோராயமாகச் சமச்சீர்", "L_wil"),
        Choice("Markedly asymmetric paired differences", "இணை வேறுபாடுகளில் மிகுந்த சமச்சீரின்மை", "L_signpair"))),
    "multi_norm" to TreeQuestion("What is the outcome scale and spread pattern?", "தரவின் வகையும் குழுப் பரவல் நிலையும் என்ன?", listOf(
        Choice("Numerical, approximately normal, similar variances", "எண் தரவு; தோராயமாக இயல்பு; ஒத்த மாறுபாடுகள்", "L_anova"),
        Choice("Numerical, approximately normal, unequal variances", "எண் தரவு; தோராயமாக இயல்பு; சமமற்ற மாறுபாடுகள்", "L_welch_anova"),
        Choice("Ordinal or non-normal with comparable distribution shapes", "வரிசைத் தரவு அல்லது ஒத்த பரவல் வடிவமுடைய இயல்பற்ற தரவு", "L_kw"),
        Choice("Strongly non-normal with different shapes and variances", "வெவ்வேறு பரவல் வடிவங்களும் மாறுபாடுகளும் கொண்ட இயல்பற்ற தரவு", "L_robust"))),
    "assoc" to TreeQuestion("What kind of relationship?", "எந்த வகைத் தொடர்பு?", listOf(
        Choice("Two numerical variables", "இரு எண் மாறிகள்", "L_corr"))),
    "cat" to TreeQuestion("What is the design?", "ஆய்வு வடிவமைப்பு என்ன?", listOf(
        Choice("One categorical variable against expected proportions", "எதிர்பார்க்கும் விகிதங்களுடன் ஒரு வகை மாறி", "cat1"),
        Choice("Two categorical variables (contingency table)", "இரு வகை மாறிகள் (இணைப்பு அட்டவணை)", "cat2"))),
    "cat1" to TreeQuestion("Are the expected proportions from genetic theory?", "எதிர்பார்க்கும் விகிதங்கள் மரபியல் கோட்பாட்டிலிருந்து வருகின்றனவா?", listOf(
        Choice("Hardy–Weinberg genotype proportions", "ஹார்டி–வெயின்பர்க் மரபணுவகை விகிதங்கள்", "L_hw"),
        Choice("Any other expected proportions (e.g. Mendelian ratios)", "பிற எதிர்பார்க்கும் விகிதங்கள் (உம்: மெண்டலிய விகிதங்கள்)", "L_gof"))),
    "cat2" to TreeQuestion("Do you need risk and odds ratios?", "இடர் விகிதமும் முரண்பாட்டு விகிதமும் தேவையா?", listOf(
        Choice("Yes — exposure and outcome (2×2)", "ஆம் — வெளிப்பாடும் விளைவும் (2×2)", "L_epi"),
        Choice("No — only test for association", "இல்லை — தொடர்பைச் சோதித்தால் போதும்", "L_chi"))),
    "plan" to TreeQuestion("What do you need to plan?", "எதைத் திட்டமிட வேண்டும்?", listOf(
        Choice("Sample size for a given margin of error", "குறிப்பிட்ட பிழை எல்லைக்கான மாதிரி அளவு", "L_ss"),
        Choice("Power of a t-test for a given n", "கொடுக்கப்பட்ட n-க்கான t-சோதனையின் ஆற்றல்", "L_pw"))),

    "L_desc" to Leaf("Descriptive statistics", "விளக்க புள்ளியியல்", "Always begin by summarising and plotting the data.", "எப்போதும் தரவைச் சுருக்கி வரைபடமாக்குவதிலிருந்து தொடங்குங்கள்.", "Look for outliers, skewness and data-entry errors.", "புறநிலை மதிப்புகள், கோட்டம், உள்ளீட்டுப் பிழைகளைத் தேடுங்கள்.", listOf("descriptive")),
    "L_ci" to Leaf("Confidence interval for a mean", "சராசரிக்கான நம்பக இடைவெளி", "Estimates the plausible range of the population mean.", "மக்கள்தொகைச் சராசரியின் நம்பத்தக்க வீச்சை மதிப்பிடுகிறது.", "Data should be roughly normal, or n reasonably large.", "தரவு தோராயமாக இயல்பாக, அல்லது n போதுமான அளவு பெரிதாக இருக்க வேண்டும்.", listOf("ci")),
    "L_qq" to Leaf("Normal Q–Q plot", "இயல்பு Q–Q வரைபடம்", "A graphical check of the normality assumption.", "இயல்புத்தன்மை அனுமானத்தின் வரைபடச் சோதனை.", "Small samples give noisy plots; combine with biological judgement.", "சிறிய மாதிரிகள் குழப்பமான வரைபடம் தரும்; உயிரியல் மதிப்பீட்டுடன் இணைத்துப் பாருங்கள்.", listOf("qq")),
    "L_z" to Leaf("Z-score", "Z-மதிப்பு", "Standardises a value and gives tail probabilities.", "மதிப்பைத் தரப்படுத்தி வால் நிகழ்தகவுகளைத் தருகிறது.", "Requires an approximately normal variable with known μ and σ.", "μ, σ தெரிந்த, தோராயமாக இயல்பான மாறி தேவை.", listOf("z")),
    "L_t1" to Leaf("One-sample t-test", "ஒரு மாதிரி t-சோதனை", "Compares a sample mean with a reference value.", "மாதிரிச் சராசரியை ஒரு குறிப்பு மதிப்புடன் ஒப்பிடுகிறது.", "Independent observations; check normality with a Q–Q plot.", "கண்டறிதல்கள் சார்பற்றவை; Q–Q வரைபடத்தால் இயல்புத்தன்மையைச் சரிபாருங்கள்.", listOf("ttest", "qq")),
    "L_wil1" to Leaf("One-sample Wilcoxon signed-rank test", "ஒரு மாதிரி வில்காக்ஸன் குறியிட்ட தரநிலைச் சோதனை", "Tests whether differences from a specified reference are centred on zero under a roughly symmetric difference distribution.", "குறிப்பு மதிப்பிலிருந்து வேறுபாடுகள் தோராயமாகச் சமச்சீராக இருக்கும் நிலையில் அவற்றின் மையம் பூஜ்யமா எனச் சோதிக்கும்.", "Enter the reference value as Before and observations as After; strong asymmetry calls for a different method.", "குறிப்பு மதிப்பை முன் தரவாகவும் கண்டறிதல்களைப் பின் தரவாகவும் உள்ளிடவும்; மிகுந்த சமச்சீரின்மைக்கு வேறு சோதனை தேவை.", listOf("wilcoxon")),
    "L_sign1" to Leaf("One-sample sign test: method guidance", "ஒரு மாதிரி குறிச் சோதனை: வழிகாட்டல்",
        "For strongly asymmetric deviations from the reference, a sign test can address a median rather than a mean.",
        "குறிப்பு மதிப்பிலிருந்து வேறுபாடுகள் மிகுந்த சமச்சீரின்மையுடன் இருந்தால், சராசரிக்கு மாற்றாக நடுக்கோடிக்கான குறிச் சோதனையைப் பயன்படுத்தலாம்.",
        "The sign test is not included in this app. Use validated software or review a suitable robust/bootstrap analysis.",
        "குறிச் சோதனை இச்செயலியில் இல்லை; சரிபார்க்கப்பட்ட மென்பொருளில் மேற்கொள்ளவும் அல்லது பொருத்தமான வலுவான பகுப்பாய்வைத் தேர்ந்தெடுக்கவும்.", emptyList()),
    "L_welch" to Leaf("Welch independent-samples t-test", "வெல்ச் சார்பற்ற மாதிரி t-சோதனை", "Compares two means without assuming equal variances.", "சம மாறுபாட்டை அனுமானிக்காமல் இரு சராசரிகளை ஒப்பிடுகிறது.", "Do not use for paired or repeated measurements.", "இணை அல்லது மீண்டும் மீண்டும் எடுத்த அளவீடுகளுக்குப் பயன்படுத்த வேண்டாம்.", listOf("ttest")),
    "L_mw" to Leaf("Mann–Whitney U test", "மான்–விட்னி U சோதனை", "Rank-based comparison of two independent groups.", "இரு சார்பற்ற குழுக்களின் தரவரிசை அடிப்படை ஒப்பீடு.", "Interpret as a median comparison only if the shapes are similar.", "வடிவங்கள் ஒத்திருந்தால் மட்டுமே நடுக்கோடி ஒப்பீடாக விளக்கவும்.", listOf("mw")),
    "L_signpair" to Leaf("Paired sign test: method guidance", "இணை குறிச் சோதனை: வழிகாட்டல்",
        "A paired sign test can test a median paired difference without assuming symmetric differences.",
        "இணை வேறுபாடுகளின் சமச்சீரை அனுமானிக்காமல், அவற்றின் நடுக்கோடிக்கான குறிச் சோதனை செய்யலாம்.",
        "The paired sign test is not in this app; do not force Wilcoxon signed-rank on strongly asymmetric differences.",
        "இணை குறிச் சோதனை இச்செயலியில் இல்லை; மிகுந்த சமச்சீரின்மையுள்ள வேறுபாடுகளுக்கு வில்காக்ஸன் சோதனையைத் திணிக்க வேண்டாம்.", emptyList()),
    "L_paired" to Leaf("Paired t-test", "இணை t-சோதனை", "Analyses within-subject differences.", "ஒரே உயிரிக்குள் உள்ள வேறுபாடுகளைப் பகுப்பாய்வு செய்கிறது.", "Pairs must be genuinely matched; the differences should be roughly normal.", "இணைகள் உண்மையாகப் பொருந்தியிருக்க வேண்டும்; வேறுபாடுகள் தோராயமாக இயல்பாக இருக்க வேண்டும்.", listOf("ttest")),
    "L_wil" to Leaf("Wilcoxon signed-rank test", "வில்காக்ஸன் குறியிட்ட தரநிலைச் சோதனை", "Rank-based alternative to the paired t-test.", "இணை t-சோதனைக்கான தரவரிசை அடிப்படை மாற்று.", "Assumes a roughly symmetric distribution of differences.", "வேறுபாடுகளின் பரவல் தோராயமாகச் சமச்சீர் என அனுமானிக்கிறது.", listOf("wilcoxon")),
    "L_anova" to Leaf("One-way ANOVA with Tukey HSD", "ஒரு வழி ANOVA மற்றும் டூக்கி HSD", "Compares several means without inflating the type I error.", "முதல்வகைப் பிழையை அதிகரிக்காமல் பல சராசரிகளை ஒப்பிடுகிறது.", "Do not run many t-tests instead; follow a significant F with Tukey HSD.", "பல t-சோதனைகளை இயக்க வேண்டாம்; முக்கியத்துவமுள்ள F-க்குப் பின் டூக்கி HSD பயன்படுத்தவும்.", listOf("anova", "tukey")),
    "L_welch_anova" to Leaf("Welch one-way ANOVA", "வெல்ச் ஒரு வழி ANOVA",
        "Compares means of independent numerical groups without assuming equal variances.",
        "சம மாறுபாடு என்ற அனுமானமின்றி சார்பற்ற எண் தரவுக் குழுக்களின் சராசரிகளை ஒப்பிடும்.",
        "Check approximate normality and independence. After a significant result, use Games–Howell for post-hoc comparisons; ordinary Tukey is not automatically appropriate.",
        "தோராயமான இயல்புப் பரவலும் சார்பின்மையும் தேவை. குறிப்பிடத்தக்க முடிவுக்குப் பின் கேம்ஸ்–ஹோவெல் இணை ஒப்பீடு பொருத்தமானது; வழக்கமான டூக்கி முறையைத் தானாகப் பயன்படுத்த வேண்டாம்.",
        listOf("welchanova")),
    "L_robust" to Leaf("Review a robust or resampling analysis", "வலுவான அல்லது மறுமாதிரி பகுப்பாய்வைத் தேர்ந்தெடு",
        "Unequal variances and unequal shapes do not automatically justify Kruskal–Wallis. The scientific question (means versus distributions) matters.",
        "சமமற்ற மாறுபாடுகளும் வடிவங்களும் கிரஸ்கல்–வாலிஸ் சோதனையைத் தானாக நியாயப்படுத்தாது; ஆய்வுக் கேள்வி முக்கியம்.",
        "Inspect the data and seek an appropriate robust, permutation or bootstrap procedure with specialist guidance. Not implemented in this app.",
        "தரவை ஆராய்ந்து நிபுணர் வழிகாட்டலுடன் பொருத்தமான வலுவான, இடமாற்ற அல்லது மறுமாதிரி முறையைப் பயன்படுத்தவும்; இச்செயலியில் இது இல்லை.", emptyList()),
    "L_kw" to Leaf("Kruskal–Wallis H test", "கிரஸ்கல்–வாலிஸ் H சோதனை", "Compares rank distributions of independent groups, not automatically their means or medians.", "சார்பற்ற குழுக்களின் தரவரிசைப் பரவல்களை ஒப்பிடும்; அவற்றின் சராசரி அல்லது நடுக்கோடியைத் தானாகச் சோதிப்பதல்ல.", "Median interpretation requires comparably shaped distributions. Unequal spread alone is not a reason to substitute this for ANOVA; a significant result needs post-hoc tests.", "நடுக்கோடி விளக்கத்திற்கு ஒத்த பரவல் வடிவங்கள் தேவை; சமமற்ற மாறுபாடு மட்டுமே ANOVA-க்கு மாற்றாக இதைப் பயன்படுத்தக் காரணமல்ல. குறிப்பிடத்தக்க முடிவுக்குப் பின் இணை ஒப்பீடு தேவை.", listOf("kw")),
    "L_corr" to Leaf("Pearson correlation and simple regression", "பியர்சன் ஒட்டுறவும் எளிய பின்னடைவும்", "Quantifies linear association and fits Y on X.", "நேரியல் தொடர்பை அளவிட்டு Y-ஐ X-மீது பொருத்துகிறது.", "Inspect the scatter plot; use Spearman ρ for monotonic non-linear relationships.", "சிதறல் வரைபடத்தைப் பாருங்கள்; நேரியல் அல்லாத ஒருபோக்கு உறவுகளுக்கு ஸ்பியர்மன் ρ பயன்படுத்தவும்.", listOf("corr")),
    "L_hw" to Leaf("Hardy–Weinberg test", "ஹார்டி–வெயின்பர்க் சோதனை", "Tests genotype counts against equilibrium expectations.", "மரபணுவகை எண்ணிக்கைகளைச் சமநிலை எதிர்பார்ப்புகளுடன் சோதிக்கிறது.", "Expected counts should be at least 5.", "எதிர்பார்க்கப்படும் எண்ணிக்கைகள் குறைந்தது 5 ஆக இருக்க வேண்டும்.", listOf("hw")),
    "L_gof" to Leaf("Chi-square goodness of fit", "கை-வர்க்கப் பொருத்தச் சோதனை", "Compares observed and expected category counts.", "காணப்பட்ட, எதிர்பார்க்கப்படும் வகை எண்ணிக்கைகளை ஒப்பிடுகிறது.", "Use counts, not percentages; expected counts should be at least 5.", "சதவீதங்களை அல்ல, எண்ணிக்கைகளைப் பயன்படுத்தவும்; எதிர்பார்க்கப்படும் எண்ணிக்கைகள் குறைந்தது 5.", listOf("chigof")),
    "L_epi" to Leaf("Risk ratio and odds ratio", "இடர் விகிதமும் முரண்பாட்டு விகிதமும்", "Effect measures for a 2×2 exposure–outcome table.", "2×2 வெளிப்பாடு–விளைவு அட்டவணைக்கான விளைவு அளவைகள்.", "Use the OR for case-control designs; the RR needs cohort or trial data.", "வழக்கு–கட்டுப்பாட்டு ஆய்வுகளுக்கு OR; RR-க்கு கூட்டு அல்லது சோதனைத் தரவு தேவை.", listOf("epi", "chiind")),
    "L_chi" to Leaf("Chi-square test of independence", "கை-வர்க்கச் சார்பின்மைச் சோதனை", "Tests association between two categorical variables.", "இரு வகை மாறிகளுக்கிடையிலான தொடர்பைச் சோதிக்கிறது.", "For sparse 2×2 tables the output also gives Fisher's exact test.", "குறைந்த எண்ணிக்கையுள்ள 2×2 அட்டவணைக்குப் பிஷரின் துல்லிய சோதனையும் வழங்கப்படும்.", listOf("chiind")),
    "L_ss" to Leaf("Sample-size planner", "மாதிரி அளவுத் திட்டமிடல்", "Finds n for the required precision.", "தேவையான துல்லியத்திற்கான n-ஐக் கண்டறிகிறது.", "Plan before collecting data and allow for drop-out.", "தரவு சேகரிக்கும் முன் திட்டமிட்டு, விடுபடுதலுக்கும் இடமளிக்கவும்.", listOf("samplesize")),
    "L_pw" to Leaf("Power of a t-test", "t-சோதனையின் ஆற்றல்", "Probability of detecting a true difference.", "உண்மை வேறுபாட்டைக் கண்டறியும் நிகழ்தகவு.", "Aim for power of at least 0.80.", "குறைந்தது 0.80 ஆற்றலை இலக்காகக் கொள்ளுங்கள்.", listOf("power"))
)

@Composable
fun DecisionTreeScreen(onOpenTool: (String) -> Unit) {
    val ta = LocalTamil.current
    var path by rememberSaveable { mutableStateOf(listOf("start")) }
    val node = TREE[path.last()]
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(tr("Which test should I use?", "எந்தச் சோதனையைப் பயன்படுத்த வேண்டும்?"), style = MaterialTheme.typography.titleLarge)
        when (node) {
            is TreeQuestion -> {
                Text(if (ta) node.ta else node.en, style = MaterialTheme.typography.titleMedium)
                node.choices.forEachIndexed { i, c ->
                    Button(onClick = { path = path + c.next }, modifier = Modifier.fillMaxWidth().testTag("choice-$i")) { Text(if (ta) c.ta else c.en) }
                }
            }
            is Leaf -> SectionCard(tr("Recommended method", "பரிந்துரைக்கப்படும் முறை"), Modifier.testTag("leaf")) {
                Text(if (ta) node.titleTa else node.titleEn, style = MaterialTheme.typography.titleMedium)
                Bullets(listOf((tr("Why: ", "காரணம்: ")) + (if (ta) node.whyTa else node.whyEn), (tr("Check: ", "சரிபார்க்க: ")) + (if (ta) node.checkTa else node.checkEn)))
                node.tools.forEach { id ->
                    val tool = TOOLS.first { it.id == id }
                    Button(onClick = { onOpenTool(id) }, modifier = Modifier.padding(top = 6.dp)) { Text(tr("Open: ", "திற: ") + (if (ta) tool.titleTa else tool.titleEn)) }
                }
            }
            null -> Text("—")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (path.size > 1) OutlinedButton(onClick = { path = path.dropLast(1) }) { Text(tr("Back", "பின்செல்")) }
            if (path.size > 1) OutlinedButton(onClick = { path = listOf("start") }) { Text(tr("Start over", "மீண்டும் தொடங்கு")) }
        }
    }
}
