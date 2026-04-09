#!/usr/bin/env bash
set -euo pipefail

# Builds an APK with channel separation:
# - `release` uses RELEASE_* signing and keeps the update-safe production package id.
# - `local` uses debug signing with a `.local` applicationId suffix for safe side-by-side installs.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -n "${RELEASE_STORE_FILE:-}" && -n "${RELEASE_STORE_PASSWORD:-}" && -n "${RELEASE_KEY_ALIAS:-}" && -n "${RELEASE_KEY_PASSWORD:-}" ]]; then
  echo "[build] Using production signing from environment variables."
  ./gradlew :android:printAppIdentity :android:printReleaseSigningSource :android:assembleRelease
  echo "[build] APK output: android/build/outputs/apk/release/"
else
  echo "[build] RELEASE_* variables not fully set; building LOCAL sideload APK with package suffix .local."
  ./gradlew :android:printAppIdentity :android:assembleLocal
  echo "[build] APK output: android/build/outputs/apk/local/"
fi
