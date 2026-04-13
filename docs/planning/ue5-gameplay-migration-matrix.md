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
| Defense doctrines and tuning | `BattleLogic.kt` | `ProjectAirDefenseBattleSimulation.*` | implemented, verified | doctrine comparison test |
| Wave spawning and accounting | `BattleSimulation.kt` | `ProjectAirDefenseBattleSimulation.*` | implemented, verified | consistency + seeded tests |
| Interceptor lead solve and fuse check | `BattleLogic.kt` | `ProjectAirDefenseBattleSimulation.*` | implemented, verified | intercept tests |
| Threat impact and city damage | `BattleSimulation.kt` | `ProjectAirDefenseBattleSimulation.*` | implemented, verified | hostile-impact test |
| Monte Carlo / headless balance lane | `BattleMonteCarloRunner.kt` | UE headless simulation runner | pending | command + report |

## Runtime Gameplay Presentation

| System | Legacy source | UE5 target | Status | Verification |
| --- | --- | --- | --- | --- |
| Battle manager that ticks the simulation | `BattleScreen.kt` | `ProjectAirDefenseBattleManager.*` | implemented, verified | editor and packaged runtime |
| Live threat / interceptor visuals | `BattleProjectileVisualController.kt` | `ProjectAirDefenseBattleManager.*` | implemented, verified | gameplay runtime capture |
| Battle HUD with compact side segments | `BattleHudController.kt` | `ProjectAirDefenseBattleHud.*` | implemented, verified | gameplay runtime capture |
| Touch-first systems drawer and camera controls | `BattleHudController.kt` + `BattleSceneController.kt` | `ProjectAirDefenseBattleHud.*` + `ProjectAirDefenseCityCameraPawn.*` | implemented, verified | systems-menu runtime capture |
| Gameplay input: start wave, doctrine, graphics toggles | `BattleHudController.kt` + `StartScreen.kt` | `ProjectAirDefensePlayerController.*` | implemented, verified | runtime input exercise |
| Radar / tactical overlay | `BattleRadarOverlayRenderer.kt` | UE overlay path | pending | runtime capture |
| Battle VFX and blast readability | `BattleEffectsController.kt` | UE VFX path | pending | runtime capture |

## Menus And UX

| System | Legacy source | UE5 target | Status | Verification |
| --- | --- | --- | --- | --- |
| Main menu | `StartScreen.kt` | UE menu scene / widget | pending | editor and packaged runtime |
| In-game settings surface | `BattleHudController.kt` | UE HUD/menu settings panel | implemented, verified | runtime capture + settings persistence |
| Graphics controls for AA / AO / quality | `GameGraphicsSettings.kt` | `ProjectAirDefenseGameUserSettings` + runtime UI | implemented, verified | packaged log + runtime UI proof |

## Visual Quality Rules

- Never reintroduce static gameplay backdrops.
- Keep the top edge mostly clear. HUD belongs in compact left and right segments, not a full-width bar.
- Any gameplay abstraction over the real city mesh must be explicit. Do not pretend a district cell is a literal mesh building.
- Do not claim a system is ported because a doc exists. The source and verification lane decide status.

## Immediate Order

1. Add the missing main menu scene / widget in UE5.
2. Port radar and tactical overlay semantics without blocking the playfield.
3. Add battle VFX and blast readability improvements.
4. Add a headless Monte Carlo lane for UE5 balance work.
