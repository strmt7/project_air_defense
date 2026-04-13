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
- [Kalasatama OBJ mesh zip](https://3d.hel.ninja/data/mesh/Kalasatama/Helsinki3D_MESH_Kalasatama_2017_OBJ_ZIP.zip)

## Mandatory Pipeline

1. Download the official mesh through `tools/ue5_city_pipeline/download_city_source.py`.
2. Extract into `data/external/helsinki_kalasatama/`.
3. Open in Blender with the district centered around a local origin.
4. Split the district into streamable city blocks or 250 m cells.
5. Keep buildings, water edge, roads, and major terrain as separate logical collections.
6. Export UE5-ready static meshes.
7. Import into `ue5/ProjectAirDefenseUE5` as the first pilot district.
8. Enable Nanite on imported static meshes and stream them through World Partition.

## Forbidden Shortcuts

- no skyline photo cards
- no background panorama as the gameplay horizon
- no procedural replacement for the main district geometry
- no Google Earth extraction

## Acceptance Gate

The pilot is only valid if it meets `docs/planning/ue5-visual-acceptance.md`.
