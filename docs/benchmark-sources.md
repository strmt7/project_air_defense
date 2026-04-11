# Benchmark Sources

This benchmark suite was shaped from the following sources.

## Official Android guidance

- Macrobenchmark inspection codelab:
  https://developer.android.com/codelabs/android-macrobenchmark-inspect
- JankStats:
  https://developer.android.com/topic/performance/jankstats
- Android Studio jank detection:
  https://developer.android.com/studio/profile/jank-detection
- Android game optimization overview:
  https://developer.android.com/games/optimize
- Android texture optimization:
  https://developer.android.com/games/optimize/textures
- API 36 `AppJankStats` reference:
  https://developer.android.com/reference/android/app/jank/AppJankStats

## Open-source references

- Android performance samples:
  https://github.com/android/performance-samples
- Now in Android:
  https://github.com/android/nowinandroid
- Dependency Analysis Gradle Plugin:
  https://github.com/autonomousapps/dependency-analysis-gradle-plugin
- KtLint:
  https://github.com/pinterest/ktlint
- KtLint Gradle Plugin:
  https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint
- Detekt:
  https://github.com/detekt/detekt
- Gradle Profiler:
  https://github.com/gradle/gradle-profiler

## How they map to this repo

- Macrobenchmark patterns, battle launch control, and artifact parsing:
  `benchmarks/` and `scripts/run-benchmark-suite.ps1`
- Standards audit:
  root `build.gradle.kts`, `ktlint`, `detekt`, `lint`, and dependency-analysis tasks
- Runtime health capture:
  `scripts/run-benchmark-suite.ps1`
- Headless gameplay balance sweep:
  `core:runBattleMonteCarlo`
