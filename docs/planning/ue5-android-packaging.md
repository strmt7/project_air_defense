# UE5 Android Packaging

## Why this exists

The `Build Legacy Android APK` CI workflow invokes `./gradlew :android:assembleLocal` and uploads `legacy-android-libgdx-apk`. That artifact only builds the **legacy libGDX** prototype in `android/`. The UE5 runtime in `ue5/ProjectAirDefenseUE5/` is not part of that artifact and never has been.

Shipping the UE5 runtime (real Helsinki Kalasatama 3D Tiles through Cesium, the procedural M901 Patriot silhouettes, the CommonUI menus, the proportional-navigation interceptor physics) to an Android device requires the UE5 Android packaging path. That path is now configured in this repo but still requires a host machine or CI runner with an actual Unreal Engine 5.7 installation and an Android NDK.

## What is configured in-repo

- `ue5/ProjectAirDefenseUE5/ProjectAirDefenseUE5.uproject` enables `CesiumForUnreal` and `EnhancedInput` (unchanged).
- `Config/DefaultEngine.ini` now contains an `AndroidRuntimeSettings` block that pins package name `com.airdefense.game.ue5`, landscape orientation, arm64 only, Vulkan plus ES3.1, min SDK 26, target SDK 36, gradle always on, external OBB enabled for datasets that exceed the APK limit, and `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK` extra permissions so Cesium can stream 3D Tiles at runtime.
- `Config/Android/AndroidEngine.ini` sets mobile-specific renderer defaults: forward shading, mobile virtual textures on, mobile AA on (TAA), mobile HDR on, distance fields off (too expensive on phone GPUs), local lights allowed, movable spotlights allowed including shadows.
- `scripts/package-ue5-runtime-android.ps1` and `.cmd` wrap the UAT `BuildCookRun` invocation for the Android platform with ASTC or ETC2 texture cook flavor, Development or Shipping configuration, and archive directory `packaged/Android`.

The Win64 `package-ue5-runtime.{ps1,cmd}` scripts are untouched and continue to work.

## What still needs to be done outside this repo

1. Install Unreal Engine 5.7 on a Windows (or Linux) host via the Epic Games Launcher. Do not use Unreal Engine from source unless Epic's Android plugin is also built.
2. Install the Android SDK (`platform-tools`, `platforms;android-36`, `build-tools;36.0.0`). Keep `local.properties` untracked and local-only, or set `ANDROID_HOME`; never commit a workstation-specific `sdk.dir`.
3. Install Android NDK r25b. UE 5.7 expects r25b specifically; newer NDKs produce link errors against UE's precompiled third-party libraries. Set `NDKROOT` or `ANDROID_NDK_HOME` to the NDK path.
4. Install JDK 17 (Android Gradle Plugin 8.x requirement for UE's Android build). Java 21 works for the legacy libGDX lane but UE's gradle scripts are pinned at Java 17.
5. In Unreal Editor, open `ProjectAirDefenseUE5.uproject` once and accept any plugin compilation prompts. Cesium for Unreal compiles a native shared object per target, so the first open after pulling the repo takes several minutes.
6. Run `.\scripts\package-ue5-runtime-android.cmd` from a Windows terminal. Add `-Configuration Shipping` for a release build. The output is placed in `packaged/Android/`.
7. Install the resulting APK on a device with `adb install packaged/Android/ProjectAirDefenseUE5-arm64.apk` or transfer the `.aab` to the Play Console.

## Known caveats

- **APK size.** A UE5 packaged Android build with Cesium streaming disabled (all tiles bundled) is measured in gigabytes. With streaming enabled, the APK ships only the engine code and shaders while the Helsinki tileset is pulled over HTTPS at runtime from `https://3d.hel.ninja/data/mesh/Kalasatama/...`. The `AndroidRuntimeSettings` block sets `bPackageDataInsideApk=False` and `bAllowLargeOBBFiles=True` so large data is split into an OBB alongside the APK.
- **Cesium runtime licensing.** Helsinki Kalasatama 3D Tiles are CC BY 4.0. The required `City of Helsinki / Helsinki 3D` credit must remain visible in the shipping UI; the main-menu widget already renders it.
- **First-run device compatibility.** UE5 mobile requires a GPU that supports Vulkan 1.1 or OpenGL ES 3.1. Pixel 6 and later, Samsung S20 and later, and most 2022+ devices qualify. Older devices fall back to the ES3.1 path which has reduced shader quality.
- **Signing.** The first packaged Android output is self-signed with a debug keystore. For Play Store upload, provide a real keystore through environment variables or the `AndroidRuntimeSettings` `KeyStore*` fields. Do not commit keystore credentials.

## CI path

The hosted `ubuntu-latest` runner still cannot package UE5 for Android because it does not contain Unreal Engine and cannot install it under GitHub's disk quota. This repo now includes a manual `Package UE5 Android Runtime` workflow for a self-hosted Windows runner labeled `self-hosted`, `windows`, `ue5`, and `android`. Its checkout uses `clean: false` so GitHub Actions does not delete the ignored local `ExternalData/helsinki_kalasatama_3dtiles` terrain directory.

The three practical routes are:

1. A self-hosted GitHub runner that is provisioned once with UE 5.7, Android SDK, and NDK r25b. This is the default Epic-recommended path for studios.
2. A community UE5 container image (`ghcr.io/epicgames-community/unreal-engine-runner` or equivalent) that requires Epic credentials stored as `secrets.EPIC_USERNAME` / `secrets.EPIC_PASSWORD`.
3. An externally-hosted build farm (Incredibuild, Horde) that the workflow triggers over HTTP.

The self-hosted workflow is intentionally manual-only. It fails early if UE 5.7, Android SDK, NDK r25b, or the local Helsinki 3D Tiles root is missing, because a misleading APK artifact is worse than no UE5 artifact.

## Verification minimum

After adding any Android-specific UE5 content:

- `python3 tools/update_readme_badges.py --check` if badge metadata changed.
- Open `ProjectAirDefenseUE5.uproject` locally and run the `ProjectAirDefense.BattleSimulation.*` automation tests. No Android-specific tests exist yet because the simulation is platform-independent.
- `.\scripts\package-ue5-runtime-android.cmd -Configuration Development` on the developer workstation, or run the manual `Package UE5 Android Runtime` workflow on the self-hosted runner.
- `adb install packaged/Android/...apk` on a phone with developer mode.
- Confirm the menu renders `PROJECT AIR DEFENSE`, the Helsinki 3D Tiles load, a Patriot battery silhouette is visible, and `START DEFENSE` launches a wave.
