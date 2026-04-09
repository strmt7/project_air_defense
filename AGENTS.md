# Project Air Defense: AI Agent Guide

## Mission
- Maintain an Android/libGDX 3D missile-defense prototype with a strong emphasis on update-safe APKs, visible combat feedback, and a believable high-end night scene.
- Prefer correctness over novelty. Do not change signing, package identity, or versioning behavior casually.

## Repository Layout
- `core/src/main/kotlin/com/airdefense/game/AirDefenseGame.kt`
  Entry point. Boots the menu screen.
- `core/src/main/kotlin/com/airdefense/game/StartScreen.kt`
  Lightweight textured title/menu screen. Keep this cheap to initialize.
- `core/src/main/kotlin/com/airdefense/game/BattleScreen.kt`
  Main combat scene, rendering setup, HUD, spawning, impacts, destruction, and effects.
- `core/src/main/kotlin/com/airdefense/game/BattleLogic.kt`
  Pure gameplay math for wave sizing, spawn timing, interception, damage, and threat launch profiles.
- `core/src/main/kotlin/com/airdefense/game/NightShader.kt`
  Custom mobile-friendly lit shader using diffuse textures, roughness maps, emissive glow, and fog.
- `core/src/test/kotlin/com/airdefense/game/*`
  Pure Kotlin tests. Add new logic here before adding more screen complexity.
- `android/assets/textures/*`
  Runtime art assets used by the menu and battle scene.
- `android/assets/references/*`
  Source/reference images only. Do not ship huge references unless necessary.
- `android/build.gradle.kts`
  Package identity, versioning, build channels, and signing rules.
- `scripts/build-installable-apk.sh`
  Human-facing build entry point. Must stay aligned with `android/build.gradle.kts`.

## Non-Negotiable Build Rules
- `release` is the update-safe production channel and must only be built with real `RELEASE_*` signing inputs.
- `local` is the debug-signed sideload channel and must keep the `.local` package suffix.
- `debug` keeps the `.debug` package suffix.
- Never reintroduce a debug-signed artifact under the production package id `com.airdefense.game`.
- Keep `versionCode` monotonic. Current strategy derives it from `APP_VERSION_CODE` or git commit count.

## Rendering Contract
- This repo does not use real hardware ray tracing.
- Quality target: approximate premium night rendering with texture detail, roughness response, emissive glow, specular highlights, fog, and stable mobile performance.
- All world materials should have:
  - a diffuse/albedo texture
  - a roughness texture or roughness fallback
  - a diffuse/specular/emissive profile where relevant
- Avoid high-poly procedural geometry explosions in menu or battle initialization.

## Performance Rules
- Hot-path update/render code should avoid per-frame allocations when a reusable vector/matrix/texture region will do.
- Spawn-time allocations are acceptable when infrequent.
- Do not decode large source images at runtime when a downscaled runtime texture already exists.
- Prefer extracting pure logic into `BattleLogic.kt` instead of adding more branching inside `BattleScreen.kt`.

## Testing Rules
- Any change to interception, damage, wave timing, spawn rules, or install metadata should add or update tests.
- Prefer testing pure logic helpers over trying to unit-test libGDX screens directly.
- Minimum verification before shipping:
  - `./gradlew :core:compileKotlin :core:test`
  - Android metadata/signing tasks if the SDK is present

## Release/Install Failure Checklist
- If an APK cannot update over an older install, check in this order:
  1. Package id mismatch (`com.airdefense.game` vs `.local` / `.debug`)
  2. Signing key mismatch
  3. Version code not increasing
  4. ABI packaging or manifest regressions
- Historical root cause in this repo:
  release-like APKs were previously allowed to fall back to debug signing, which breaks update continuity across machines/CI environments.

## Asset Provenance
- Night skyline reference: `android/assets/references/tel_aviv_night.jpg`
  Source: Wikimedia Commons, Tel Aviv at night.
- Night sky panorama: `android/assets/textures/sky_panorama_2k.jpg`
  Derived runtime texture from the referenced Wikimedia Commons / Poly Haven panorama.

## Safe Change Strategy
- For gameplay work:
  adjust `BattleLogic.kt` first, then wire the screen.
- For visual work:
  keep shader compatibility and material texture coverage intact.
- For packaging work:
  update docs/scripts/workflow in the same change.
