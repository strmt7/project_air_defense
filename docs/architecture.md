# Architecture Reference

## Active Runtime
- `ProjectAirDefenseGameMode`
  Owns bootstrapping of the active UE5 pilot scene, including local 3D Tiles resolution, optional remote candidate URL resolution, Cesium georeference setup, SunSky setup, atmosphere and post-process setup, camera focus framing, and runtime logging.
- `ProjectAirDefenseCityCameraPawn`
  Owns the in-game inspection camera, including pan, yaw rotation, pitch rotation, zoom, vertical motion, reset, and focus framing around the live district bounds.
  Stores camera distances and speeds in Unreal units internally even though config authors them in meters. This is required because Cesium focus points arrive in Unreal world space, not raw meters.
- `ProjectAirDefenseRuntimeSettings`
  Owns project-configurable runtime defaults for source id, remote 3D Tiles URL, fallback local tileset location, lighting, day/night controls, camera pose, and camera motion. These are tuned through config instead of hardcoded rebuild-only constants.
- `ProjectAirDefenseGameUserSettings`
  Owns persistent runtime graphics settings such as AA method, AO enablement, motion blur policy, ray-tracing request state, and UE scalability groups. These toggles are applied with `ECVF_SetByGameOverride`, and `DefaultEngine.ini` no longer hard-forces AA or AO above the user-settings layer.
- `ProjectAirDefenseBattleSimulation`
  Owns the deterministic air-defense simulation ported from the legacy runtime: hostile launch profiles, wave accounting, doctrine behavior, interceptor solve, fuse checks, and city-damage outcomes.
- `ProjectAirDefenseBattleManager`
  Owns the live gameplay bridge between the deterministic simulation and the active city scene. It ticks the simulation, exposes runtime snapshots to the HUD, applies graphics-setting changes through `ProjectAirDefenseGameUserSettings`, and renders battle markers/trails/blasts through packaged mesh components rather than debug-only draw calls. It must not render healthy synthetic district towers over the real city mesh.
- `ProjectAirDefenseBattleMonteCarloCommandlet`
  Runs headless balance sweeps through the same `FProjectAirDefenseBattleSimulation` class used by the runtime bridge and writes a JSON benchmark report.
- `ProjectAirDefensePlayerController`
  Owns menu-to-battle state, widget visibility, systems-drawer visibility, and the hidden debug-only keyboard fallback. Keyboard bindings remain available for verification and editor debugging, but are not part of the player-facing UI contract.
- `ProjectAirDefenseMainMenuWidget`
  Owns the active player-facing front-end menu surface: one compact touch-first action card, render summary, and city-source summary without blocking most of the view.
- `ProjectAirDefenseBattleWidget`
  Owns the active player-facing battle HUD: compact top corner status cards, a bottom-left radar cluster, bottom-right doctrine and wave actions, and a floating systems drawer for touch controls, graphics controls, and day/night controls.
- `ProjectAirDefenseRadarWidget`
  Owns the touch-safe radar surface fed by `ProjectAirDefenseBattleManager::BuildRadarSnapshot()`.

## Scene Bootstrap
- UE5 boots through `/Engine/Maps/Entry`.
- `ProjectAirDefenseGameMode::BeginPlay()` loads the configured local Helsinki `tileset.json` by default. Remote URLs remain a candidate path only when explicitly configured.
- `ACesiumGeoreference` is created or reused and aligned to the pilot origin.
- `ACesiumSunSky` is created or reused and applies project runtime lighting defaults. It remains the day/night authority; runtime solar-time changes must call `UpdateSun()`.
- `AExponentialHeightFog` and `APostProcessVolume` are created or reused and apply the repo runtime atmosphere and exposure defaults.
- `ACesium3DTileset` is created or reused and loads the active local `file:///` Helsinki URL, or an explicitly configured remote URL for candidate checks.
- Local Helsinki framing parses root bounds from the master tileset when available.
- `ProjectAirDefenseCityCameraPawn::FrameFocusPoint()` uses the resolved focus point and radius to choose the initial orbit distance.
- `ProjectAirDefenseBattleManager::InitializeBattlefield()` seeds the gameplay surface around the framed district.

## Data Pipeline
- Active runtime source:
  `ue5/ProjectAirDefenseUE5/ExternalData/helsinki_kalasatama_3dtiles/tileset.json`
- Active runtime license:
  Helsinki 3D CC BY 4.0 with required City of Helsinki / Helsinki 3D attribution.
- Active raw source archive:
  `data/external/downloads/Helsinki3D_MESH_Kalasatama_2017_3D_Tiles_ZIP.zip`
- Active runtime dataset:
  `data/external/helsinki_kalasatama_3dtiles/`
- Evaluated remote candidate:
  `https://data.3dbag.nl/v20250903/cesium3dtiles/lod22/tileset.json`
- Optional later photoreal bake input:
  `data/external/helsinki_kalasatama_mesh/`
- `scripts/prepare-ue5-city-tiles.ps1`
  Extracts the active Helsinki source and builds the root `tileset.json`.
- `scripts/upgrade-ue5-city-tiles.ps1`
  Runs Cesium `3d-tiles-tools upgrade` for the active Helsinki package because the upstream runtime package contains legacy `b3dm` payloads that packaged runtime rejects unchanged.
- `tools/ue5_city_pipeline/patch_cesium_for_unreal.py`
  Applies the local Cesium compatibility patch set, including the metadata-statistic enum default fix.

CesiumSunSky protected mobile-rendering fields are not set from project C++ because the installed plugin exposes no public setter. Mobile rendering support is handled through project settings, mobile-supported lighting defaults, and verified graphics CVars instead.

## Verification Lanes
- Deterministic gameplay coverage:
  `scripts/run-ue5-automation-tests.ps1`
  Runs the UE automation suite for the battle simulation and related runtime glue.
- Headless UE5 battle balance:
  `scripts/run-ue5-battle-monte-carlo.ps1`
  Runs `ProjectAirDefenseBattleMonteCarlo` through `UnrealEditor-Cmd.exe -nullrhi` and writes `benchmark-results/ue5-battle-monte-carlo.json`.
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
- The default Helsinki runtime path is local and should keep only one raw upstream archive and one active upgraded runtime dataset.
- Remote candidates should not create large local city-data copies unless explicitly selected for a bake or offline proof.
- Keep one packaged build.
- Do not keep long-lived legacy backup copies of the runtime dataset after verification.
- Do not keep `Saved/StagedBuilds` after a successful archive unless a task explicitly needs staged-only inspection.
