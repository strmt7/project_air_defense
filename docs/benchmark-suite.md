# Benchmark Suite

This repository now has a benchmark suite for more than correctness.

## Coverage

- Clean, warm, and standards-audit build timing
- Android startup timing
- Direct-to-battle startup timing
- Active battle frame timing
- Runtime health capture with `dumpsys gfxinfo`, `meminfo`, crash buffer, logcat, and screenshot evidence
- Headless Monte Carlo battle balance analysis
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

The suite writes artifacts under `benchmark-results/<timestamp>/`:

- `summary.md`
- `summary.json`
- `build-benchmarks.json`
- `runtime-health.json`
- `battle-monte-carlo.json`
- `runtime-battle.png`
- copied `*-benchmarkData.json` files from the Android connected-test output

## Notes

- The Android benchmark target uses the `benchmark` app build type: `com.airdefense.game.benchmark`.
- Battle frame benchmarks launch directly into battle with a fixed seed so measurements are repeatable.
- Macrobenchmark execution on the emulator is intentionally allowed for comparative local work, but final performance decisions should still be re-checked on a physical device.
- For libGDX / SurfaceView rendering, `dumpsys gfxinfo` is supplementary only. Use the macrobenchmark traces and sampled frame metrics as the primary frame-health signal.
- Baseline Profile generation is intentionally not enabled in this repo-level suite because the standard Android plugin flow creates internal release-like variants that conflict with this project's signing-safety rules.
- This suite is report-oriented. It is intended to show the current state of performance and engineering health, not only binary pass or fail.
