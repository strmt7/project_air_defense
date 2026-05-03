# Project Air Defense

<!-- BEGIN GENERATED BADGES -->
[![Legacy Android APK](https://img.shields.io/github/actions/workflow/status/strmt7/project_air_defense/android-release-apk.yml?branch=main&label=legacy-android-apk)](https://github.com/strmt7/project_air_defense/actions/workflows/android-release-apk.yml)
[![Quality](https://img.shields.io/github/actions/workflow/status/strmt7/project_air_defense/quality.yml?branch=main&label=quality)](https://github.com/strmt7/project_air_defense/actions/workflows/quality.yml)
[![Ktlint](https://img.shields.io/github/actions/workflow/status/strmt7/project_air_defense/ktlint.yml?branch=main&label=ktlint)](https://github.com/strmt7/project_air_defense/actions/workflows/ktlint.yml)
[![caveman](https://img.shields.io/badge/caveman-555?logo=github&labelColor=555)](https://github.com/JuliusBrussee/caveman)
[![andrej-karpathy-skills](https://img.shields.io/static/v1?label=&message=andrej-karpathy-skills&color=555&logo=github&logoColor=white)](https://github.com/forrestchang/andrej-karpathy-skills)
<!-- END GENERATED BADGES -->

A repo in transition from a legacy Android/libGDX prototype to a UE5-first air-defense game with real 3D city ingestion, photoreal material targets, and no static gameplay backdrops.

## Early Alpha Notice

This project is an early alpha test. Many systems are experimental, incomplete, or actively being migrated, and many or most features may not work as intended. The project is provided as-is, without any responsibility or liability for failures, incorrect behavior, data loss, compatibility issues, or other consequences from using or testing it.

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
- UE5 active city pilot: `docs/planning/ue5-city-pilot-helsinki-kalasatama.md`
- UE5 evaluated city candidate: `docs/planning/ue5-city-pilot-3dbag-rotterdam.md`
- UE5 regression root cause log: `docs/planning/ue5-regression-root-cause-2026-04-17.md`
- UE5 visual acceptance: `docs/planning/ue5-visual-acceptance.md`
- UE5 Android packaging: `docs/planning/ue5-android-packaging.md`
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
- The active runtime city pilot uses the local Helsinki Kalasatama 3D Tiles dataset through Cesium for Unreal.
- The offline photoreal shipping path remains official mesh or OBJ source -> Blender cleanup -> UE5 Nanite; Helsinki Kalasatama is also the current best candidate for that bake because an official OBJ mesh exists.
- Direct geospatial runtime rendering via Cesium or ArcGIS is allowed for truth-checking, inspection, and bootstrap validation. It is not automatic permission to skip the later shipping bake.

## UE5 Pilot Tooling
1. Install the verified Cesium for Unreal plugin locally:
   `.\scripts\install-cesium-for-unreal.cmd`
2. Validate the UE5 city pipeline:
   `py -3 .\tools\ue5_city_pipeline\test_ue5_city_pipeline.py`
3. Verify the UE5 bootstrap path headlessly:
   `.\scripts\verify-ue5-bootstrap.cmd`
4. Build one storage-safe packaged Win64 runtime:
   `.\scripts\package-ue5-runtime.cmd`
4b. Build an Android APK from the UE5 runtime (requires UE 5.7, Android NDK r25b, JDK 17, or the manual self-hosted `Package UE5 Android Runtime` workflow; see `docs/planning/ue5-android-packaging.md`):
   `.\scripts\package-ue5-runtime-android.cmd`
5. Capture the packaged runtime:
   `.\scripts\capture-ue5-runtime-screenshot.cmd -Exe packaged/Win64/ProjectAirDefenseUE5.exe`
6. Capture the packaged menu, battle, and systems states serially:
   `.\scripts\run-ue5-mobile-ui-proof.cmd`
7. Capture the editor-game runtime without repackaging:
   `.\scripts\capture-ue5-editor-runtime-screenshot.cmd`
8. Open the UE5 scaffold under `ue5\ProjectAirDefenseUE5\`.

Local Helsinki tooling, when the active source needs to be rebuilt or refreshed:
`.\scripts\generate-ue5-city-pilot.cmd`, `.\scripts\download-ue5-city-source.cmd`, `.\scripts\prepare-ue5-city-tiles.cmd`, and `.\scripts\upgrade-ue5-city-tiles.cmd`.

## Verified UE5 Toolchain
- Unreal Engine `5.7`
- Blender `5.1.0`
- Cesium for Unreal `2.25.0`

## Verified UE5 Runtime Controls
- Default runtime city source: local Helsinki Kalasatama 3D Tiles
- Runtime time controls: systems drawer `TIME` controls step solar hour, slow/fast cycle rate, and pause/resume day/night progression through `ACesiumSunSky`.
- Runtime camera controls: `W/A/S/D` or arrow keys to pan, `Q/E` to yaw, `T/G` or `Numpad8/Numpad5` to pitch, `PageUp/PageDown` or mouse wheel to zoom, `R/F` to raise/lower, `Home` to reset
- Runtime graphics settings backend: `UProjectAirDefenseGameUserSettings` owns AA method, AO, motion blur, ray-tracing request state, and UE scalability groups on startup for both editor-game and packaged runtime lanes; those toggles are no longer hard-forced in `DefaultEngine.ini`
- Runtime camera settings remain authored in meters in `UProjectAirDefenseRuntimeSettings`, but `AProjectAirDefenseCityCameraPawn` converts them to Unreal units internally. This avoids the 100x framing error that produced roof-level closeups from Cesium tileset bounds.

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
- Runtime 3D Tiles dataset:
  [Helsinki Kalasatama 3D Tiles](https://3d.hel.ninja/data/mesh/Kalasatama/Helsinki3D_MESH_Kalasatama_2017_3D_Tiles_ZIP.zip), licensed under [CC BY 4.0](https://www.hel.fi/en/decision-making/information-on-helsinki/maps-and-geospatial-data/helsinki-3d).
  Required credit: City of Helsinki / Helsinki 3D.
  The game loads the local prepared dataset through Cesium for Unreal.
- Evaluated remote 3D Tiles candidate:
  [3DBAG Rotterdam LoD2.2](https://data.3dbag.nl/v20250903/cesium3dtiles/lod22/tileset.json), licensed under [CC BY 4.0](https://docs.3dbag.nl/en/copyright/).
  Required credit: `(c) 3DBAG by tudelft3d and 3DGI`.

## Security And Performance
- **ProGuard/R8 enabled**: minification and obfuscation for production security.
- **Adaptive mobile renderer**: GLES 2.0-safe path with quality-tier control and selective MSAA.
- **Optimized memory**: minimal allocations in the main loop using math buffers and pooled entities.
- **Strict repositories**: only Google and Maven Central are used for dependency resolution.
- **Mandatory Kotlin format/lint**: KtLint is the formatting gate; Detekt remains the semantic/code-smell audit.
- **Report-oriented benchmark suite**: build timing, startup, battle entry, runtime frame telemetry, runtime health capture, simulation sweeps, KtLint, lint, detekt, dependency health, and APK size snapshots.

## Toolchain
- Android Gradle Plugin `9.2.0`
- Gradle `9.5.0`
- Kotlin JVM plugin `2.3.21`
- Detekt `2.0.0-alpha.3`
- libGDX `1.14.0`
- Java `21`
- Compile / target SDK `36`

Version source map: [docs/reference/toolchain-source-map.md](docs/reference/toolchain-source-map.md).

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
These Gradle outputs are the legacy Android/libGDX prototype only. They do not package the UE5 runtime, Cesium, or the local Helsinki 3D Tiles terrain.

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
This policy applies to legacy Gradle APKs. UE5 Android packages use the separate package id `com.airdefense.game.ue5` and the UE5 packaging path documented above.

- `release`
  Production package id `com.airdefense.game`. Must use the stable release keystore.
- `local`
  Debug-signed package id `com.airdefense.game.local`. Safe for local side-load testing.
- `debug`
  Debug-signed package id `com.airdefense.game.debug`.

## Notes
- This is still a gameplay prototype, not a military simulator.
- Static background images are not allowed in the gameplay battlespace going forward.
- The current UE5 city proof path is real local Helsinki 3D Tiles data through Cesium for Unreal. It is not a static panorama and not a fake skyline.
- The GitHub `Build Legacy Android APK` artifact is the outgoing libGDX prototype and is not proof of the UE5 city runtime; use the UE5 packaging script or manual self-hosted UE5 workflow for terrain APKs.
- The official Helsinki 3D Tiles package requires an explicit Cesium `3d-tiles-tools upgrade` pass before packaged runtime use because the upstream district payload contains legacy `b3dm` content.
- Keep one canonical raw Helsinki archive under `data/external/downloads/` and one active runtime dataset, then remove temporary backups after verification.
- `.\scripts\package-ue5-runtime.ps1` removes `ue5/ProjectAirDefenseUE5/Saved/StagedBuilds` after a successful archive by default because it is a duplicate of the packaged build.
- `.\scripts\run-ue5-mobile-ui-proof.ps1` is the trusted packaged UI proof lane because it runs menu, battle, and systems captures serially under a per-build mutex. Do not launch parallel packaged captures against the same build.
- `.\scripts\capture-ue5-editor-runtime-screenshot.ps1` is the low-waste iteration lane. Use editor-game capture first; repackage only after a verified scene or runtime change worth shipping.
- The current pilot uses the native UE5/Cesium exposure path by default. `PostExposureBias` only becomes an override when intentionally set to a non-zero value after measured visual proof.
- The current renderer targets a high-quality mobile approximation of premium night lighting. Ray tracing is opt-in, off by default, and not the mobile baseline.
- Waterfront building placement and radar orientation are now validated in tests so the city stays inland and the radar reads top-down toward the horizon.
- Android is intentionally wide-only. `AndroidLauncher` stays on `sensorLandscape`, and the manifest carries a targeted `DiscouragedApi` lint suppression on that activity because the game is not intended to support portrait operation.
- Android quality selection is now capability-based. High-capacity Android 15 emulator lanes can resolve above `PERFORMANCE`, while genuinely low-memory devices still fall back safely.
- AI-agent routing is intentionally compact and generated/verified in-repo so external agents do not keep rediscovering the same workflow.
- The repo's concise agent overlay is pinned to [caveman `v1.5.0`](https://github.com/JuliusBrussee/caveman/releases/tag/v1.5.0). Upstream `v1.5.0` adds configurable default mode, `off`, and `/caveman-help`; this repo intentionally keeps only the mandatory instruction-only `lite`-equivalent overlay.
- Generated `build/`, `.kotlin/`, and copied native-library output are intentionally excluded from git so the repository stays source-clean.
