# Google Play submission checklist — Biostatistics Mastery

Package name: `com.gasc.zoology.biostat` (permanent once published). Current version: `5.0.0` (versionCode 6; increase versionCode for every upload).

## 1. One-time setup
- [ ] Play Console developer account. Publishing under the college name ("GASC", "Department of Zoology") needs an **organisation** account or written authorisation from the college (Play impersonation policy). A new *personal* account must run a closed test with the required number of testers for the required period before production — check the current rule in Play Console.
- [ ] Create the **upload keystore** once and back it up in two places (never commit it):
  `keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
- [ ] Add GitHub repository secrets: `BIOSTAT_KEYSTORE_B64` (`base64 -w0 upload.jks`), `BIOSTAT_KEYSTORE_PASSWORD`, `BIOSTAT_KEY_ALIAS`, `BIOSTAT_KEY_PASSWORD`.
- [ ] Enrol in **Play App Signing** when creating the app (required for new apps).

## 2. Build
Run the *Build Android APK and AAB* workflow. Download:
- `Biostatistics-debug-APK` — install on a phone to test now.
- `Biostatistics-release-AAB` — upload this to Play Console (an AAB is mandatory for new apps).

## 3. Store listing
- [ ] App name (≤30 chars): `Biostatistics Mastery`
- [ ] Short description (≤80): `Offline biostatistics & computer applications for Zoology students (English + Tamil).`
- [ ] Full description: 44 lessons across six units, 176 practice questions, 50 flashcards, statistical calculators (t-tests, ANOVA, χ², non-parametric tests, power, confidence intervals), dyslexia-friendly and high-contrast modes, fully offline.
- [ ] Category: **Education**. Tags: education, statistics, biology.
- [ ] Icon 512×512 PNG, feature graphic 1024×500, at least 2 phone screenshots (take them from the debug build).
- [ ] Privacy policy URL: publish `docs/PRIVACY_POLICY.md` (e.g. GitHub Pages) after filling the placeholders.
- [ ] Contact email (shown publicly on the store page).

## 4. App content declarations
- **Data safety:** no data collected, no data shared; no data is transmitted off the device. (The app declares no Internet permission.)
- **Target audience:** 18+ (university students). Do not select "children" unless you intend to join the Families programme.
- **Ads:** none. **App access:** all functionality available without login.
- **Content rating (IARC):** answer the questionnaire truthfully — educational reference, no violence/sexual content/user-generated sharing.
- **Health apps / medical:** declare it is *not* a medical app; the in-app footer carries an educational-use disclaimer.

## 5. Before pressing "Release"
- [ ] Pre-launch report in Play Console shows no crashes.
- [ ] Tested on Android 8, 12 and 15/16 (edge-to-edge) devices, portrait and landscape, Tamil toggle on.
- [ ] Import/export progress tested with a Tamil note.
- [ ] Back gesture exits the app from the home screen.
