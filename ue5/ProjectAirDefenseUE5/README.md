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

## Verified Controls

- `W/S` or `Up/Down`: pan forward and back
- `A/D` or `Left/Right`: pan left and right
- `Q/E`: rotate yaw
- `PageUp/PageDown` or mouse wheel: zoom
- `R/F`: raise and lower the focus point
- `Home`: reset the camera
