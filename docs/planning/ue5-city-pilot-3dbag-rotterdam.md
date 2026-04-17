# UE5 City Pilot: 3DBAG Rotterdam

## Pilot Choice

This is an evaluated UE5 remote-runtime candidate, not the current default. It streams 3DBAG LoD2.2 3D Tiles centered on a Rotterdam high-rise district through Cesium for Unreal when explicitly configured.

## Why This Pilot

- official 3D Tiles stream exists
- LoD2.2 building geometry gives real roof and facade volume instead of card-like placeholders
- Rotterdam has dense waterfront high-rises suitable for skyline, lighting, and camera stress tests
- remote streaming avoids redundant multi-gigabyte local city-data copies during iteration

Primary sources:

- [3DBAG webservices](https://docs.3dbag.nl/en/delivery/webservices/)
- [3DBAG LoD2.2 tileset](https://data.3dbag.nl/v20250903/cesium3dtiles/lod22/tileset.json)
- [3DBAG copyright](https://docs.3dbag.nl/en/copyright/)

## Runtime Contract

- `ProjectAirDefenseRuntimeSettings` may set `PreferredSourceId=3dbag_rotterdam_lod22`, the remote 3D Tiles URL, the Rotterdam origin, and a fallback framing radius for candidate checks. Quote the URL in `DefaultGame.ini`; otherwise Unreal treats `//` as a comment and truncates `https://` to `https:`.
- `ProjectAirDefenseGameMode` must load this remote URL only when it is explicitly configured. The current default runtime remains local Helsinki.
- `ACesiumSunSky` is the day/night authority. Call `UpdateSun()` after runtime solar-time changes.
- Do not set CesiumSunSky protected mobile fields directly from C++; no public setter is available in the installed Cesium plugin.
- Use mobile-supported lighting as the default: directional light, skylight, reflection captures, fog, post-process tuning, and graphics settings that map to real CVars.
- Ray tracing may exist as an opt-in request path, but it is off by default and not the Android mobile baseline.

## Attribution Contract

3DBAG data and documentation are licensed under CC BY 4.0. Runtime and documentation surfaces must include:

- required credit: `© 3DBAG by tudelft3d and 3DGI`
- link: `https://docs.3dbag.nl/en/copyright/`
- change statement: this candidate streams the source 3D Tiles through Cesium for Unreal without local geometry changes

The runtime battle view places the credit near the bottom-right map edge without using a static backdrop.

## Fallback Path

Helsinki Kalasatama is the current local default because it is verified in this workspace. Use this 3DBAG candidate only for explicit remote-source evaluation.

## Acceptance Gate

The pilot is only valid if it meets `docs/planning/ue5-visual-acceptance.md` and passes the current UE5 verification lane without retaining duplicate staged builds or stale city-data copies.
