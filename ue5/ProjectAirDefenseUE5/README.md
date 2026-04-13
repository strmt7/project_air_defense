# ProjectAirDefenseUE5

This is the UE5 migration scaffold for Project Air Defense.

## Scope

- primary city pilot: Helsinki Kalasatama
- real 3D district ingestion, not a fake skyline
- UE5 is the only future runtime target
- current runtime proof path: official local 3D Tiles plus Cesium for Unreal
- current photoreal bake target: official OBJ mesh plus Blender cleanup plus Nanite

## Local Expectations

- use Unreal Engine `5.7`
- use Blender `5.1.0`
- install Cesium for Unreal `2.25.0` with `..\..\scripts\install-cesium-for-unreal.ps1`
- ingest the pilot district from `data/ue5_city_pilot/helsinki_kalasatama/pilot_manifest.json`
- keep raw downloads outside git in `data/external/`
- keep the generated master 3D Tiles entry point at `data/external/helsinki_kalasatama_3dtiles/tileset.json`

## Scene Policy

- Nanite for imported city meshes
- World Partition for district streaming
- UE5 lighting and reflections for the night scene
- no static background images in the gameplay camera
- mandatory camera inspection controls in the playable scene
- player-facing UI is smartphone-first and landscape-only
- visible controls must be touch-first; keyboard bindings are debug-only fallback

## Player-Facing Controls

- bottom-left `SYSTEMS`: open or close the floating systems drawer
- bottom-right doctrine button: cycle the active defense doctrine
- bottom-right action button: start the next wave or confirm the battle is live
- systems drawer `TACTICS`: battle state and doctrine controls
- systems drawer `CAMERA`: pan, yaw, tilt, zoom, altitude, and reset
- systems drawer `GRAPHICS`: overall quality plus AA, AO, blur, shadow, reflection, and post-process controls

## Debug-Only Keyboard Fallback

- hidden keyboard input remains available for verification, automation, and editor debugging only
- it is not part of the player-facing product contract and must never appear in visible UI copy
