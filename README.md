# Project Air Defense (Android 3D)

<!-- BEGIN GENERATED BADGES -->
[![Android APK](https://img.shields.io/github/actions/workflow/status/strmt7/project_air_defense/android-release-apk.yml?branch=main&label=android-apk)](https://github.com/strmt7/project_air_defense/actions/workflows/android-release-apk.yml)
[![Quality](https://img.shields.io/github/actions/workflow/status/strmt7/project_air_defense/quality.yml?branch=main&label=quality)](https://github.com/strmt7/project_air_defense/actions/workflows/quality.yml)
[![Ktlint](https://img.shields.io/github/actions/workflow/status/strmt7/project_air_defense/ktlint.yml?branch=main&label=ktlint)](https://github.com/strmt7/project_air_defense/actions/workflows/ktlint.yml)
[![Caveman](https://img.shields.io/badge/Caveman-555?logo=github&labelColor=555)](https://github.com/JuliusBrussee/caveman/releases/tag/v1.5.0)
<!-- END GENERATED BADGES -->

A high-fidelity libGDX + Kotlin Android prototype where you command a Patriot-inspired battery in a living 3D battlespace.

## AI-Friendly Docs
- Agent operating guide: `AGENTS.md`
- Agent routing: `docs/reference/ai-agent-context-routing.md`
- Agent skills: `docs/reference/ai-agent-skills.md`
- Agent upstream sources: `docs/reference/ai-agent-upstream-sources.md`
- Architecture reference: `docs/architecture.md`
- Release/install behavior: `docs/release-and-install.md`
- Benchmark suite: `docs/benchmark-suite.md`
- Benchmark sources: `docs/benchmark-sources.md`
- Popular 3D Android game workflows: `docs/popular-3d-android-game-workflows.md`
- Level asset pipeline: `docs/level-asset-pipeline.md`
- Level asset source map: `docs/level-asset-source-map.md`
- Runtime asset attribution: `android/assets/ATTRIBUTION.md`
- Project skill: `skills/android-3d-air-defense/SKILL.md`

## Major Gameplay Upgrades
- **Fire-control loop, not point-and-shoot**: scan -> track table -> prioritize -> salvo engage -> terminal intercept.
- **Live doctrine tuning**: engagement range, interceptor speed, launch cooldown, radar refresh interval, blast radius, and salvo size are adjustable while fighting.
- **Mixed raids**: ballistic, cruise-like, decoy, and anti-radiation missile profiles.
- **Counter-battery pressure**: about 1 in 20 threat missiles is anti-radiation and can directly damage radar / ECS / launcher capacity.
- **Shared simulation core**: the GUI battle and the headless Monte Carlo runner now use the same missile, interceptor, damage, and wave logic.

## Full 3D Battlespace
- Textured night skyline backdrop and downscaled night-sky panorama for a more believable urban atmosphere.
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
- **Report-oriented benchmark suite**: build timing, startup, battle entry, frame timing, runtime health capture, simulation sweeps, KtLint, lint, detekt, dependency health, and APK size snapshots.

## Toolchain
- Android Gradle Plugin `8.13.0`
- Gradle `8.13`
- Kotlin `2.2.21`
- libGDX `1.14.0`
- Java `21`
- Compile / target SDK `36`

## Local Development
1. Open the project in the latest stable Android Studio.
2. Make sure Java 21 and Android SDK 36 are installed.
3. Sync Gradle.
4. Run `.\gradlew.bat ktlintCheck` for the mandatory Kotlin format/lint gate.
5. Run `.\gradlew.bat ktlintFormat` to apply repository formatting.
6. Run `.\gradlew.bat :core:test` for gameplay and targeting logic coverage.
7. Run `.\gradlew.bat :core:runBattleMonteCarlo -Pruns=300 -Pwaves=1 -Pseconds=48 -Pstep=0.05` for headless balance sweeps.
8. Or use `.\scripts\run-battle-monte-carlo.cmd 300 1 48 0.05 20260411` on Windows without changing PowerShell execution policy.
9. Run `.\scripts\run-benchmark-suite.cmd` for the report-oriented benchmark suite.
   It captures build timings, macrobenchmarks, runtime-health artifacts, Monte Carlo balance metrics, and standards reports under `benchmark-results/`.
10. Run `python3 tools/update_readme_badges.py --check` after badge metadata changes.
11. Run `.\gradlew.bat :android:installDebug` to install the debug package on an emulator or device.
12. Launch the `android` module and enter a battle from the main menu.

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
- The current renderer targets a high-quality mobile approximation of premium night lighting; it does not use true hardware ray tracing.
- Waterfront building placement and radar orientation are now validated in tests so the city stays inland and the radar reads top-down toward the horizon.
- AI-agent routing is intentionally compact and generated/verified in-repo so external agents do not keep rediscovering the same workflow.
- The repo's concise agent overlay is pinned to [caveman `v1.5.0`](https://github.com/JuliusBrussee/caveman/releases/tag/v1.5.0). Upstream `v1.5.0` adds configurable default mode, `off`, and `/caveman-help`; this repo intentionally keeps only the mandatory instruction-only `lite`-equivalent overlay.
- Generated `build/`, `.kotlin/`, and copied native-library output are intentionally excluded from git so the repository stays source-clean.
