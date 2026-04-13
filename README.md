# Project Air Defense

<!-- BEGIN GENERATED BADGES -->
[![Android APK](https://img.shields.io/github/actions/workflow/status/strmt7/project_air_defense/android-release-apk.yml?branch=main&label=android-apk)](https://github.com/strmt7/project_air_defense/actions/workflows/android-release-apk.yml)
[![Quality](https://img.shields.io/github/actions/workflow/status/strmt7/project_air_defense/quality.yml?branch=main&label=quality)](https://github.com/strmt7/project_air_defense/actions/workflows/quality.yml)
[![Ktlint](https://img.shields.io/github/actions/workflow/status/strmt7/project_air_defense/ktlint.yml?branch=main&label=ktlint)](https://github.com/strmt7/project_air_defense/actions/workflows/ktlint.yml)
[![caveman](https://img.shields.io/badge/caveman-555?logo=github&labelColor=555)](https://github.com/JuliusBrussee/caveman)
<!-- END GENERATED BADGES -->

A repo in transition from a legacy Android/libGDX prototype to a UE5-first air-defense game with real 3D city ingestion, photoreal material targets, and no static gameplay backdrops.

## AI-Friendly Docs
- Agent operating guide: `AGENTS.md`
- Docs index: `docs/index.md`
- Agent routing: `docs/reference/ai-agent-context-routing.md`
- Agent skills: `docs/reference/ai-agent-skills.md`
- Agent integrations: `docs/reference/ai-agent-integrations.md`
- Agent web research stack: `docs/reference/ai-agent-web-research-stack.md`
- Agent upstream sources: `docs/reference/ai-agent-upstream-sources.md`
- UE5 engine mandate: `docs/planning/ue5-engine-mandate.md`
- UE5 city sourcing plan: `docs/planning/ue5-city-model-strategies.md`
- UE5 pilot district: `docs/planning/ue5-city-pilot-helsinki-kalasatama.md`
- UE5 visual acceptance: `docs/planning/ue5-visual-acceptance.md`
- Architecture reference: `docs/architecture.md`
- Release/install behavior: `docs/release-and-install.md`
- Benchmark suite: `docs/benchmark-suite.md`
- Benchmark sources: `docs/benchmark-sources.md`
- Core detekt priorities: `docs/core-detekt-priorities.md`
- Android visual QA: `docs/android-visual-qa.md`
- Android visual QA sources: `docs/reference/android-visual-qa-sources.md`
- Popular 3D Android game workflows: `docs/popular-3d-android-game-workflows.md`
- Level asset pipeline: `docs/level-asset-pipeline.md`
- Level asset source map: `docs/level-asset-source-map.md`
- Runtime asset attribution: `android/assets/ATTRIBUTION.md`
- UE5 city pipeline skill: `.agents/skills/ue5-city-pipeline/SKILL.md`
- UE5 photoreal scene skill: `.agents/skills/ue5-photoreal-city-scene/SKILL.md`

## Current Direction
- UE5 is the only permitted engine for new runtime, rendering, tooling, editor, prototype, and shipping work.
- The Android/libGDX codebase remains in-repo only as migration input and gameplay reference.
- The active runtime city pilot is Helsinki Kalasatama using the official 3D Tiles package and Cesium for Unreal.
- The photoreal shipping path for the same district remains official mesh -> Blender cleanup -> UE5 Nanite.
- Direct geospatial runtime rendering via Cesium or ArcGIS is allowed for truth-checking, inspection, and bootstrap validation. It is not automatic permission to skip the later shipping bake.

## UE5 Pilot Tooling
1. Install the verified Cesium for Unreal plugin locally:
   `.\scripts\install-cesium-for-unreal.cmd`
2. Generate the checked-in pilot manifest:
   `.\scripts\generate-ue5-city-pilot.cmd`
3. Dry-run the official source download:
   `.\scripts\download-ue5-city-source.cmd`
4. Prepare the extracted 3D Tiles package and generate the master `tileset.json`:
   `.\scripts\prepare-ue5-city-tiles.cmd`
5. Upgrade the official Helsinki 3D Tiles runtime package in place:
   `.\scripts\upgrade-ue5-city-tiles.cmd`
6. Validate the UE5 pipeline:
   `py -3 .\tools\ue5_city_pipeline\test_ue5_city_pipeline.py`
7. Verify the UE5 bootstrap path headlessly:
   `.\scripts\verify-ue5-bootstrap.cmd`
8. Build one storage-safe packaged runtime:
   `.\scripts\package-ue5-runtime.cmd`
9. Capture the packaged runtime:
   `.\scripts\capture-ue5-runtime-screenshot.cmd -Exe packaged/Win64/ProjectAirDefenseUE5.exe`
10. Capture the editor-game runtime without repackaging:
   `.\scripts\capture-ue5-editor-runtime-screenshot.cmd`
11. Open the UE5 scaffold under `ue5\ProjectAirDefenseUE5\`.

## Verified UE5 Toolchain
- Unreal Engine `5.7`
- Blender `5.1.0`
- Cesium for Unreal `2.25.0`

## Verified UE5 Runtime Controls
- Default runtime city source: local upgraded Helsinki Kalasatama 3D Tiles
- Runtime camera controls: `W/A/S/D` or arrow keys to pan, `Q/E` to rotate, `PageUp/PageDown` or mouse wheel to zoom, `R/F` to raise/lower, `Home` to reset
- Runtime graphics settings backend: `UProjectAirDefenseGameUserSettings` now applies AA method, AO, motion blur, and UE scalability groups on startup for both editor-game and packaged runtime lanes

## Legacy Android Prototype Snapshot
These bullets describe the outgoing Android/libGDX prototype that remains in-repo for migration reference.

- **Fire-control loop, not point-and-shoot**: scan -> track table -> prioritize -> salvo engage -> terminal intercept.
- **Live doctrine tuning**: engagement range, interceptor speed, launch cooldown, radar refresh interval, blast radius, and salvo size are adjustable while fighting.
- **Mixed raids**: ballistic, cruise-like, decoy, and anti-radiation missile profiles.
- **Counter-battery pressure**: about 1 in 20 threat missiles is anti-radiation and can directly damage radar / ECS / launcher capacity.
- **Shared simulation core**: the GUI battle and the headless Monte Carlo runner now use the same missile, interceptor, damage, and wave logic.

## Legacy Android Visual Snapshot
These bullets describe the outgoing renderer only. They are not the target UE5 city scene.

- The old Android renderer uses a textured skyline backdrop and night panorama. That path is deprecated and not permitted in the future UE5 gameplay view.
- Procedural surface materials with diffuse texture detail and roughness response for buildings, roads, metal equipment, debris, and projectiles.
- Dynamic trails, blasts, camera shake, and persistent damage: buildings darken, lean, collapse in height, and emit debris when hit.
- Gameplay-first projectile readability: hostile missiles and interceptors are intentionally oversized and color-separated so they stay legible on mobile screens.

## Public References Used For Design Direction
- RTX Patriot overview: https://www.rtx.com/raytheon/what-we-do/integrated-air-and-missile-defense/patriot
- Lockheed PAC-3 product page: https://www.lockheedmartin.com/en-us/products/pac-3-advanced-capability-3.html
- U.S. Army Patriot article: https://www.army.mil/article/171144/patriot_missile_system

## Imported Open Assets
- `android/assets/models/engel_house.obj`
  Imported from the MIT-licensed [Ladybug Tools / 3D Models](https://github.com/ladybug-tools/3d-models) repository.
  Source asset: `obj/engel-house/AngelHouse_Bauhaus-in-Israel-r2.obj`
  Reference notes in the source repo identify it as Engel House / Beit Engel, a Bauhaus-in-Israel model associated with Tel Aviv.

## Security And Performance
- **ProGuard/R8 enabled**: minification and obfuscation for production security.
- **Adaptive mobile renderer**: GLES 2.0-safe path with quality-tier control and selective MSAA.
- **Optimized memory**: minimal allocations in the main loop using math buffers and pooled entities.
- **Strict repositories**: only Google and Maven Central are used for dependency resolution.
- **Mandatory Kotlin format/lint**: KtLint is the formatting gate; Detekt remains the semantic/code-smell audit.
- **Report-oriented benchmark suite**: build timing, startup, battle entry, runtime frame telemetry, runtime health capture, simulation sweeps, KtLint, lint, detekt, dependency health, and APK size snapshots.

## Toolchain
- Android Gradle Plugin `9.1.0`
- Gradle `9.4.1`
- Kotlin JVM plugin `2.3.20`
- libGDX `1.14.0`
- Java `21`
- Compile / target SDK `36`

## Local Development
Android steps below are legacy maintenance and migration-reference only until the UE5 runtime replaces them.

1. Open the project in the latest stable Android Studio.
2. Make sure Java 21 and Android SDK 36 are installed.
3. Sync Gradle.
4. Run `.\gradlew.bat ktlintCheck` for the mandatory Kotlin format/lint gate.
5. Run `.\gradlew.bat ktlintFormat` to apply repository formatting.
6. Run `.\gradlew.bat :android:testDebugUnitTest` for Android launch-policy and compatibility unit coverage.
7. Run `.\gradlew.bat :core:test` for gameplay and targeting logic coverage.
8. Run `.\gradlew.bat :core:runBattleMonteCarlo -Pruns=300 -Pwaves=1 -Pseconds=48 -Pstep=0.05` for headless balance sweeps.
9. Or use `.\scripts\run-battle-monte-carlo.cmd 300 1 48 0.05 20260411` on Windows without changing PowerShell execution policy.
10. Run `.\scripts\run-benchmark-suite.cmd` for the report-oriented benchmark suite.
   It captures build timings, startup macrobenchmarks, runtime frame telemetry, runtime-health artifacts, Monte Carlo balance metrics, and standards reports under `benchmark-results/`.
11. Use a current Android 15 / API 35 Google APIs emulator. The verified repo lane is Pixel 9 Pro (`airdefense_android15_pixel9pro`).
12. Run `powershell -ExecutionPolicy Bypass -File .\tools\android_visual_qa\bootstrap.ps1` once on a Windows workstation that will do emulator QA.
13. Run `py -3 .\tools\android_visual_qa\visual_qa.py probe` before screen-driven emulator checks.
14. Run `python3 tools/update_readme_badges.py --check` after badge metadata changes.
15. Run `.\gradlew.bat :android:installDebug` to install the debug package on an emulator or device.
16. Clear any first-boot Android full-screen education overlay before OCR-based menu checks.
17. Keep emulator proof in landscape; this game does not support portrait.
18. Verify navigation with screenshot + OCR before tap, tap-proof JSON, and screenshot + OCR after tap; do not treat a live PID or logcat alone as proof of a usable battle scene.

## Build Outputs
- Local side-load build:
  `.\gradlew.bat :android:assembleLocal`
- Debug install:
  `.\gradlew.bat :android:installDebug`
- Production/update-safe build:
  `.\gradlew.bat :android:printAppIdentity :android:printReleaseSigningSource :android:assembleRelease`

Production signing variables can be set in `~/.gradle/gradle.properties`, CI secrets, or environment variables:
- `RELEASE_STORE_FILE=/absolute/path/to/keystore.jks`
- `RELEASE_STORE_PASSWORD=...`
- `RELEASE_KEY_ALIAS=...`
- `RELEASE_KEY_PASSWORD=...`

## APK Channel Policy
- `release`
  Production package id `com.airdefense.game`. Must use the stable release keystore.
- `local`
  Debug-signed package id `com.airdefense.game.local`. Safe for local side-load testing.
- `debug`
  Debug-signed package id `com.airdefense.game.debug`.

## Notes
- This is still a gameplay prototype, not a military simulator.
- Static background images are not allowed in the gameplay battlespace going forward.
- The current UE5 city proof path is real 3D Tiles data streamed locally through Cesium for Unreal. It is not a static panorama and not a fake skyline.
- The official Helsinki 3D Tiles package requires an explicit Cesium `3d-tiles-tools upgrade` pass before packaged runtime use because the upstream district payload contains legacy `b3dm` content.
- The upgraded runtime path keeps one canonical raw archive under `data/external/downloads/` and one active runtime dataset under `data/external/helsinki_kalasatama_3dtiles/`. Do not keep a long-lived duplicate legacy backup once the upgraded runtime is verified.
- `.\scripts\package-ue5-runtime.ps1` removes `ue5/ProjectAirDefenseUE5/Saved/StagedBuilds` after a successful archive by default because it is a duplicate of the packaged build.
- `.\scripts\capture-ue5-editor-runtime-screenshot.ps1` is the low-waste iteration lane. Use editor-game capture first; repackage only after a verified scene or runtime change worth shipping.
- The current pilot uses the native UE5/Cesium exposure path. Do not force a global unbound post-process exposure override unless a measured regression proves the native path is insufficient.
- The current renderer targets a high-quality mobile approximation of premium night lighting; it does not use true hardware ray tracing.
- Waterfront building placement and radar orientation are now validated in tests so the city stays inland and the radar reads top-down toward the horizon.
- Android is intentionally wide-only. `AndroidLauncher` stays on `sensorLandscape`, and the manifest carries a targeted `DiscouragedApi` lint suppression on that activity because the game is not intended to support portrait operation.
- Android quality selection is now capability-based. High-capacity Android 15 emulator lanes can resolve above `PERFORMANCE`, while genuinely low-memory devices still fall back safely.
- AI-agent routing is intentionally compact and generated/verified in-repo so external agents do not keep rediscovering the same workflow.
- The repo's concise agent overlay is pinned to [caveman `v1.5.0`](https://github.com/JuliusBrussee/caveman/releases/tag/v1.5.0). Upstream `v1.5.0` adds configurable default mode, `off`, and `/caveman-help`; this repo intentionally keeps only the mandatory instruction-only `lite`-equivalent overlay.
- Generated `build/`, `.kotlin/`, and copied native-library output are intentionally excluded from git so the repository stays source-clean.
