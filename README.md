# Project Air Defense (Android 3D)

A high-fidelity libGDX + Kotlin Android prototype where you command a Patriot-inspired battery in a living 3D battlespace.

## Major Gameplay Upgrades
- **Fire-control loop, not point-and-shoot**: scan -> track table -> prioritize -> salvo engage -> terminal intercept.
- **Live doctrine tuning**: engagement range, interceptor speed, launch cooldown, radar refresh interval, blast radius, and salvo size are adjustable while fighting.
- **Mixed raids**: ballistic, cruise-like, decoy, and anti-radiation missile profiles.
- **Counter-battery pressure**: ~1 in 20 threat missiles is anti-radiation and can directly damage radar / ECS / launcher capacity.

## Full 3D Battlespace
- Semi-desert terrain palette with roads, mountain belt, defended urban blocks, and scattered vegetation.
- Patriot-inspired battery footprint including launcher trailer + canisters, radar, ECS shelter, mast, and power unit silhouettes.
- Dynamic trails, blast effects, and persistent damage: buildings darken/collapse, battery components degrade, radar can go offline temporarily.

## Public references used for design direction
- RTX Patriot overview: https://www.rtx.com/raytheon/what-we-do/integrated-air-and-missile-defense/patriot
- Lockheed PAC-3 product page: https://www.lockheedmartin.com/en-us/products/pac-3-advanced-capability-3.html
- U.S. Army Patriot article: https://www.army.mil/article/171144/patriot_missile_system

## Security and Performance
- **ProGuard/R8 Enabled**: Minification and obfuscation for production security.
- **GLES 3.0 + MSAA**: Modern hardware-accelerated rendering with anti-aliasing.
- **Optimized Memory**: Minimal allocations in the main loop using math buffers and pooled entities.
- **Strict Repositories**: Only Google and MavenCentral used for dependency resolution.
1. Open project in Android Studio (latest stable).
2. Sync Gradle.
3. Run the `android` module on an emulator or device.

4. Build an installable APK:
   - Preferred helper script (prints which signing path is used): `./scripts/build-installable-apk.sh`
   - Direct Gradle command: `./gradlew :android:printReleaseSigningSource :android:assembleRelease`
   - Production signing (set these in `~/.gradle/gradle.properties`, CI secrets, or environment variables):
     - `RELEASE_STORE_FILE=/absolute/path/to/keystore.jks`
     - `RELEASE_STORE_PASSWORD=...`
     - `RELEASE_KEY_ALIAS=...`
     - `RELEASE_KEY_PASSWORD=...`

## Notes
- This is still a gameplay prototype, not a military simulator.
- Shapes are procedural and inspired by public imagery; swap in art assets for production-grade visual fidelity.
