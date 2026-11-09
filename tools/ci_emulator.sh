#!/usr/bin/env bash
# Runs the instrumented UI tests on the emulator started by android-emulator-runner and pulls QA screenshots.
set +e
adb shell mkdir -p /sdcard/Download/qa
gradle connectedDebugAndroidTest --stacktrace
status=$?
mkdir -p qa-screens
adb pull /sdcard/Download/qa/. qa-screens/ >/dev/null 2>&1
exit $status
