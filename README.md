# Air Defense Warrior (Android 3D Prototype)

A working libGDX + Kotlin Android prototype where the player commands a Patriot-like air-defense launcher to intercept incoming ballistic missiles in a fully 3D scene.

## Focus Areas
- **3D graphics**: Ground plane, launcher, incoming ballistic missiles, and interceptors are rendered as real 3D models.
- **Physics**: Ballistic trajectories are computed with gravity (`9.81 m/s^2`) and predictive lead targeting for interceptors.
- **Touch controls**: Launch interceptor, rotate radar/camera left-right, and track wave pressure via HUD.
- **Modern structure**: Multi-module Gradle project (`:core` + `:android`) in Kotlin.

## Run (Android)
1. Open project in Android Studio (latest stable).
2. Sync Gradle.
3. Run the `android` module on a physical device or emulator.

## Gameplay
- Tap **Launch Interceptor** to fire toward the closest threat using predictive aiming.
- Use **< Radar** and **Radar >** to rotate camera/radar perspective.
- Survive escalating waves as incoming missile rates and speeds increase.

## Notes
- This is a gameplay prototype; visuals are intentionally procedural to keep iteration fast.
- You can tune difficulty in `BattleScreen.kt` using the balancing defaults in `skills/android-3d-air-defense/SKILL.md`.
