# UE5 City Pilot: Helsinki Kalasatama

## Pilot Choice

The first UE5 photoreal district pilot is Helsinki Kalasatama.

## Why This District

- official public 3D mesh exists
- high-rise waterfront district, so it stresses skyline quality instead of hiding it
- compact enough for a district-sized pilot
- good fit for Nanite plus World Partition

Primary sources:

- [Helsinki 3D](https://www.hel.fi/en/decision-making/information-on-helsinki/maps-and-geospatial-data/helsinki-3d)
- [Kalasatama 3D Tiles zip](https://3d.hel.ninja/data/mesh/Kalasatama/Helsinki3D_MESH_Kalasatama_2017_3D_Tiles_ZIP.zip)
- [Kalasatama OBJ mesh zip](https://3d.hel.ninja/data/mesh/Kalasatama/Helsinki3D_MESH_Kalasatama_2017_OBJ_ZIP.zip)

## Mandatory Pipeline

### Phase A: Truthful Runtime Bootstrap

1. Install Cesium for Unreal project-locally through `scripts/install-cesium-for-unreal.ps1`.
2. Download or verify the official Kalasatama 3D Tiles archive under `data/external/downloads/`.
3. Extract it into `data/external/helsinki_kalasatama_3dtiles/`.
4. Generate the root `tileset.json` with `tools/ue5_city_pipeline/build_master_tileset.py`.
5. Upgrade the extracted runtime dataset with `scripts/upgrade-ue5-city-tiles.ps1` before packaged runtime verification. The official Helsinki 3D Tiles package ships legacy `b3dm` payloads with glTF 1 content, which packaged Cesium runtime rejects.
6. Open or launch `ue5/ProjectAirDefenseUE5` and verify the city boots from the local master `tileset.json`.
7. Build one packaged runtime through `scripts/package-ue5-runtime.ps1`. This path must keep exactly one archived build and remove `Saved/StagedBuilds` afterward.
8. Use the in-game inspection camera to validate skyline depth, shoreline geometry, and building credibility from multiple angles.

Storage policy for this pilot:
- keep the original upstream zip under `data/external/downloads/` as the reproducible source of truth
- keep one active upgraded runtime dataset under `data/external/helsinki_kalasatama_3dtiles/`
- do not keep `helsinki_kalasatama_3dtiles_legacy_backup/` after the upgraded runtime is verified
- do not keep `Saved/StagedBuilds/` after packaging unless a task explicitly requires staged-only inspection

### Phase B: Photoreal Shipping Bake

1. Download the official OBJ mesh through `tools/ue5_city_pipeline/download_city_source.py`.
2. Extract into `data/external/helsinki_kalasatama_mesh/`.
3. Open in Blender with the district centered around a local origin.
4. Split the district into streamable city blocks or 250 m cells.
5. Keep buildings, water edge, roads, and major terrain as separate logical collections.
6. Export UE5-ready static meshes.
7. Import them into `ue5/ProjectAirDefenseUE5` as the baked district.
8. Enable Nanite on imported static meshes and stream them through World Partition.

## Forbidden Shortcuts

- no skyline photo cards
- no background panorama as the gameplay horizon
- no procedural replacement for the main district geometry
- no Google Earth extraction

## Acceptance Gate

The pilot is only valid if it meets `docs/planning/ue5-visual-acceptance.md`.
