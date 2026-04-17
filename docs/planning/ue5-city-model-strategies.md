# UE5 City Model Strategies

## Scope

The goal is a believable, inspectable, gameplay-usable 3D district in UE5. The target is a district-sized slice, not an entire metro region.

## Strategy Ranking

| Rank | Strategy | Use when | Why it is strong | Main risk |
| --- | --- | --- | --- | --- |
| 1 | Official open textured city mesh -> Blender cleanup -> UE5 Nanite + World Partition | an official textured mesh or OBJ district exists | fastest path to truthful geometry and materials | requires cleanup, recentering, and runtime budgeting |
| 2 | Official 3D Tiles / integrated mesh / scene-layer streaming in UE5 | fast scouting, truth-checking, or editor-side validation is needed | direct geospatial rendering inside UE5 is possible with Cesium or ArcGIS | offline, licensing, packaging, and streaming constraints can block shipping |
| 3 | CityJSON / CityGML / OBJ open data -> DCC conversion -> UE5 | only semantic city data is available | keeps licensing cleaner and gives full bake control | conversion pipeline is heavier and needs QA |
| 4 | Procedural supplement on top of real data | the base district exists but has gaps | PCG or CityEngine can fill props, streetscape, and non-critical blocks | procedural output is not ground truth and must stay secondary |
| 5 | Google Earth bulk extraction | never | not acceptable for shipped geometry | legal and product risk |

## Strategy 1: Official Open Mesh First

Use an official textured mesh or official OBJ district whenever it exists.

