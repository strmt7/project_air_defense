#!/usr/bin/env bash
set -euo pipefail

# Builds a release APK that is always signed:
# - Uses RELEASE_* signing properties when provided.
# - Falls back to debug signing for local sideload testing.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -n "${RELEASE_STORE_FILE:-}" && -n "${RELEASE_STORE_PASSWORD:-}" && -n "${RELEASE_KEY_ALIAS:-}" && -n "${RELEASE_KEY_PASSWORD:-}" ]]; then
  echo "[build] Using production signing from environment variables."
else
  echo "[build] RELEASE_* variables not fully set; falling back to debug signing for installable local APK."
fi

./gradlew :android:printReleaseSigningSource :android:assembleRelease

echo "[build] APK output: android/build/outputs/apk/release/"
