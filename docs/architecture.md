# Architecture Reference

## Active Runtime
- `ProjectAirDefenseGameMode`
  Owns bootstrapping of the active UE5 pilot scene, including local 3D Tiles path resolution, Cesium georeference setup, SunSky setup, atmosphere and post-process setup, camera focus framing, and runtime logging.
- `ProjectAirDefenseCityCameraPawn`
  Owns the in-game inspection camera, including pan, yaw rotation, pitch rotation, zoom, vertical motion, reset, and focus framing around the live district bounds.
  Stores camera distances and speeds in Unreal units internally even though config authors them in meters. This is required because Cesium focus points arrive in Unreal world space, not raw meters.
- `ProjectAirDefenseRuntimeSettings`
  Owns project-configurable runtime defaults for lighting, camera pose, camera motion, and local tileset location. These are tuned through config instead of hardcoded rebuild-only constants.
- `ProjectAirDefenseGameUserSettings`
  Owns persistent runtime graphics settings such as AA method, AO enablement, motion blur policy, and UE scalability groups. These toggles are applied with `ECVF_SetByGameOverride`, and `DefaultEngine.ini` no longer hard-forces AA or AO above the user-settings layer.
- `ProjectAirDefenseBattleSimulation`
  Owns the deterministic air-defense simulation ported from the legacy runtime: hostile launch profiles, wave accounting, doctrine behavior, interceptor solve, fuse checks, and city-damage outcomes.
- `ProjectAirDefenseBattleManager`
  Owns the live gameplay bridge between the deterministic simulation and the active city scene. It ticks the simulation, exposes runtime snapshots to the HUD, and applies graphics-setting changes through `ProjectAirDefenseGameUserSettings`.
- `ProjectAirDefensePlayerController`
  Owns menu-to-battle state, widget visibility, systems-drawer visibility, and the hidden debug-only keyboard fallback. Keyboard bindings remain available for verification and editor debugging, but are not part of the player-facing UI contract.
- `ProjectAirDefenseMainMenuWidget`
  Owns the active player-facing front-end menu surface: one compact touch-first action card, render summary, and city-source summary without blocking most of the view.
- `ProjectAirDefenseBattleWidget`
  Owns the active player-facing battle HUD: compact top corner status cards, a bottom-left radar cluster, bottom-right doctrine and wave actions, and a floating systems drawer for touch controls.
- `ProjectAirDefenseRadarWidget`
  Owns the touch-safe radar surface fed by `ProjectAirDefenseBattleManager::BuildRadarSnapshot()`.

## Scene Bootstrap
- UE5 boots through `/Engine/Maps/Entry`.
- `ProjectAirDefenseGameMode::BeginPlay()` resolves `tileset.json` from `ExternalData/helsinki_kalasatama_3dtiles`.
- `ACesiumGeoreference` is created or reused and aligned to the pilot origin.
- `ACesiumSunSky` is created or reused and applies project runtime lighting defaults.
- `AExponentialHeightFog` and `APostProcessVolume` are created or reused and apply the repo runtime atmosphere and exposure defaults.
- `ACesium3DTileset` is created or reused and loads the local upgraded Helsinki Kalasatama dataset through a `file:///` URL.
- The root bounding sphere from the tileset is parsed and transformed from ECEF into Unreal coordinates.
- `ProjectAirDefenseCityCameraPawn::FrameFocusPoint()` uses that radius to choose the initial orbit distance and focus point.
- `ProjectAirDefenseBattleManager::InitializeBattlefield()` seeds the gameplay surface around the framed district.

## Data Pipeline
- Raw source archive:
  `data/external/downloads/Helsinki3D_MESH_Kalasatama_2017_3D_Tiles_ZIP.zip`
- Active runtime dataset:
  `data/external/helsinki_kalasatama_3dtiles/`
- Optional later photoreal bake input:
  `data/external/helsinki_kalasatama_mesh/`
- Runtime staging into the UE5 project is through:
  `ue5/ProjectAirDefenseUE5/ExternalData/helsinki_kalasatama_3dtiles`
- `scripts/prepare-ue5-city-tiles.ps1`
  Extracts the official source and builds the root `tileset.json`.
- `scripts/upgrade-ue5-city-tiles.ps1`
  Runs Cesium `3d-tiles-tools upgrade` because the upstream Helsinki runtime package contains legacy `b3dm` payloads that packaged runtime rejects unchanged.
- `tools/ue5_city_pipeline/patch_cesium_for_unreal.py`
  Applies the local Cesium compatibility patch set, including the metadata-statistic enum default fix.

## Verification Lanes
- Deterministic gameplay coverage:
  `scripts/run-ue5-automation-tests.ps1`
  Runs the UE automation suite for the battle simulation and related runtime glue.
- Low-waste scene iteration:
  `scripts/capture-ue5-editor-runtime-screenshot.ps1`
  Launches `UnrealEditor.exe <uproject> -game`, captures a runtime frame, and supports scripted camera key input.
- Shipping-path verification:
  `scripts/package-ue5-runtime.ps1`
  Builds one packaged runtime and removes duplicate `Saved/StagedBuilds` after archiving.
- Packaged runtime capture:
  `scripts/capture-ue5-runtime-screenshot.ps1 -Exe packaged/Win64/ProjectAirDefenseUE5.exe`
  Supports battle-frame proof and systems-drawer proof.
- Packaged mobile UI proof set:
  `scripts/run-ue5-mobile-ui-proof.ps1`
  Captures packaged menu, battle, and systems surfaces serially and prevents overlapping verification launches against the same packaged build.

## Storage Policy
- Keep one raw upstream archive.
- Keep one active upgraded runtime dataset.
- Keep one packaged build.
- Do not keep long-lived legacy backup copies of the runtime dataset after verification.
- Do not keep `Saved/StagedBuilds` after a successful archive unless a task explicitly needs staged-only inspection.
