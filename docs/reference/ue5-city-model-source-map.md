# UE5 City Model Source Map

Compact evidence list for the UE5 city-model plan. Official sources come first. Community links are included only where they expose real workflow or performance constraints.

## Official Engine Sources

| Source | Type | Why it matters |
| --- | --- | --- |
| [Nanite Virtualized Geometry in Unreal Engine](https://dev.epicgames.com/documentation/en-us/unreal-engine/nanite-virtualized-geometry-in-unreal-engine) | Epic official docs | dense imported city meshes are a first-class UE5 path |
| [Level Streaming in Unreal Engine](https://dev.epicgames.com/documentation/unreal-engine/level-streaming-in-unreal-engine) | Epic official docs | UE5 recommends World Partition for large streamed worlds |
| [Large World Coordinates Rendering in Unreal Engine 5](https://dev.epicgames.com/documentation/en-us/unreal-engine/large-world-coordinates-rendering-in-unreal-engine-5) | Epic official docs | real-scale geospatial worlds need LWC awareness |
| [Using PCG with World Partition in Unreal Engine](https://dev.epicgames.com/documentation/en-us/unreal-engine/using-pcg-with-world-partition-in-unreal-engine) | Epic official docs | procedural infill can align with Data Layers and HLOD |
| [Lumen Technical Details in Unreal Engine](https://dev.epicgames.com/documentation/unreal-engine/lumen-technical-details-in-unreal-engine) | Epic official docs | dynamic night lighting and reflections are a built-in UE5 path |
| [Datasmith Supported Software and File Types](https://dev.epicgames.com/documentation/en-us/unreal-engine/datasmith-supported-software-and-file-types?application_version=5.6) | Epic official docs | identifies supported DCC/CAD ingestion paths |

## Official City Data And Geospatial Runtime Sources

| Source | Type | Why it matters |
| --- | --- | --- |
| [Helsinki 3D](https://www.hel.fi/en/decision-making/information-on-helsinki/maps-and-geospatial-data/helsinki-3d) | official city data portal | open 3D mesh and semantic city models exist |
| [Helsinki Kalasatama 3D Tiles](https://3d.hel.ninja/data/mesh/Kalasatama/Helsinki3D_MESH_Kalasatama_2017_3D_Tiles_ZIP.zip) | official tiles download | direct local UE5 runtime proof path exists |
| [Helsinki Kalasatama OBJ mesh](https://3d.hel.ninja/data/mesh/Kalasatama/Helsinki3D_MESH_Kalasatama_2017_OBJ_ZIP.zip) | official mesh download | district-sized textured mesh candidate exists now |
| [Berlin 3D download portal](https://www.businesslocationcenter.de/en/economic-atlas/download-portal) | official city portal | official textured OBJ city mesh downloads exist |
| [3DBAG OBJ delivery](https://docs.3dbag.nl/en/delivery/obj/) | official dataset docs | open OBJ exports are available |
| [3DBAG CityJSON delivery](https://docs.3dbag.nl/en/delivery/cityjson/) | official dataset docs | semantic open city data is available |
| [3DBAG web services](https://docs.3dbag.nl/en/delivery/webservices/) | official dataset docs | 3D Tiles and web-service path exists |
| [3DBAG copyright](https://docs.3dbag.nl/en/copyright/) | official dataset docs | licensing boundary is explicit |
| [Cesium for Unreal](https://cesium.com/learn/unreal/) | official SDK docs | direct 3D Tiles rendering path inside UE5 |
| [Cesium for Unreal releases](https://github.com/CesiumGS/cesium-unreal/releases) | official plugin releases | version-pinned local plugin installation path |
| [Cesium OSM Buildings](https://cesium.com/platform/cesium-ion/content/cesium-osm-buildings/) | official Cesium content page | quick building truth-check layer exists |
| [ArcGIS Maps SDK for Unreal layers](https://developers.arcgis.com/unreal-engine/layers/) | official SDK docs | supports local or online scene layers, integrated mesh, building layers, and 3D Tiles |
| [ArcGIS 3D Tiles layers](https://developers.arcgis.com/unreal-engine/layers/data-layers/3d-tiles/) | official SDK docs | explicit 3D Tiles path inside UE5 |
| [CityEngine DATASMITH export for Unreal](https://doc.arcgis.com/en/cityengine/2022.0/help/help-export-unreal.htm) | official Esri docs | rule-based city generation can land in UE via Datasmith |
| [PLATEAU SDK for Unreal](https://github.com/Project-PLATEAU/PLATEAU-SDK-for-Unreal) | official project repo | official CityGML-to-UE path exists for PLATEAU datasets |

## Official Tooling And Discovery Sources

| Source | Type | Why it matters |
| --- | --- | --- |
| [CityJSON software](https://www.cityjson.org/software/) | official spec ecosystem page | lists maintained tools for CityJSON workflows |
| [BlenderGIS](https://github.com/domlysz/BlenderGIS) | official repo | practical Blender-side geospatial cleanup tool |
| [awesome-citygml](https://github.com/OloOcki/awesome-citygml) | curated index | useful discovery map for CityGML tooling and datasets |

## Community Constraint Sources

| Source | Type | Why it matters |
| --- | --- | --- |
| [Cesium Community: offline/on-premises](https://community.cesium.com/t/cesium-offline-on-premises/45278) | vendor community forum | local 3D Tiles are possible, but self-hosted/offline workflows are constrained |
| [Cesium Community: World Terrain offline](https://community.cesium.com/t/how-to-use-cesium-world-terrain-in-offline/29096) | vendor community forum | provider-scale data is not a free offline mirror |
| [Epic forum: HLODs and World Partition streaming performance](https://forums.unrealengine.com/t/hlods-dont-work-and-world-partition-streaming-performance-is-terrible/1208893) | engine community forum | large city streaming and HLOD tuning are real production risks |
| [Epic forum: World Partition and Z-space](https://forums.unrealengine.com/t/world-partition-not-taking-z-space-into-account/545872) | engine community forum | dense vertical cities need careful partition strategy |

## Prohibited Or Restricted Inputs

| Source | Type | Why it matters |
| --- | --- | --- |
| [Google Earth / Maps Additional Terms of Service](https://www.google.com/help/terms_maps-earth/) | Google terms | prohibits mass download or creating bulk feeds of the content |
| [Google Maps Platform Terms](https://cloud.google.com/maps-platform/terms/index-20190502) | Google terms | forbids using content to create or augment a mapping-related dataset without explicit permission |
