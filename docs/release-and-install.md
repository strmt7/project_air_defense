# Release And Install Notes

## Why Updates Failed Before
The production package id `com.airdefense.game` was previously allowed to build without a stable release keystore by falling back to debug signing. That creates APKs which may install cleanly on one machine but fail to update an existing install built elsewhere.

Android update rules are strict:
- Same package id
- Same signing certificate lineage
- Same or higher `versionCode`

If any one of those changes, in-place update fails.

## Current Safeguards
- `release` no longer falls back to debug signing.
- `local` and `debug` use separate package suffixes.
- `versionCode` is no longer hard-coded; it is derived from `APP_VERSION_CODE` or git commit count.
- If git metadata is unavailable on a machine, the build falls back to a date-based version code so local installs still work.
- `printAppIdentity` reports package ids and versions before building.

## Recommended Build Paths
- Production/update-safe build:
  set `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`
  then run `.\gradlew.bat :android:printAppIdentity :android:printReleaseSigningSource :android:assembleRelease`
- Local side-load testing:
  run `.\gradlew.bat :android:assembleLocal`
  this produces the `.local` package, not the production package
- Debug QA on emulator or device:
  run `.\gradlew.bat :core:test :android:installDebug`
  then launch `com.airdefense.game.debug`
- Headless balance QA:
  run `.\gradlew.bat :core:runBattleMonteCarlo -Pruns=300 -Pwaves=1 -Pseconds=48 -Pstep=0.05`
- Windows wrapper for the same shared simulation:
  run `.\scripts\run-battle-monte-carlo.cmd 300 1 48 0.05 20260411`

## CI Behavior
- CI now publishes the `local` artifact by default.
- Do not treat the CI `local` APK as a production update candidate.

## Manual Diagnosis Checklist
If a user reports "App not installed" or update failure:
1. Compare installed package id vs APK package id.
2. Compare signing certificate fingerprints.
3. Compare installed `versionCode` vs APK `versionCode`.
4. Confirm the APK was built from the expected channel (`release` vs `local` vs `debug`).
5. Confirm the user is not trying to update a `release` install with a `local` or `debug` build.
