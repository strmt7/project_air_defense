# UE5 Gameplay Migration Matrix

This file is the authoritative checklist for porting the actual game from the legacy Android prototype into the UE5 runtime. Do not mark an item complete without code, a verification lane, and an artifact or log reference.

## Verification Rule

- `implemented` means the system exists in UE5 source and is wired into the active runtime.
- `verified` means there is a reproducible command or test for it.
- `pending` means not yet present in the UE5 runtime.

## Runtime Shell

| System | Legacy source | UE5 target | Status | Verification |
| --- | --- | --- | --- | --- |
| Real 3D city runtime | `BattleWorldBootstrapper.kt` | `ProjectAirDefenseGameMode.cpp` + Cesium tileset bootstrap | implemented, verified | packaged log + runtime screenshots |
| Camera orbit / pan / zoom / pitch / reset | `BattleSceneController.kt` | `ProjectAirDefenseCityCameraPawn.cpp` | implemented, verified | packaged/editor runtime captures |
| Graphics settings backend | `GameGraphicsSettings.kt` | `ProjectAirDefenseGameUserSettings.cpp` | implemented, verified | packaged log shows applied settings |
| Atmosphere and post-process defaults | `BattleScreen.kt` render path | `ProjectAirDefenseGameMode.cpp` + `ProjectAirDefenseRuntimeSettings.*` | implemented, verified | editor and packaged runtime captures |

## Core Gameplay

| System | Legacy source | UE5 target | Status | Verification |
| --- | --- | --- | --- | --- |
| Deterministic battle simulation core | `BattleSimulation.kt` | `ProjectAirDefenseBattleSimulation.*` | implemented, verified | UE automation tests |
| Defense doctrines and tuning | `BattleLogic.kt` | `ProjectAirDefenseBattleSimulation.*` | implemented, verified | doctrine comparison + shoot-look-shoot tests |
| Wave spawning and accounting | `BattleSimulation.kt` | `ProjectAirDefenseBattleSimulation.*` | implemented, verified | consistency + seeded tests |
| Interceptor lead solve, fuse check, and attempt cap | `BattleLogic.kt` | `ProjectAirDefenseBattleSimulation.*` | implemented, verified | intercept, seeker-cone, and attempt-limit tests |
| Threat impact and city damage | `BattleSimulation.kt` | `ProjectAirDefenseBattleSimulation.*` | implemented, verified | hostile-impact test |
| Monte Carlo / headless balance lane | `BattleMonteCarloRunner.kt` | `ProjectAirDefenseBattleMonteCarloCommandlet.*` | implemented, verified | `scripts/run-ue5-automation-tests.ps1` smoke JSON contract + `scripts/run-ue5-battle-monte-carlo.ps1` full balance report |

## Runtime Gameplay Presentation

| System | Legacy source | UE5 target | Status | Verification |
| --- | --- | --- | --- | --- |
| Battle manager that ticks the simulation | `BattleScreen.kt` | `ProjectAirDefenseBattleManager.*` | implemented, verified | editor and packaged runtime |
| Live threat / interceptor visuals | `BattleProjectileVisualController.kt` | `ProjectAirDefenseBattleManager.*` | implemented, verified | gameplay runtime capture |
| Battle HUD with compact side segments | `BattleHudController.kt` | `ProjectAirDefenseBattleWidget.*` | implemented, verified | packaged runtime capture |
| Touch-first systems drawer and camera controls | `BattleHudController.kt` + `BattleSceneController.kt` | `ProjectAirDefenseBattleWidget.*` + `ProjectAirDefenseCityCameraPawn.*` | implemented, verified | packaged systems runtime capture |
| Gameplay input: start wave, doctrine, graphics toggles | `BattleHudController.kt` + `StartScreen.kt` | `ProjectAirDefensePlayerController.*` | implemented, verified | runtime input exercise |
| Radar / tactical overlay | `BattleRadarOverlayRenderer.kt` | `ProjectAirDefenseRadarWidget.*` + `ProjectAirDefenseBattleManager::BuildRadarSnapshot()` | implemented, verified | packaged runtime capture |
| Battle VFX and blast readability | `BattleEffectsController.kt` | `ProjectAirDefenseBattleManager.*` packaged mesh VFX | implemented, verified | packaged battle runtime capture |

## Menus And UX

| System | Legacy source | UE5 target | Status | Verification |
| --- | --- | --- | --- | --- |
| Main menu | `StartScreen.kt` | `ProjectAirDefenseMainMenuWidget.*` | implemented, verified | packaged runtime capture |
| In-game settings surface | `BattleHudController.kt` | UE HUD/menu settings panel | implemented, verified | runtime capture + settings persistence |
| Graphics controls for AA / AO / quality | `GameGraphicsSettings.kt` | `ProjectAirDefenseGameUserSettings` + runtime UI | implemented, verified | packaged log + runtime UI proof |

## Visual Quality Rules

- Never reintroduce static gameplay backdrops.
- Keep the top edge mostly clear. HUD belongs in compact left and right segments, not a full-width bar.
- Any gameplay abstraction over the real city mesh must be explicit. Do not pretend a district cell is a literal mesh building.
- Do not claim a system is ported because a doc exists. The source and verification lane decide status.

## Immediate Order

1. Add Android-device UE packaging/profiling proof after the Win64 UE5 runtime lane is stable.
2. Add asset validation gates for 3D Tiles, glTF/GLB, and compressed texture inputs before new city datasets become active runtime content.
3. Move the front-end menu stack toward CommonUI only if the current UMG menu flow starts fragmenting.
4. Replace the direct `UButton::IsFocusable` construction workaround with a stable UE5.7-safe non-focus path when Epic exposes one publicly.
