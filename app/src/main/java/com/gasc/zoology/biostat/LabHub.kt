package com.gasc.zoology.biostat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private data class Entry(val route: String, val en: String, val ta: String, val descEn: String, val descTa: String)

private val entries = listOf(
    Entry("lab/tools", "Analysis Lab", "பகுப்பாய்வு ஆய்வகம்", "17 calculators: t-tests, chi-square, ANOVA, non-parametric tests, power and more.", "17 கணிப்பான்கள்: t-சோதனை, கை-வர்க்கம், ANOVA, அளபுரு-அற்ற சோதனைகள், ஆற்றல் உள்ளிட்டவை."),
    Entry("lab/tree", "Test decision tree", "சோதனைத் தேர்வு முடிவு மரம்", "Answer a few questions to choose the right statistical test.", "சில கேள்விகளுக்குப் பதிலளித்துச் சரியான புள்ளியியல் சோதனையைத் தேர்ந்தெடுக்கவும்."),
    Entry("lab/visuals", "Interactive visuals", "ஊடாடும் காட்சிகள்", "Sampling distribution, normal curve, regression and Q–Q demonstrations.", "மாதிரிப் பரவல், இயல்பு வளைவு, பின்னடைவு, Q–Q செயல்விளக்கங்கள்."),
    Entry("lab/data", "Data lab", "தரவு ஆய்வகம்", "Thirteen biological datasets with guided analysis.", "வழிகாட்டப்பட்ட பகுப்பாய்வுடன் பதின்மூன்று உயிரியல் தரவுத்தொகுப்புகள்."),
    Entry("lab/formulas", "Formula explorer", "சூத்திரத் தேடல்", "All 43 formulae with meanings, searchable.", "பொருளுடன் அனைத்து 43 சூத்திரங்கள் — தேடலாம்."),
    Entry("lab/toolkit", "Student toolkit", "மாணவர் கருவித்தொகுப்பு", "Mnemonics, extra problems and a formula cheat-sheet.", "நினைவுத் துணுக்குகள், கூடுதல் கணக்குகள், சூத்திரக் குறிப்புத்தாள்."),
    Entry("lab/progress", "Progress backup", "முன்னேற்றக் காப்பு", "Export or import your progress as a JSON file.", "உங்கள் முன்னேற்றத்தை JSON கோப்பாக ஏற்றுமதி/இறக்குமதி செய்யவும்.")
)

@Composable
fun LabHubScreen(onOpen: (String) -> Unit) {
    val ta = LocalTamil.current
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(tr("Laboratory", "ஆய்வகம்"), style = MaterialTheme.typography.titleLarge)
        entries.forEach { e ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(e.route) }.testTag("hub-" + e.route.substringAfter('/'))) {
                Column(Modifier.padding(14.dp)) {
                    Text(if (ta) e.ta else e.en, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(if (ta) e.descTa else e.descEn, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
