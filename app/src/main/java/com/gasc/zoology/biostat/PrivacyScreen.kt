package com.gasc.zoology.biostat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

const val PRIVACY_UPDATED = "9 November 2026"
const val PRIVACY_CONTACT = "rameshgascngl@gmail.com"

private data class PolicySection(val en: String, val ta: String, val bodyEn: List<String>, val bodyTa: List<String>)

private val POLICY = listOf(
    PolicySection(
        "Summary", "சுருக்கம்",
        listOf("This app works completely offline. It does not collect, transmit, sell or share any personal information, and it contains no accounts, advertising, analytics or tracking."),
        listOf("இந்தச் செயலி முழுமையாக இணையமின்றிச் செயல்படுகிறது. இது எந்தத் தனிப்பட்ட தகவலையும் சேகரிக்கவோ, அனுப்பவோ, விற்கவோ, பகிரவோ செய்வதில்லை. கணக்குகள், விளம்பரங்கள், பகுப்பாய்வு, கண்காணிப்பு எதுவும் இதில் இல்லை.")
    ),
    PolicySection(
        "Who we are", "நாங்கள் யார்", 
        listOf("Biostatistics and Computer Application is an educational app created by Dr. R. Ramesh, Department of Zoology, Government Arts and Science College (GASC), Nagercoil, Tamil Nadu, India, for undergraduate students."),
        listOf("உயிரிப் புள்ளியியலும் கணினிப் பயன்பாடும் என்பது தமிழ்நாடு, நாகர்கோவில், அரசு கலை மற்றும் அறிவியல் கல்லூரி (GASC) விலங்கியல் துறையின் முனைவர் ஆர். ரமேஷ் அவர்களால் இளநிலை மாணவர்களுக்காக உருவாக்கப்பட்ட கல்விச் செயலி.")
    ),
    PolicySection(
        "Information we collect", "நாங்கள் சேகரிக்கும் தகவல்",
        listOf("None. We have no servers and the app has no way to send data to us or to anyone else. The app does not request the internet permission, and it requests no permission for your contacts, location, camera, microphone, photos, files or device identifiers."),
        listOf("எதுவுமில்லை. எங்களிடம் சேவையகங்கள் இல்லை; உங்கள் தரவை எங்களுக்கோ வேறு எவருக்கோ அனுப்பும் வழியும் செயலியில் இல்லை. இணைய அனுமதி கோரப்படுவதில்லை; தொடர்புகள், இருப்பிடம், கேமரா, ஒலிவாங்கி, படங்கள், கோப்புகள், சாதன அடையாளங்காட்டிகளுக்கான அனுமதிகளும் கோரப்படுவதில்லை.")
    ),
    PolicySection(
        "Data stored on your device", "உங்கள் சாதனத்தில் சேமிக்கப்படும் தரவு",
        listOf(
            "To let you continue where you left off, the app stores the following only in its private storage on your device:",
            "Lessons you completed, bookmarks, and your own notes.",
            "Flashcards you marked as known, quiz answers and attempt counts.",
            "Your language choice (English or Tamil).",
            "This data never leaves your device unless you export it yourself (see below). Notes are plain text that you type; please do not store sensitive personal information in them. Automatic cloud backup is switched off for this app. Uninstalling the app, deletes this data; Reset on the Progress backup screen clears progress, bookmarks and notes."
        ),
        listOf(
            "நீங்கள் நிறுத்திய இடத்திலிருந்து தொடர, பின்வருவன உங்கள் சாதனத்தில் செயலியின் தனிப்பட்ட சேமிப்பகத்தில் மட்டுமே சேமிக்கப்படுகின்றன:",
            "நீங்கள் முடித்த பாடங்கள், புத்தகக்குறிகள், உங்கள் சொந்தக் குறிப்புகள்.",
            "அறிந்ததாகக் குறித்த அட்டைகள், வினாடி வினா விடைகள், முயற்சி எண்ணிக்கை.",
            "உங்கள் மொழித் தேர்வு (ஆங்கிலம் அல்லது தமிழ்).",
            "நீங்களாக ஏற்றுமதி செய்தாலன்றி (கீழே காண்க) இத்தரவு உங்கள் சாதனத்தை விட்டு வெளியேறாது. குறிப்புகள் நீங்கள் தட்டச்சு செய்யும் எளிய உரை; முக்கியமான தனிப்பட்ட தகவலை அவற்றில் சேமிக்க வேண்டாம். இச்செயலிக்குத் தானியங்கி மேகக் காப்பு முடக்கப்பட்டுள்ளது. செயலியை நீக்குவதால் இத்தரவு அழிக்கப்படும்; முன்னேற்றக் காப்புத் திரையில் மீட்டமைப்பது முன்னேற்றம், புத்தகக்குறிகள், குறிப்புகளை அழிக்கும்."
        )
    ),
    PolicySection(
        "Export and import", "ஏற்றுமதி, இறக்குமதி",
        listOf("If you choose Export, the app writes a JSON file to a location you pick using Android's system file picker. If you choose Import, you pick a file and the app reads only that file. The app never accesses other files, and what you do with an exported file (for example sharing it) is up to you. Imported files are checked and rejected if they are not valid progress data."),
        listOf("ஏற்றுமதி என்பதைத் தேர்ந்தால், ஆண்ட்ராய்டின் கோப்புத் தேர்வியில் நீங்கள் குறிப்பிடும் இடத்தில் செயலி ஒரு JSON கோப்பை எழுதும். இறக்குமதி என்பதைத் தேர்ந்தால், நீங்கள் தேர்ந்த கோப்பை மட்டுமே செயலி படிக்கும். வேறு கோப்புகளை அது அணுகாது; ஏற்றுமதி செய்த கோப்பை நீங்கள் என்ன செய்கிறீர்கள் (எ.கா. பகிர்தல்) என்பது உங்கள் விருப்பம். இறக்குமதிக் கோப்புகள் சரிபார்க்கப்படும்; செல்லுபடியான முன்னேற்றத் தரவு அல்லாதவை நிராகரிக்கப்படும்.")
    ),
    PolicySection(
        "Calculator inputs", "கணிப்பான் உள்ளீடுகள்",
        listOf("Numbers you type into the Analysis Lab are processed on your device only and are not stored after you leave the screen."),
        listOf("பகுப்பாய்வு ஆய்வகத்தில் நீங்கள் உள்ளிடும் எண்கள் உங்கள் சாதனத்திலேயே செயலாக்கப்படும்; திரையை விட்டு வெளியேறிய பின் சேமிக்கப்படாது.")
    ),
    PolicySection(
        "Third parties", "மூன்றாம் தரப்பினர்",
        listOf("The app includes no third-party advertising, analytics, crash-reporting or tracking software, and shares nothing with third parties. Google Play, if you install through it, has its own privacy practices that apply to your use of the store."),
        listOf("செயலியில் மூன்றாம் தரப்பு விளம்பர, பகுப்பாய்வு, பிழை-அறிக்கை, கண்காணிப்பு மென்பொருள் எதுவும் இல்லை; மூன்றாம் தரப்பினருடன் எதுவும் பகிரப்படுவதில்லை. கூகுள் பிளே வழியாக நிறுவினால், அங்காடியின் பயன்பாட்டுக்குப் பிளேயின் சொந்தத் தனியுரிமை நடைமுறைகள் பொருந்தும்.")
    ),
    PolicySection(
        "Children", "குழந்தைகள்",
        listOf("The app is intended for college students and is not directed at children under 13. Because it collects no personal information, it collects none from children either."),
        listOf("இச்செயலி கல்லூரி மாணவர்களுக்கானது; 13 வயதுக்குட்பட்ட குழந்தைகளை இலக்காகக் கொண்டதல்ல. தனிப்பட்ட தகவல் எதுவும் சேகரிக்கப்படாததால் குழந்தைகளிடமிருந்தும் சேகரிக்கப்படுவதில்லை.")
    ),
    PolicySection(
        "Your control", "உங்கள் கட்டுப்பாடு",
        listOf("All data is on your device and under your control: edit or delete notes and bookmarks in the app, use Reset on the Progress backup screen, or uninstall the app to remove everything."),
        listOf("அனைத்துத் தரவும் உங்கள் சாதனத்திலேயே உங்கள் கட்டுப்பாட்டில் உள்ளது: செயலியில் குறிப்புகள், புத்தகக்குறிகளைத் திருத்தலாம்/நீக்கலாம்; முன்னேற்றக் காப்புத் திரையில் மீட்டமைக்கலாம்; அல்லது செயலியை நீக்கி அனைத்தையும் அகற்றலாம்.")
    ),
    PolicySection(
        "Changes to this policy", "இக்கொள்கையில் மாற்றங்கள்",
        listOf("If a future version changes how data is handled, this policy will be updated in the app before the change takes effect. The date below shows the latest revision."),
        listOf("எதிர்காலப் பதிப்பில் தரவு கையாளும் முறை மாறினால், அம்மாற்றம் நடைமுறைக்கு வரும் முன் இக்கொள்கை செயலியிலேயே புதுப்பிக்கப்படும். கீழுள்ள தேதி சமீபத்திய திருத்தத்தைக் காட்டுகிறது.")
    ),
    PolicySection(
        "Contact", "தொடர்புக்கு",
        listOf("Questions about this policy: $PRIVACY_CONTACT"),
        listOf("இக்கொள்கை குறித்த கேள்விகளுக்கு: $PRIVACY_CONTACT")
    )
)

@Composable
fun PrivacyScreen() {
    val ta = LocalTamil.current
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp).testTag("privacy"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("Privacy Policy", "தனியுரிமைக் கொள்கை"), style = MaterialTheme.typography.titleLarge)
        Text(tr("Last updated: $PRIVACY_UPDATED", "கடைசியாகப் புதுப்பிக்கப்பட்டது: $PRIVACY_UPDATED"), style = MaterialTheme.typography.bodySmall)
        POLICY.forEach { s ->
            SectionCard(if (ta) s.ta else s.en) {
                val body = if (ta) s.bodyTa else s.bodyEn
                if (s.en == "Data stored on your device") { Para(body[0]); Bullets(body.subList(1, 4)); Para(body[4]) }
                else body.forEach { Para(it) }
            }
        }
    }
}
