# Runtime Asset Attribution

## Imported runtime assets
- `models/engel_house.obj`
  - Source: [Ladybug Tools / 3D Models](https://github.com/ladybug-tools/3d-models)
  - Upstream path: `obj/engel-house/AngelHouse_Bauhaus-in-Israel-r2.obj`
  - License: MIT
- Runtime 3D Tiles dataset: [Helsinki Kalasatama 3D Tiles](https://3d.hel.ninja/data/mesh/Kalasatama/Helsinki3D_MESH_Kalasatama_2017_3D_Tiles_ZIP.zip)
  - Source docs: [Helsinki 3D](https://www.hel.fi/en/decision-making/information-on-helsinki/maps-and-geospatial-data/helsinki-3d)
  - License: [CC BY 4.0](https://www.hel.fi/en/decision-making/information-on-helsinki/maps-and-geospatial-data/helsinki-3d)
  - Required credit: City of Helsinki / Helsinki 3D
  - Changes: prepared as a local Cesium 3D Tiles runtime dataset for UE5; no source geometry is hand-authored
- Evaluated remote 3D Tiles candidate: [3DBAG Rotterdam LoD2.2](https://data.3dbag.nl/v20250903/cesium3dtiles/lod22/tileset.json)
  - Source docs: [3DBAG webservices](https://docs.3dbag.nl/en/delivery/webservices/)
  - License: [CC BY 4.0](https://docs.3dbag.nl/en/copyright/)
  - Required credit: `© 3DBAG by tudelft3d and 3DGI`

## Reference-only inputs
- `references/tel_aviv_night.jpg`
  - Used for visual benchmarking and direction, not as runtime 3D geometry

## Candidate future runtime sources
- [Poly Haven](https://polyhaven.com/license) -- CC0
- [ambientCG](https://docs.ambientcg.com/license/) -- CC0
- [Kenney](https://kenney.nl/assets?q=city) -- CC0
