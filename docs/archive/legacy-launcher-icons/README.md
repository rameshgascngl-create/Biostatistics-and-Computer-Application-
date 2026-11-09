# Legacy launcher icons (archived)

Illustrated launcher icon (microscope, DNA helix, fish, leaf and distribution curve) taken from the
`Biostatistics_Android_Studio_Project.zip` that was previously kept in the repository root.

These files are **not** part of the build: they live under `docs/`, not under `app/src/main/res/`.
The app currently uses the plain bar-chart launcher icon in `app/src/main/res/mipmap-*`.
To use this artwork instead, copy the `mipmap-*` folders into `app/src/main/res/` (check the
adaptive-icon files in `mipmap-anydpi-v26` first, because they take precedence on Android 8.0+).

The old ZIP also contained a stray copy of the xxxhdpi icon at `res/ic_launcher.png`
(byte-identical to `mipmap-xxxhdpi/ic_launcher.png`) and an earlier snapshot of the app source
(compileSdk 34, INTERNET permission, cleartext traffic allowed), which is fully superseded by the
current project. The ZIP remains retrievable from Git history (commit `bb8c4f2` and earlier).
