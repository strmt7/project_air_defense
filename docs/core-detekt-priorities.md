# Core Detekt Priorities

Measured from [core detekt XML](C:\codex_3dgame_android\project_air_defense\core\build\reports\detekt\detekt.xml) on `2026-04-12`.

## Current Verified State

- Core detekt findings: `0`
- Measured reduction from the original baseline: `1929 -> 0`
- [BattleScreen.kt](C:\codex_3dgame_android\project_air_defense\core\src\main\kotlin\com\airdefense\game\BattleScreen.kt): `1362 -> 0`
- No active detekt debt remains in `core` on this tree.

## Structural Changes That Closed The Debt

Root-cause ownership moved out of [BattleScreen.kt](C:\codex_3dgame_android\project_air_defense\core\src\main\kotlin\com\airdefense\game\BattleScreen.kt) into small, testable collaborators:

- [BattleSceneController.kt](C:\codex_3dgame_android\project_air_defense\core\src\main\kotlin\com\airdefense\game\BattleSceneController.kt)
  Owns battle camera shake, environment setup, launcher pulses, and city/launcher light updates.
- [BattleBuildingVisualController.kt](C:\codex_3dgame_android\project_air_defense\core\src\main\kotlin\com\airdefense\game\BattleBuildingVisualController.kt)
  Owns building damage visuals, collapse/lean animation, and building transform sync.
- [BattleInitializationController.kt](C:\codex_3dgame_android\project_air_defense\core\src\main\kotlin\com\airdefense\game\BattleInitializationController.kt)
  Owns staged startup progress and timing instead of embedding initialization control inside the screen.
- [BattleAssetBootstrapper.kt](C:\codex_3dgame_android\project_air_defense\core\src\main\kotlin\com\airdefense\game\BattleAssetBootstrapper.kt)
  Owns texture/model bootstrap coordination instead of mixing asset setup with battle orchestration.
- [BattleWorldBootstrapper.kt](C:\codex_3dgame_android\project_air_defense\core\src\main\kotlin\com\airdefense\game\BattleWorldBootstrapper.kt)
  Owns base world instance creation, imported landmarks, and simulation construction.
- [BattleProjectileVisualController.kt](C:\codex_3dgame_android\project_air_defense\core\src\main\kotlin\com\airdefense\game\BattleProjectileVisualController.kt)
  Owns missile and interceptor transform synchronization.
- [BattleWorldRenderPass.kt](C:\codex_3dgame_android\project_air_defense\core\src\main\kotlin\com\airdefense\game\BattleWorldRenderPass.kt)
  Owns the world 3D draw pass.
- [BattleRuntimeSnapshot.kt](C:\codex_3dgame_android\project_air_defense\core\src\main\kotlin\com\airdefense\game\BattleRuntimeSnapshot.kt)
  Owns the lightweight runtime state handed to HUD and frame-telemetry formatting.
- [BattleEffectFactory.kt](C:\codex_3dgame_android\project_air_defense\core\src\main\kotlin\com\airdefense\game\BattleEffectFactory.kt) and [BattleEffectUpdate.kt](C:\codex_3dgame_android\project_air_defense\core\src\main\kotlin\com\airdefense\game\BattleEffectUpdate.kt)
  Split creation and update rules out of the effects controller.
- [BattleEffectsController.kt](C:\codex_3dgame_android\project_air_defense\core\src\main\kotlin\com\airdefense\game\BattleEffectsController.kt)
  Reduced to orchestration over effect factories, update helpers, and budget policy.

## Verification On This Tree

- `.\gradlew.bat ktlintCheck :core:test :core:detekt --no-daemon --console=plain`
- `.\gradlew.bat :core:runBattleMonteCarlo :android:installDebug --no-daemon --console=plain "-Pruns=1" "-Pwaves=1" "-Pseconds=48" "-Pstep=0.05" "-Pseed=4242"`

Deterministic guardrail stayed exact:

- city integrity: `88.000%`
- intercept rate: `88.889%`
- score: `1200`
- hostile impacts: `1`
- destroyed buildings: `0`

## Android 15 Proof Lane

Live emulator proof on `com.airdefense.game.debug` used the trusted Android 15 / Pixel 9 Pro lane:

- OCR found `ENTER AIRSPACE` and the tap landed on that text.
- OCR then found battle-only text: `BATTLESPACE`, `CONTROL`, `CITY 100%`, and live `WAVE 1` state.
- logcat confirmed `StartScreen: ENTER AIRSPACE pressed`, `Switching to BattleScreen`, full `BattleInit`, and live `BattleFrame` telemetry.
- PID and crash-buffer checks were retained as secondary evidence, not primary evidence.
- The legacy `airdefense_api36` AVD is excluded from proof after it stalled at the Android logo with `adb offline`.

Artifacts:

- [menu screenshot](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\android15-menu-clear.png)
- [menu OCR](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\android15-menu-clear-ocr.json)
- [tap proof](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\android15-enterairspace-tap.json)
- [battle screenshot](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\android15-battle-clear.png)
- [battle OCR](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\android15-battle-clear-ocr.json)
- [process proof](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\android15-process-clear.txt)
- [crash buffer](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\android15-crash-clear.txt)
- [logcat](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\android15-logcat-clear.txt)

## Process Rules Learned From Earlier Failures

- Navigation is not treated as verified from PID or logcat alone.
- OCR or template proof is mandatory both before and after a screen transition.
- Emulator health is verified before app health.
- The active QA package is documented explicitly instead of guessed from memory.
- Broken emulator configurations are excluded from proof instead of being treated as flaky noise.

## Next Measurable Priorities After Detekt

| Priority | Surface | Why it matters now | Measurable target | Verification |
| --- | --- | --- | --- | --- |
| P1 | battle startup and frame pacing | detekt debt is closed, so the next user-visible cost is startup latency and frame stability | improve cold battle startup and runtime FPS in the benchmark suite without regressing gameplay | `run-benchmark-suite`, Android 15 visual QA |
| P2 | battle visuals and readability | the current graphics stack still needs stronger readability and production value | improve scene quality without breaking mobile-safe rendering | Android 15 visual QA, benchmark runtime telemetry |
| P3 | gameplay balance | the shared simulation is now clean enough to tune more aggressively | raise intercept quality without making outcomes deterministic or trivial | seeded Monte Carlo, targeted gameplay tests |
