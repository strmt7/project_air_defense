# ProjectAirDefenseUE5

This is the UE5 migration scaffold for Project Air Defense.

## Scope

- primary city pilot: Helsinki Kalasatama
- real 3D district ingestion, not a fake skyline
- UE5 is the only future runtime target

## Local Expectations

- use a current UE5 installation
- ingest the pilot district from `data/ue5_city_pilot/helsinki_kalasatama/pilot_manifest.json`
- keep raw downloads outside git in `data/external/`

## Scene Policy

- Nanite for imported city meshes
- World Partition for district streaming
- UE5 lighting and reflections for the night scene
- no static background images in the gameplay camera
- mandatory camera inspection controls in the playable scene