- Helsinki publishes open 3D mesh and semantic city data: [Helsinki 3D](https://www.hel.fi/en/decision-making/information-on-helsinki/maps-and-geospatial-data/helsinki-3d)
- Helsinki also exposes district mesh downloads such as Kalasatama OBJ: [Kalasatama mesh OBJ zip](https://3d.hel.ninja/data/mesh/Kalasatama/Helsinki3D_MESH_Kalasatama_2017_OBJ_ZIP.zip)
- Berlin publishes a free 3D model with OBJ geometry and textures through its official portal: [Berlin 3D download portal](https://www.businesslocationcenter.de/en/economic-atlas/download-portal)
- 3DBAG publishes OBJ exports and 3D Tiles for Dutch city data: [3DBAG OBJ delivery](https://docs.3dbag.nl/en/delivery/obj/), [3DBAG web services](https://docs.3dbag.nl/en/delivery/webservices/)
- 3DBAG data and documentation are CC BY 4.0 and require the credit `© 3DBAG by tudelft3d and 3DGI`: [3DBAG copyright](https://docs.3dbag.nl/en/copyright/)

Recommended use:

- pick a 0.5 to 2.0 square kilometer district
- clean and recenter it in Blender
- split it into UE5-friendly chunks
- import as Nanite static meshes
- stream it with World Partition and HLOD

This is the default primary path.

## Strategy 2: Direct Geospatial Rendering In UE5

Use direct geospatial rendering when the team needs truth-grounded validation fast.

- Cesium for Unreal can load 3D Tiles from local directories or supported servers: [Cesium for Unreal learning hub](https://cesium.com/learn/unreal/)
- Cesium OSM Buildings is a ready global building layer for quick layout truth checks: [Cesium OSM Buildings](https://cesium.com/platform/cesium-ion/content/cesium-osm-buildings/)
- ArcGIS Maps SDK for Unreal supports local or online scene layers, integrated mesh, building layers, and 3D Tiles: [ArcGIS Maps SDK layers](https://developers.arcgis.com/unreal-engine/layers/), [ArcGIS 3D Tiles layers](https://developers.arcgis.com/unreal-engine/layers/data-layers/3d-tiles/)

Use this path for:

- scouting candidate districts
- checking scale, camera, skyline shape, and horizon truth
- temporary in-editor visualization
- first-pass runtime proof before a later Nanite shipping bake

Current repo state:

- the active runtime pilot loads local Helsinki Kalasatama 3D Tiles through Cesium for Unreal
- 3DBAG Rotterdam LoD2.2 remains an evaluated remote candidate, not the default runtime
- the later shipping-quality district path still moves toward cleaned Helsinki OBJ or another official mesh source with UE5-native scene ownership

Do not treat this as the default final shipping path unless licensing, offline behavior, packaging, and performance are proven.

## Strategy 3: Open Semantic Data -> DCC -> UE5

Use this when only semantic city data exists or when a mesh needs controlled rebuilding.

- 3DBAG ships CityJSON plus OBJ: [3DBAG CityJSON](https://docs.3dbag.nl/en/delivery/cityjson/), [3DBAG OBJ delivery](https://docs.3dbag.nl/en/delivery/obj/)
- CityJSON maintains an official software index for tooling: [CityJSON software](https://www.cityjson.org/software/)
- BlenderGIS is a practical Blender-side geospatial helper: [BlenderGIS](https://github.com/domlysz/BlenderGIS)
- `awesome-citygml` is a useful discovery index, not a shipping authority: [awesome-citygml](https://github.com/OloOcki/awesome-citygml)

Recommended use:

- acquire a bounded district
- normalize coordinates and units in DCC
- fix topology, pivots, and materials there
- export a UE5-friendly asset set

## Strategy 4: Procedural Supplement, Never Procedural Replacement

Use procedural systems only after a truthful city core exists.

- UE5 PCG integrates with World Partition and Data Layers: [Using PCG with World Partition in Unreal Engine](https://dev.epicgames.com/documentation/en-us/unreal-engine/using-pcg-with-world-partition-in-unreal-engine)
- CityEngine can export Datasmith directly to Unreal: [CityEngine DATASMITH export for Unreal](https://doc.arcgis.com/en/cityengine/2022.0/help/help-export-unreal.htm)
- PLATEAU provides a UE SDK for official Japanese 3D city data workflows: [PLATEAU SDK for Unreal](https://github.com/Project-PLATEAU/PLATEAU-SDK-for-Unreal)

Use this path for:

- vegetation
- street furniture
- filler blocks
- LOD-specific simplification layers

Do not use it to fake the primary skyline.

## Community Evidence And Failure Modes

- World Partition and HLOD tuning can become a real bottleneck in large city scenes: [Epic forum: HLODs don't work and World Partition streaming performance is terrible](https://forums.unrealengine.com/t/hlods-dont-work-and-world-partition-streaming-performance-is-terrible/1208893)
- Dense vertical cities can overload World Partition if cell sizing and loading ranges are wrong: [Epic forum: World Partition not taking Z space into account](https://forums.unrealengine.com/t/world-partition-not-taking-z-space-into-account/545872)
- Cesium offline use is possible with local 3D Tiles or self-hosted tooling, but not as a free universal offline mirror of provider-scale datasets: [Cesium Community: offline/on-premises](https://community.cesium.com/t/cesium-offline-on-premises/45278), [Cesium Community: World Terrain offline](https://community.cesium.com/t/how-to-use-cesium-world-terrain-in-offline/29096)

These links are not the plan. They are the warning label for the plan.

## Recommended Path

1. Keep the current Helsinki Kalasatama Strategy 2 local 3D Tiles path as the default runtime truth-check path.
2. Move to Strategy 1 for shipping-quality offline scenes by cleaning a bounded official mesh/OBJ district and attributing it.
3. Fall back to Strategy 3 when no official textured mesh exists.
4. Use Strategy 4 only after the base district is proven in-engine.
5. Reject Strategy 5 outright.

## Immediate Next Step After This Plan

Keep the current Helsinki Kalasatama pilot truthful with Strategy 2, use 3DBAG only as an explicit remote candidate, and graduate to Strategy 1 only after a specific official mesh district is selected. Do not resume libGDX city rendering work.
