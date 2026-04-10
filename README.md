# Project Air Defense (Android 3D)

A high-fidelity libGDX + Kotlin Android prototype where you command a Patriot-inspired battery in a living 3D battlespace.

## AI-Friendly Docs
- Agent operating guide: `AGENTS.md`
- Architecture reference: `docs/architecture.md`
- Release/install behavior: `docs/release-and-install.md`

## Major Gameplay Upgrades
- **Fire-control loop, not point-and-shoot**: scan -> track table -> prioritize -> salvo engage -> terminal intercept.
- **Live doctrine tuning**: engagement range, interceptor speed, launch cooldown, radar refresh interval, blast radius, and salvo size are adjustable while fighting.
- **Mixed raids**: ballistic, cruise-like, decoy, and anti-radiation missile profiles.
- **Counter-battery pressure**: about 1 in 20 threat missiles is anti-radiation and can directly damage radar / ECS / launcher capacity.

## Full 3D Battlespace
- Textured night skyline backdrop and downscaled night-sky panorama for a more believable urban atmosphere.
- Procedural surface materials with diffuse texture detail and roughness response for buildings, roads, metal equipment, debris, and projectiles.
- Dynamic trails, blasts, camera shake, and persistent damage: buildings darken, lean, collapse in height, and emit debris when hit.
- Gameplay-first projectile readability: hostile missiles and interceptors are intentionally oversized and color-separated so they stay legible on mobile screens.

## Public References Used For Design Direction
- RTX Patriot overview: https://www.rtx.com/raytheon/what-we-do/integrated-air-and-missile-defense/patriot
- Lockheed PAC-3 product page: https://www.lockheedmartin.com/en-us/products/pac-3-advanced-capability-3.html
- U.S. Army Patriot article: https://www.army.mil/article/171144/patriot_missile_system

## Security And Performance
- **ProGuard/R8 enabled**: minification and obfuscation for production security.
- **GLES 3.0 + MSAA**: modern hardware-accelerated rendering with anti-aliasing.
- **Optimized memory**: minimal allocations in the main loop using math buffers and pooled entities.
- **Strict repositories**: only Google and Maven Central are used for dependency resolution.

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
4. Run `.\gradlew.bat :core:test` for gameplay and targeting logic coverage.
5. Run `.\gradlew.bat :android:installDebug` to install the debug package on an emulator or device.
6. Launch the `android` module and enter a battle from the main menu.

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
- Generated `build/`, `.kotlin/`, and copied native-library output are intentionally excluded from git so the repository stays source-clean.
