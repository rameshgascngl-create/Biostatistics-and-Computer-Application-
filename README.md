# Biostatistics Mastery — Android App

Student-oriented offline biostatistics and computer-applications learning app for Zoology & Life Sciences (English + Tamil).

- Package: `com.gasc.zoology.biostat` · Version 5.0.0 (versionCode 6) · target SDK 36 · min SDK 23
- No Android permissions; fully offline.

## Build
1. Open the **Actions** tab → **Build Android APK and AAB** → *Run workflow* (it also runs on every pull request).
2. Download `Biostatistics-debug-APK` (installable test build) or `Biostatistics-release-AAB` (Google Play upload).
3. Signed release builds need the four `BIOSTAT_*` repository secrets — see `docs/PLAY_STORE_CHECKLIST.md`.

## Tests
`node tests/test_calculators.js app/src/main/assets/index.html` — checks the calculator cores against SciPy reference values.

## Docs
- `docs/PLAY_STORE_CHECKLIST.md` — submission steps and store-listing text
- `docs/PRIVACY_POLICY.md` — policy draft (fill in the placeholders and host it)

## Note
All app content lives in `app/src/main/assets/index.html`.
