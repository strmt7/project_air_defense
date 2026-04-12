# Architecture Reference

## Screen Flow
- `AirDefenseGame.create()`
  Starts `StartScreen`.
- `StartScreen`
  Draws the textured menu scene and transitions into `BattleScreen`.
- `BattleScreen`
  Owns battle orchestration, live entity state, audio/effects hooks, and delegates rendering/asset work to specialized collaborators.
- `BattleHudController`
  Owns battle HUD widget construction and snapshot-driven widget updates.
- `BattleHudState`
  Owns pure HUD text/button-state shaping for the battle screen.
- `BattleSimulationStepApplier`
  Owns translation of shared simulation step events into live render-entity/effect/audio updates.
- `BattleEffectsController`
  Owns battle effect pressure rules, trail throttling, explosions, smoke, sparks, debris, and impact-light updates.
- `BattleSceneRenderer`
  Owns backdrop composition, atmosphere passes, loading/game-over screens, and radar/threat overlay rendering.
- `BattleTextureFactory`
  Coordinates the generated texture pipeline and shared texture registration.
- `BattleSurfaceTextureFactory`
  Owns generated facade, terrain, metal, concrete, and solid material textures plus tiled UV attributes.
- `BattleBackdropTextureFactory`
  Owns generated sky, fog, glow, and reflection textures plus horizon image loading.
- `BattleTerrainAssetFactory`
  Owns the ground/sea/beach/promenade/road model bundle plus backdrop texture selection.
- `BattleBuildingAssetFactory`
  Owns procedural tower/hotel/podium/slab model generation.
- `BattleDefenseAssetFactory`
  Owns launcher and radar model generation.
- `BattleProjectileAssetFactory`
  Owns threat, interceptor, blast, trail, debris, and moon model generation.
- `BattleSimulation`
  Owns the live battle math used by both the GUI and the headless runner.
- `BattleSimulationStepApplier`
  Applies `BattleStepEvents` from the shared simulation to render entities, launcher pulses, damage visuals, blasts, and status/HUD updates.
- `BattleWorldLayout`
  Owns launcher coordinates, city-building placement, shoreline safety constraints, and radar projection rules.

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
- `BattleSimulation`
  Shared wave spawning, launcher choice, interceptor guidance, fuse checks, city damage, and score accounting.
- `RadarProjection`
  Shared top-down radar mapping so the overlay matches the camera-facing direction of inbound threats.

## Rendering Layers
- Procedural models:
  terrain, coastline, roads, launchers, radar, buildings, missiles, interceptors, effects, debris.
- Generated textures:
  facade/window maps, ground/sea/beach/park/promenade/road maps, metal maps, concrete maps, sky/fog/glow/reflection maps.
- Imported textures:
  sky panorama and skyline backdrop.
- Frame composition:
  `BattleSceneRenderer` draws backdrop bands, reflection/horizon layers, atmosphere fog, radar contacts, and tracked-threat labels after the 3D world pass.
- Effects lifecycle:
  `BattleEffectsController` owns the visual-effect and debris arrays, including budget scaling and per-frame effect updates.
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
- HUD state:
  [BattleHudStateTest.kt](C:\codex_3dgame_android\project_air_defense\core\src\test\kotlin\com\airdefense\game\BattleHudStateTest.kt) covers wave-state, summary, and button-state formatting.
- Effects budget math:
  [BattleEffectsControllerTest.kt](C:\codex_3dgame_android\project_air_defense\core\src\test\kotlin\com\airdefense\game\BattleEffectsControllerTest.kt) covers effect-budget scaling, trail-stride escalation, and bounded effect-count reduction.
- Headless balance:
  `.\gradlew.bat :core:runBattleMonteCarlo -Pruns=300 -Pwaves=1 -Pseconds=48 -Pstep=0.05`
- Windows shortcut:
  `scripts/run-battle-monte-carlo.cmd`
- Rendering:
  emulator-verified for menu flow, battle entry, HUD state, and projectile readability.
- Visual QA tooling:
  `tools/android_visual_qa/visual_qa.py` drives OCR-backed launch, tap, capture, and screen-text verification on the emulator.
- Android packaging:
  debug install verified on emulator; production channel still requires a stable release keystore for update-safe release builds.
