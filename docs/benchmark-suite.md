# Benchmark Suite

This repository now has a benchmark suite for more than correctness.

## Coverage

- Clean, warm, and standards-audit build timing
- Android startup timing
- Direct-to-battle startup timing
- Runtime health capture with `dumpsys gfxinfo`, `meminfo`, crash buffer, logcat, and screenshot evidence
- Runtime battle frame telemetry windows with rolling FPS and frame-time stats
- Headless Monte Carlo battle balance analysis
- UE5 automation smoke for the headless Monte Carlo commandlet through the active C++ simulation
- UE5 full headless Monte Carlo battle balance through the active C++ simulation when the standalone UE5 runner is invoked
- KtLint format/lint gate
- Android Lint
- Detekt
- Dependency health reports
- APK size snapshot

## Ready-to-use building blocks adopted

- Android Macrobenchmark patterns from official Android guidance and `android/performance-samples`
- Benchmark module structure from `android/nowinandroid`
- Runtime frame and memory capture with standard `adb` / `dumpsys` Android tooling
- Dependency health reporting from `autonomousapps/dependency-analysis-gradle-plugin`
- Kotlin static analysis from `detekt`
- Kotlin formatting and style enforcement from `ktlint`

## Main entry point

Windows:

```powershell
.\scripts\run-benchmark-suite.cmd
```

Optional arguments:

```powershell
.\scripts\run-benchmark-suite.cmd -Runs 500 -Waves 2 -Seconds 60 -Step 0.05 -Seed 20260411
```

## Output

The Android benchmark suite writes artifacts under `benchmark-results/<timestamp>/`:

- `summary.md`
- `summary.json`
- `build-benchmarks.json`
- `runtime-health.json`
- `battle-monte-carlo.json`
- `runtime-battle.png`
- copied `*-benchmarkData.json` files from the Android connected-test output

UE5-specific scripts write under `benchmark-results/` unless their output paths are overridden:

- `ue5-automation-tests.log` from `scripts/run-ue5-automation-tests.ps1`
- `ue5-automation-monte-carlo-smoke.json` from the default-on smoke lane in `scripts/run-ue5-automation-tests.ps1`
- `ue5-automation-monte-carlo-smoke.log` from the default-on smoke lane in `scripts/run-ue5-automation-tests.ps1`
- `ue5-battle-monte-carlo.json` from `scripts/run-ue5-battle-monte-carlo.ps1`
- `ue5-battle-monte-carlo.log` from `scripts/run-ue5-battle-monte-carlo.ps1`
- `ue5-packaged-ui-proof-manifest.json` from `scripts/run-ue5-mobile-ui-proof.ps1`
- `ue5-packaged-*-proof-state.json` from `scripts/run-ue5-mobile-ui-proof.ps1`, one per menu, battle, and systems capture

## UE5 Automation Smoke

`scripts/run-ue5-automation-tests.ps1` runs the UE automation filter first, then runs a bounded `ProjectAirDefenseBattleMonteCarlo` smoke pass by default. The smoke pass uses `-nullrhi`, `5` runs, `1` wave, `12` seconds per wave, a `0.05` second step, seed `20260417`, the `ShieldWall` doctrine, `2150m` engagement range, `Auto` engagement mode, `Balanced` threat priority, and `Balanced` fire control.

The smoke lane validates that the commandlet produced JSON and that the report contains the expected core contract: non-blank `engine`, `simulation` equal to `FProjectAirDefenseBattleSimulation`, matching `runs`, `waves`, `seed`, `secondsPerWave`, `stepSeconds`, matching doctrine and tactical controls, aggregate metric ranges, non-zero `totalThreatsSpawned`, non-negative totals, and one `runsDetail` entry per run. Use `-SkipBattleMonteCarloSmoke` when intentionally running only the UE automation tests.

## UE5 Mobile UI Proof

`scripts/run-ue5-mobile-ui-proof.ps1` captures menu, battle, and systems screenshots through `scripts/capture-ue5-runtime-screenshot.ps1`. The lane now validates that each screenshot exists, is non-trivial in byte size, has valid dimensions, has sampled color and luminance variation, and has a matching state JSON proving the UE process was still alive when the screenshot appeared. Tesseract OCR is used when available to record whether the menu title `PROJECT AIR DEFENSE` was found; pass `-RequireMenuOcr` when a run must fail without that title proof.

Local Win64 proof command used on 2026-05-05:

```powershell
scripts/run-ue5-mobile-ui-proof.ps1 -Exe ue5/ProjectAirDefenseUE5/Binaries/Win64/ProjectAirDefenseUE5.exe -OutputDir benchmark-results/ue5-ui-proof-final -RequireState -RequireMenuOcr
```

Latest validated Win64 UI proof from 2026-05-05: menu, battle, and systems captures all had `stateVerified=true`; menu OCR found `PROJECT AIR DEFENSE`; screenshot samples had 163, 184, and 167 distinct colors respectively. This is packaged desktop landscape proof, not Android device proof.

Current UE5 balance reference from 2026-05-05 after the fire-control/salvo refinement:

```text
scripts/run-ue5-battle-monte-carlo.ps1 -Runs 300 -Waves 1 -Seconds 48.0 -Step 0.05 -Seed 20260411 -Doctrine ShieldWall -EngagementRange 2150 -EngagementMode Auto -ThreatPriority Balanced -FireControl Balanced
averageInterceptRate=0.903 averageCityIntegrity=87.12 totalThreatsSpawned=3300 totalHostileImpacts=319 totalInterceptorsLaunched=5634 averageMissDistanceMeters=45.53 interceptRateP05=0.727 interceptRateP95=1.000
```

## Notes

- The Android benchmark target uses the `benchmark` app build type: `com.airdefense.game.benchmark`.
- The app smoke lane uses `com.airdefense.game.debug`; keep benchmark and smoke package evidence separate.
- Battle frame benchmarks launch directly into battle with a fixed seed so measurements are repeatable.
- The standards audit now includes `ktlintCheck`, so formatting drift is part of the benchmarked engineering-health surface.
- Macrobenchmark execution on the emulator is intentionally allowed for comparative local work, but final performance decisions should still be re-checked on a physical device.
- The Android lint gate keeps a targeted manifest-level suppression for the Android 16 `DiscouragedApi` warning on `screenOrientation`, because this game is a deliberate landscape-only product.
- For libGDX / SurfaceView rendering, `dumpsys gfxinfo` is supplementary only. Use the runtime `BattleFrame` telemetry windows as the primary frame-health signal.
- The full UE5 Monte Carlo lane is `scripts/run-ue5-battle-monte-carlo.ps1`; it runs `ProjectAirDefenseBattleMonteCarlo` with `-nullrhi` and uses `FProjectAirDefenseBattleSimulation`, the same deterministic C++ simulation class used by the GUI bridge.
- UE5 Monte Carlo accepts tactical controls with the same sanitized settings used by the HUD: `-EngagementRange`, `-EngagementMode Auto|Single|Pair|Ripple`, `-ThreatPriority Balanced|Ballistic|Glide|Cruise|Impact`, and `-FireControl Early|Balanced|Terminal`.
- Baseline Profile generation is intentionally not enabled in this repo-level suite because the standard Android plugin flow creates internal release-like variants that conflict with this project's signing-safety rules.
- This suite is report-oriented. It is intended to show the current state of performance and engineering health, not only binary pass or fail.
