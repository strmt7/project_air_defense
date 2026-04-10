# Architecture Reference

## Screen Flow
- `AirDefenseGame.create()`
  Starts `StartScreen`.
- `StartScreen`
  Draws the textured menu scene and transitions into `BattleScreen`.
- `BattleScreen`
  Owns battle state, renderables, HUD, audio, effects, and destruction state.

## Gameplay Layers
- `BattleBalance`
  Wave size and spawn cadence.
- `ThreatFactory`
  Ballistic launch generation toward the city.
- `InterceptionMath`
  Lead-solve used by interceptors.
- `DamageModel`
  Blast falloff and city-integrity accounting.
- `FireControl`
  Shared target-priority rules used by tests and the live battle loop. It prioritizes the closest inbound track, then the lower-altitude threat, then the threat closest to the centerline.

## Rendering Layers
- Procedural models:
  sky sphere, ground plane, roads, launchers, radar, missiles, interceptors, effects, debris.
- Generated textures:
  facade/window maps, ground/road maps, metal maps, concrete maps.
- Imported textures:
  sky panorama and skyline backdrop.
- Shader:
  `NightShader` combines diffuse textures, roughness textures, emissive glow, directional moon/fill light, impact point light, specular response, fresnel rim, and fog.

## Destruction Model
- Threat impact triggers:
  blast effect, debris emission, camera shake, building damage, city-integrity loss.
- Buildings store:
  `integrity`, `visibleHeight`, `lean`, `leanTarget`, `collapseVelocity`.
- Collapse is faked but stateful:
  height reduction, lean, darkening, and debris make destruction visible without full rigid-body simulation.

## APK Channels
- `release`
  Production package id `com.airdefense.game`, requires stable release signing.
- `local`
  Debug-signed package id `com.airdefense.game.local`, safe for repeat sideload testing.
- `debug`
  Debug-signed package id `com.airdefense.game.debug`.

## Verification Surface
- Logic:
  `core/src/test/kotlin/com/airdefense/game/*`
- Rendering:
  emulator-verified for menu flow, battle entry, HUD state, and projectile readability.
- Android packaging:
  debug install verified on emulator; production channel still requires a stable release keystore for update-safe release builds.
