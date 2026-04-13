# Architecture Reference

## Active Runtime
- `ProjectAirDefenseGameMode`
  Owns bootstrapping of the active UE5 pilot scene, including local 3D Tiles path resolution, Cesium georeference setup, SunSky setup, camera focus framing, and runtime logging.
- `ProjectAirDefenseCityCameraPawn`
  Owns the in-game inspection camera, including pan, rotate, zoom, vertical motion, reset, and focus framing around the live district bounds.
- `ProjectAirDefenseRuntimeSettings`
  Owns project-configurable runtime defaults for lighting, camera pose, camera motion, and local tileset location. These are tuned through config instead of hardcoded rebuild-only constants.
- `ProjectAirDefenseGameUserSettings`
  Owns persistent runtime graphics settings such as AA method, AO enablement, motion blur policy, and UE scalability groups. This is the authoritative graphics-settings backend for future menus.

## Scene Bootstrap
- UE5 boots through `/Engine/Maps/Entry`.
- `ProjectAirDefenseGameMode::BeginPlay()` resolves `tileset.json` from `ExternalData/helsinki_kalasatama_3dtiles`.
- `ACesiumGeoreference` is created or reused and aligned to the pilot origin.
- `ACesiumSunSky` is created or reused and applies project runtime lighting defaults.
- `ACesium3DTileset` is created or reused and loads the local upgraded Helsinki Kalasatama dataset through a `file:///` URL.
- The root bounding sphere from the tileset is parsed and transformed from ECEF into Unreal coordinates.
- `ProjectAirDefenseCityCameraPawn::FrameFocusPoint()` uses that radius to choose the initial orbit distance and focus point.

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
- Low-waste scene iteration:
  `scripts/capture-ue5-editor-runtime-screenshot.ps1`
  Launches `UnrealEditor.exe <uproject> -game`, captures a runtime frame, and supports scripted camera key input.
- Shipping-path verification:
  `scripts/package-ue5-runtime.ps1`
  Builds one packaged runtime and removes duplicate `Saved/StagedBuilds` after archiving.
- Packaged runtime capture:
  `scripts/capture-ue5-runtime-screenshot.ps1 -Exe packaged/Win64/ProjectAirDefenseUE5.exe`

## Storage Policy
- Keep one raw upstream archive.
- Keep one active upgraded runtime dataset.
- Keep one packaged build.
- Do not keep long-lived legacy backup copies of the runtime dataset after verification.
- Do not keep `Saved/StagedBuilds` after a successful archive unless a task explicitly needs staged-only inspection.
