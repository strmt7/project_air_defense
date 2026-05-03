# Toolchain Source Map

Verified on 2026-05-03. Use release or plugin pages below before changing
versions; do not replace pinned versions with floating `latest`.

| Tool | Pinned version | Source |
| --- | --- | --- |
| Gradle wrapper | `9.5.0` | <https://gradle.org/releases/> |
| Android Gradle Plugin | `9.2.0` | <https://developer.android.com/build/releases/gradle-plugin> |
| Kotlin JVM plugin | `2.3.21` | <https://kotlinlang.org/docs/releases.html> |
| ktlint Gradle plugin | `14.2.0` | <https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint> |
| ktlint engine | `1.8.0` | <https://github.com/pinterest/ktlint/releases> |
| detekt Gradle plugin | `2.0.0-alpha.3` | <https://plugins.gradle.org/plugin/dev.detekt> |

UE5 Android packaging remains separate from the legacy Android Gradle lane:
UE 5.7 Android packaging is pinned to NDK r25b `25.1.8937393` until Epic's
UE5 Android tooling changes.
