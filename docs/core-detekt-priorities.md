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

- `.\gradlew.bat ktlintCheck :android:testDebugUnitTest :core:test :core:detekt --no-daemon --console=plain`
- `.\gradlew.bat :core:runBattleMonteCarlo :android:installDebug --no-daemon --console=plain "-Pruns=1" "-Pwaves=1" "-Pseconds=48" "-Pstep=0.05" "-Pseed=4242"`

Deterministic guardrail stayed exact:

- city integrity: `88.000%`
- intercept rate: `88.889%`
- score: `1200`
- hostile impacts: `1`
- destroyed buildings: `0`

## Android 15 Proof Lane

Live emulator proof on `com.airdefense.game.debug` used the trusted Android 15 / Pixel 9 Pro lane in wide orientation:

- OCR found `ENTER AIRSPACE` and the tap landed on that text.
- OCR then found battle-only text: `BATTLESPACE`, `CONTROL`, `CITY 100%`, and live `WAVE 1` state.
- logcat confirmed `DeviceProfile: emulator=true lowRam=false heapClassMb=576 resolved=HIGH`, `BattleQuality: requested=AUTO effective=BALANCED deviceClass=HIGH`, `StartScreen: ENTER AIRSPACE pressed`, full `BattleInit`, and live `BattleFrame` telemetry.
- PID and crash-buffer checks were retained as secondary evidence, not primary evidence.
- The legacy `airdefense_api36` AVD is excluded from proof after it stalled at the Android logo with `adb offline`.

Artifacts:

- [menu screenshot](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\wide-policy-menu.png)
- [menu OCR](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\wide-policy-menu-ocr.json)
- [tap proof](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\wide-policy-tap.json)
- [battle screenshot](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\wide-policy-battle.png)
- [battle OCR](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\wide-policy-battle-ocr.json)
- [process proof](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\wide-policy-process.txt)
- [crash buffer](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\wide-policy-crash.txt)
- [logcat](C:\codex_3dgame_android\project_air_defense\benchmark-results\visual-qa\wide-policy-logcat.txt)

## Process Rules Learned From Earlier Failures

- Navigation is not treated as verified from PID or logcat alone.
- OCR or template proof is mandatory both before and after a screen transition.
- Emulator health is verified before app health.
- The active QA package is documented explicitly instead of guessed from memory.
- Broken emulator configurations are excluded from proof instead of being treated as flaky noise.

## Next Measurable Priorities After Detekt

| Priority | Surface | Why it matters now | Measurable target | Verification |
| --- | --- | --- | --- | --- |
| P1 | battle visuals and readability | detekt debt is closed, so the next visible problem is scene richness versus frame safety on the Android 15 wide-only lane | raise Android 15 quality selection and keep startup/runtime metrics green | `run-benchmark-suite`, wide-only Android 15 visual QA, logcat `BattleQuality` / `DeviceProfile` |
| P2 | gameplay balance | the shared simulation is now clean enough to tune more aggressively | raise intercept quality without making outcomes deterministic or trivial | seeded Monte Carlo, targeted gameplay tests |
| P3 | production asset quality | the renderer is still limited more by placeholder assets than by code structure | replace procedural placeholders with stronger licensed city assets without breaking mobile safety | Android 15 visual QA, benchmark runtime telemetry |
